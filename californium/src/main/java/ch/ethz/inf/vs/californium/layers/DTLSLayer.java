/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.layers;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.dtls.ApplicationMessage;
import ch.ethz.inf.vs.californium.dtls.ClientHandshaker;
import ch.ethz.inf.vs.californium.dtls.ClientHello;
import ch.ethz.inf.vs.californium.dtls.ContentType;
import ch.ethz.inf.vs.californium.dtls.DTLSFlight;
import ch.ethz.inf.vs.californium.dtls.DTLSMessage;
import ch.ethz.inf.vs.californium.dtls.DTLSSession;
import ch.ethz.inf.vs.californium.dtls.HandshakeMessage;
import ch.ethz.inf.vs.californium.dtls.Handshaker;
import ch.ethz.inf.vs.californium.dtls.Record;
import ch.ethz.inf.vs.californium.dtls.ResumingClientHandshaker;
import ch.ethz.inf.vs.californium.dtls.ResumingServerHandshaker;
import ch.ethz.inf.vs.californium.dtls.ServerHandshaker;
import ch.ethz.inf.vs.californium.dtls.ServerHello;
import ch.ethz.inf.vs.californium.util.ByteArrayUtils;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * This layer provides security. If no session to the peer available, start a
 * new handshake to negotiate the security parameters. Afterwards, encrypt the
 * message with the current write state. See <a
 * href="http://tools.ietf.org/html/rfc5246">The Transport Layer Security (TLS)
 * Protocol Version 1.2</a> and <a
 * href="http://tools.ietf.org/html/rfc6347">Datagram Transport Layer Security
 * Version 1.2</a> for details.
 * 
 * @author Stefan Jucker
 * 
 */
public class DTLSLayer extends Layer {

	/** The socket to send the datagrams. */
	private DatagramSocket socket;

	/** The timer daemon to schedule retransmissions. */
	private Timer timer = new Timer(true); // run as daemon

	/** */
	private ReceiverThread receiverThread;

	/** Storing sessions according to peer-addresses */
	private Map<String, DTLSSession> dtlsSessions = new HashMap<String, DTLSSession>();

	/** Storing handshakers according to peer-addresses. */
	private Map<String, Handshaker> handshakers = new HashMap<String, Handshaker>();

	/** Storing flights according to peer-addresses. */
	private Map<String, DTLSFlight> flights = new HashMap<String, DTLSFlight>();

	/**
	 * Utility class to handle timeouts.
	 */
	private class RetransmitTask extends TimerTask {

		private DTLSFlight flight;

		RetransmitTask(DTLSFlight flight) {
			this.flight = flight;
		}

		@Override
		public void run() {
			handleTimeout(flight);
		}
	}

	class ReceiverThread extends Thread {

		public ReceiverThread() {
			super("ReceiverThread");
		}

		@Override
		public void run() {
			while (true) {

				// allocate buffer
				byte[] buffer = new byte[Properties.std.getInt("RX_BUFFER_SIZE") + 1];

				DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

				try {
					socket.receive(datagram);
				} catch (IOException e) {
					LOG.severe("Could not receive datagram: " + e.getMessage());
					e.printStackTrace();
					continue;
				}

				// TODO: Dispatch to worker thread
				datagramReceived(datagram);
			}
		}
	}

	public DTLSLayer(int port, boolean daemon) throws SocketException {
		this.socket = new DatagramSocket(port);
		this.receiverThread = new ReceiverThread();

		receiverThread.setDaemon(daemon);

		this.receiverThread.start();
	}

	public DTLSLayer() throws SocketException {
		this(0, true); // use any available port on the local host machine
	}

	@Override
	protected void doSendMessage(Message message) throws IOException {

		EndpointAddress peerAddress = message.getPeerAddress();

		DTLSSession session = dtlsSessions.get(peerAddress.toString());

		Record record = null;
		Handshaker handshaker = null;

		if (session == null) {
			// no session with endpoint available, create new empty session,
			// start fresh handshake
			session = new DTLSSession(true);
			dtlsSessions.put(peerAddress.toString(), session);
			handshaker = new ClientHandshaker(peerAddress, message, session);

		} else {
			if (session.isActive()) {
				// session to peer is active, send encrypted message
				DTLSMessage fragment = new ApplicationMessage(message.toByteArray());
				record = new Record(ContentType.APPLICATION_DATA, session.getWriteEpoch(), session.getSequenceNumber(), fragment, session);

			} else {
				// try resuming session
				handshaker = new ResumingClientHandshaker(peerAddress, message, session);
			}
		}

		// get starting handshake message
		if (handshaker != null) {
			handshakers.put(peerAddress.toString(), handshaker);

			DTLSFlight flight = handshaker.getStartHandshakeMessage();
			flight.setPeerAddress(peerAddress);
			flight.setSession(session);

			flights.put(peerAddress.toString(), flight);

			scheduleRetransmission(flight);
			sendFlight(flight);
		}

		if (record != null) {

			// retrieve payload
			System.out.println(record.toString());
			byte[] payload = record.toByteArray();

			// create datagram
			DatagramPacket datagram = new DatagramPacket(payload, payload.length, peerAddress.getAddress(), peerAddress.getPort());

			// remember when this message was sent for the first time
			// set timestamp only once in order
			// to handle retransmissions correctly
			if (message.getTimestamp() == -1) {
				message.setTimestamp(System.nanoTime());
			}

			// send it over the UDP socket
			socket.send(datagram);
		}
	}

	@Override
	protected void doReceiveMessage(Message msg) {
		deliverMessage(msg);
	}

	private void datagramReceived(DatagramPacket datagram) {

		if (datagram.getLength() > 0) {

			long timestamp = System.nanoTime();

			EndpointAddress peerAddress = new EndpointAddress(datagram.getAddress(), datagram.getPort());

			DTLSSession session = dtlsSessions.get(peerAddress.toString());
			Handshaker handshaker = handshakers.get(peerAddress.toString());
			byte[] data = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getLength());

			// TODO multiplex message types: DTLS or CoAP
			List<Record> records = Record.fromByteArray(data);

			for (Record record : records) {
				record.setSession(session);

				Message msg = null;

				ContentType contentType = record.getType();

				switch (contentType) {
				case APPLICATION_DATA:
					if (session == null) {
						// There is no session available, so no application data
						// should be received, discard it
						LOG.info("Discarded unexpected application data message.");
						return;
					}
					ApplicationMessage applicationData = (ApplicationMessage) record.getFragment();
					msg = Message.fromByteArray(applicationData.getData());
					break;

				case ALERT:
				case CHANGE_CIPHER_SPEC:
				case HANDSHAKE:
					if (handshaker == null) {
						/*
						 * A handshake message received, but no handshaker
						 * available: this must mean that we either received a
						 * HELLO_REQUEST (from server) or a CLIENT_HELLO (from
						 * client)
						 */
						HandshakeMessage message = (HandshakeMessage) record.getFragment();

						switch (message.getMessageType()) {
						case HELLO_REQUEST:
							// client side
							if (session == null) {
								// create new session
								session = new DTLSSession(true);
								// store session according to peer address
								dtlsSessions.put(peerAddress.toString(), session);

								LOG.finest("Client: Created new session with peer: " + peerAddress.toString());
							}
							handshaker = new ClientHandshaker(peerAddress, null, session);
							handshakers.put(peerAddress.toString(), handshaker);
							break;

						case CLIENT_HELLO:
							/*
							 * Server side: server received a client hello:
							 * check first if client wants to resume a session
							 * (message must contain session identifier) and
							 * then check if particular session still available,
							 * otherwise conduct full handshake with fresh
							 * session.
							 */
							
							ClientHello clientHello = (ClientHello) message;
							session = getSessionByIdentifier(clientHello.getSessionId().getSessionId());
							
							if (session == null) {
								// create new session
								session = new DTLSSession(false);
								// store session according to peer address
								dtlsSessions.put(peerAddress.toString(), session);

								LOG.info("Server: Created new session with peer: " + peerAddress.toString());
								handshaker = new ServerHandshaker(peerAddress, getCertificates(), session);
							} else {
								handshaker = new ResumingServerHandshaker(peerAddress, session);
							}
							handshakers.put(peerAddress.toString(), handshaker);
							break;

						default:
							LOG.severe("Received unexpected first handshake message:\n");
							System.out.println(message.toString());
							break;
						}
					}

					DTLSFlight flight = handshaker.processMessage(record);

					if (flight != null) {
						// cancel previous flight, since we are now able to send
						// next one
						DTLSFlight prevFlight = flights.get(peerAddress.toString());
						if (prevFlight != null) {
							prevFlight.getRetransmitTask().cancel();
							prevFlight.setRetransmitTask(null);
							flights.remove(peerAddress.toString());
						}

						flight.setPeerAddress(peerAddress);
						flight.setSession(session);

						if (flight.isRetransmissionNeeded()) {
							flights.put(peerAddress.toString(), flight);
							scheduleRetransmission(flight);
						}
						try {
							// Collections.shuffle(flight.getMessages());
							sendFlight(flight);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					break;

				default:
					LOG.severe("Received unknown DTLS record:\n" + data.toString());
					break;
				}

				if (msg != null) {

					// remember when this message was received
					msg.setTimestamp(timestamp);

					msg.setPeerAddress(new EndpointAddress(datagram.getAddress(), datagram.getPort()));

					if (datagram.getLength() > Properties.std.getInt("RX_BUFFER_SIZE")) {
						LOG.info(String.format("Marking large datagram for blockwise transfer: %s", msg.key()));
						msg.requiresBlockwise(true);
					}

					// protect against unknown exceptions
					try {

						// call receive handler
						receiveMessage(msg);

					} catch (Exception e) {

					}
				}
			}
		}

	}

	public boolean isDaemon() {
		return receiverThread.isDaemon();
	}

	public int getPort() {
		return socket.getLocalPort();
	}

	/**
	 * Searches through all stored sessions and returns that session which
	 * matches the session identifier or <code>null</code> if no such session
	 * available. This method is used when the server receives a
	 * {@link ClientHello} containing a session identifier indicating that the
	 * client wants to resume a previous session. If a matching session is
	 * found, the server will resume the session with a abbreviated handshake,
	 * otherwise a full handshake (with new session identifier in
	 * {@link ServerHello}) is conducted.
	 * 
	 * @param sessionID
	 *            the client's session identifier.
	 * @return the session which matches the session identifier or
	 *         <code>null</code> if no such session exists.
	 */
	private DTLSSession getSessionByIdentifier(byte[] sessionID) {
		if (sessionID == null) {
			return null;
		}
		for (Entry<String, DTLSSession> entry : dtlsSessions.entrySet()) {
			byte[] id = entry.getValue().getSessionIdentifier().getSessionId();
			if (Arrays.equals(sessionID, id)) {
				return entry.getValue();
			}
		}

		return null;
	}

	private X509Certificate[] getCertificates() {
		X509Certificate[] certificates = new X509Certificate[1];

		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			FileInputStream in = new FileInputStream("C:\\Users\\Jucker\\git\\Californium\\src\\ch\\ethz\\inf\\vs\\californium\\dtls\\ec3.crt");

			Certificate certificate = cf.generateCertificate(in);
			in.close();

			certificates[0] = (X509Certificate) certificate;
		} catch (Exception e) {
			LOG.severe("Could not create the certificates.");
			e.printStackTrace();
			certificates = null;
		}

		return certificates;
	}

	private void sendFlight(DTLSFlight flight) throws IOException {
		// FIXME debug infos
		boolean allInOneRecord = true;
		if (flight.getTries() > 0) {
			// LOG.info("Retransmit current flight:\n" +
			// flight.getMessages().toString());
		}
		if (allInOneRecord) {
			byte[] payload = new byte[0];
			for (Record record : flight.getMessages()) {
				if (flight.getTries() > 0) {
					// adjust the record sequence number
					int epoch = record.getEpoch();
					record.setSequenceNumber(flight.getSession().getSequenceNumber(epoch));
				}
	
				// retrieve payload
				payload = ByteArrayUtils.concatenate(payload, record.toByteArray());
	
			}
			// create datagram
			DatagramPacket datagram = new DatagramPacket(payload, payload.length, flight.getPeerAddress().getAddress(), flight.getPeerAddress().getPort());
	
			// send it over the UDP socket
			socket.send(datagram);
		} else {
			for (Record record : flight.getMessages()) {
				if (flight.getTries() > 0) {
					// adjust the record sequence number
					int epoch = record.getEpoch();
					record.setSequenceNumber(flight.getSession().getSequenceNumber(epoch));
				}

				// retrieve payload
				byte[] payload = record.toByteArray();

				// create datagram
				DatagramPacket datagram = new DatagramPacket(payload, payload.length, flight.getPeerAddress().getAddress(), flight.getPeerAddress().getPort());

				// send it over the UDP socket
				socket.send(datagram);
			}
		}
	}

	private void handleTimeout(DTLSFlight flight) {

		final int max = Properties.std.getInt("MAX_RETRANSMIT");

		// check if limit of retransmissions reached
		if (flight.getTries() < max) {

			flight.incrementTries();

			try {
				sendFlight(flight);
			} catch (IOException e) {
				return;
			}

			// schedule next retransmission
			scheduleRetransmission(flight);

		} else {
			// TODO maximum tries reached
		}
	}

	private void scheduleRetransmission(DTLSFlight flight) {

		// cancel existing schedule (if any)
		if (flight.getRetransmitTask() != null) {
			flight.getRetransmitTask().cancel();
		}

		// create new retransmission task
		flight.setRetransmitTask(new RetransmitTask(flight));

		// calculate timeout using exponential back-off
		if (flight.getTimeout() == 0) {
			// use initial timeout
			flight.setTimeout(initialTimeout());
		} else {
			// double timeout
			flight.incrementTimeout();
		}

		// schedule retransmission task
		timer.schedule(flight.getRetransmitTask(), flight.getTimeout());
	}

	private int initialTimeout() {
		// TODO load this from config file
		return 1000;
	}
}

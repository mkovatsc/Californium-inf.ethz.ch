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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.dtls.AlertMessage;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;
import ch.ethz.inf.vs.californium.dtls.ApplicationMessage;
import ch.ethz.inf.vs.californium.dtls.ClientHandshaker;
import ch.ethz.inf.vs.californium.dtls.ClientHello;
import ch.ethz.inf.vs.californium.dtls.ContentType;
import ch.ethz.inf.vs.californium.dtls.DTLSFlight;
import ch.ethz.inf.vs.californium.dtls.DTLSMessage;
import ch.ethz.inf.vs.californium.dtls.DTLSSession;
import ch.ethz.inf.vs.californium.dtls.FragmentedHandshakeMessage;
import ch.ethz.inf.vs.californium.dtls.HandshakeException;
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
public class DTLSLayer extends AbstractLayer {

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
		
		// remember when this message was sent for the first time
		// set timestamp only once in order
		// to handle retransmissions correctly
		if (message.getTimestamp() == -1) {
			message.setTimestamp(System.nanoTime());
		}

		EndpointAddress peerAddress = message.getPeerAddress();

		DTLSSession session = dtlsSessions.get(peerAddress.toString());
		
		/*
		 * When the DTLS layer receives a message from an upper layer, there is
		 * either a already a DTLS session available with the peer or a new
		 * handshake must be executed. If a session is available and active, the
		 * message will be encrypted and send to the peer, otherwise a short
		 * handshake will be initiated.
		 */
		Record encryptedMessage = null;
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
				encryptedMessage = new Record(ContentType.APPLICATION_DATA, session.getWriteEpoch(), session.getSequenceNumber(), fragment, session);

			} else if (message.getRetransmissioned() > 0) {
				// TODO when message retransmitted from TransactionLayer: what to do?
				return;
			} else {
				// try resuming session
				handshaker = new ResumingClientHandshaker(peerAddress, message, session);
			}
		}
		
		DTLSFlight flight = new DTLSFlight();
		// the CoAP message can not be encrypted since no session with peer
		// available, start DTLS handshake protocol
		if (handshaker != null) {
			// get starting handshake message
			handshakers.put(peerAddress.toString(), handshaker);

			flight = handshaker.getStartHandshakeMessage();
			flights.put(peerAddress.toString(), flight);
			scheduleRetransmission(flight);
		}
		
		// the CoAP message has been encrypted and can be sent to the peer
		if (encryptedMessage != null) {
			flight.addMessage(encryptedMessage);
		}
		
		flight.setPeerAddress(peerAddress);
		flight.setSession(session);
		sendFlight(flight);
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

			try {
				// TODO multiplex message types: DTLS or CoAP
				List<Record> records = Record.fromByteArray(data);

				for (Record record : records) {
					record.setSession(session);

					Message msg = null;

					ContentType contentType = record.getType();
					DTLSFlight flight = null;
					switch (contentType) {
					case APPLICATION_DATA:
						if (session == null) {
							// There is no session available, so no application data
							// should be received, discard it
							LOG.info("Discarded unexpected application data message from " + peerAddress.toString());
							return;
						}
						// at this point, the current handshaker is not needed
						// anymore, remove it
						handshakers.remove(peerAddress.toString());

						ApplicationMessage applicationData = (ApplicationMessage) record.getFragment();
						msg = Message.fromByteArray(applicationData.getData());
						break;

					case ALERT:
					case CHANGE_CIPHER_SPEC:
					case HANDSHAKE:
						if (handshaker == null) {
							/*
							 * A handshake message received, but no handshaker
							 * available: this must mean that we either received
							 * a HelloRequest (from server) or a ClientHello
							 * (from client) => initialize appropriate
							 * handshaker type
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
								 * check first if client wants to resume a
								 * session (message must contain session
								 * identifier) and then check if particular
								 * session still available, otherwise conduct
								 * full handshake with fresh session.
								 */

								if (!(message instanceof FragmentedHandshakeMessage)) {
									// check if session identifier set
									ClientHello clientHello = (ClientHello) message;
									session = getSessionByIdentifier(clientHello.getSessionId().getSessionId());
								}
								

								if (session == null) {
									// create new session
									session = new DTLSSession(false);
									// store session according to peer address
									dtlsSessions.put(peerAddress.toString(), session);

									LOG.info("Server: Created new session with peer: " + peerAddress.toString());
									handshaker = new ServerHandshaker(peerAddress, session);
								} else {
									handshaker = new ResumingServerHandshaker(peerAddress, session);
								}
								handshakers.put(peerAddress.toString(), handshaker);
								break;

							default:
								LOG.severe("Received unexpected first handshake message from " + peerAddress.toString() + ":\n" + message.toString());
								break;
							}
						}

						flight = handshaker.processMessage(record);

						break;

					default:
						LOG.severe("Received unknown DTLS record from " + peerAddress.toString() + ":\n" + ByteArrayUtils.toHexString(data));
						break;
					}

					if (flight != null) {
						cancelPreviousFlight(peerAddress);

						flight.setPeerAddress(peerAddress);
						flight.setSession(session);

						if (flight.isRetransmissionNeeded()) {
							flights.put(peerAddress.toString(), flight);
							scheduleRetransmission(flight);
						}

						sendFlight(flight);
					}

					if (msg != null) {

						// remember when this message was received
						msg.setTimestamp(timestamp);

						msg.setPeerAddress(new EndpointAddress(datagram.getAddress(), datagram.getPort()));

						if (datagram.getLength() > Properties.std.getInt("RX_BUFFER_SIZE")) {
							LOG.info(String.format("Marking large datagram for blockwise transfer: %s", msg.key()));
							msg.requiresBlockwise(true);
						}

						receiveMessage(msg);
					}
				}

			} catch (Exception e) {
				/*
				 * If it is a known handshake failure, send the specific Alert,
				 * otherwise the general Handshake_Failure Alert. 
				 */
				DTLSFlight flight = new DTLSFlight();
				flight.setRetransmissionNeeded(false);
				flight.setPeerAddress(peerAddress);
				flight.setSession(session);
				
				AlertMessage alert;
				if (e instanceof HandshakeException) {
					alert = ((HandshakeException) e).getAlert();
					LOG.severe("Handshake Exception (" + peerAddress.toString() + "): " + e.getMessage());
				} else {
					alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
					LOG.severe("Unknown Exception (" + peerAddress + ").");
				}
				LOG.severe("Datagram which lead to exception (" + peerAddress + "): " + ByteArrayUtils.toHexString(data));
				LOG.severe(logStackTrace(e));
				
				if (session == null) {
					// if the first received message failed, no session has been set
					session = new DTLSSession(false);
				}
				cancelPreviousFlight(peerAddress);
				
				flight.addMessage(new Record(ContentType.ALERT, session.getWriteEpoch(), session.getSequenceNumber(), alert, session));
				sendFlight(flight);
				
				
			}
		}

	}
	
	private String logStackTrace(Exception e) {
		StringBuilder builder = new StringBuilder();
		builder.append("Crash: ");
		builder.append(e.getMessage());
		builder.append('\n');
		builder.append("\t\t");
		builder.append("Stacktrace for ");
		builder.append(e.getClass().getName());
		builder.append(":\n");
		for (StackTraceElement elem : e.getStackTrace()) {
			builder.append("\t\t");
			builder.append(elem.getClassName());
			builder.append('.');
			builder.append(elem.getMethodName());
			builder.append('(');
			builder.append(elem.getFileName());
			builder.append(':');
			builder.append(elem.getLineNumber());
			builder.append(")\n");
		}
		
		return builder.toString();
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
			// FIXME session identifiers may not be set, when the handshake failed after the initial message
			// these sessions must be deleted when this happens
			try {
				byte[] id = entry.getValue().getSessionIdentifier().getSessionId();
				if (Arrays.equals(sessionID, id)) {
					return entry.getValue();
				}
			} catch (Exception e) {
				continue;
			}
		}

		return null;
	}
	
	private void sendFlight(DTLSFlight flight) {
		byte[] payload = new byte[] {};
		// the overhead for the record header (13 bytes) and the handshake
		// header (12 bytes) is 25 bytes
		int maxPayloadSize = Properties.std.getInt("MAX_FRAGMENT_LENGTH") + 25;
		
		// put as many records into one datagram as allowed by the block size
		List<DatagramPacket> datagrams = new ArrayList<DatagramPacket>();

		for (Record record : flight.getMessages()) {
			if (flight.getTries() > 0) {
				// adjust the record sequence number
				int epoch = record.getEpoch();
				record.setSequenceNumber(flight.getSession().getSequenceNumber(epoch));
			}
			
			byte[] recordBytes = record.toByteArray();
			if (payload.length + recordBytes.length > maxPayloadSize) {
				// can't add the next record, send current payload as datagram
				DatagramPacket datagram = new DatagramPacket(payload, payload.length, flight.getPeerAddress().getAddress(), flight.getPeerAddress().getPort());
				datagrams.add(datagram);
				payload = new byte[] {};
			}

			// retrieve payload
			payload = ByteArrayUtils.concatenate(payload, recordBytes);
		}
		DatagramPacket datagram = new DatagramPacket(payload, payload.length, flight.getPeerAddress().getAddress(), flight.getPeerAddress().getPort());
		datagrams.add(datagram);

		// send it over the UDP socket
		try {
			for (DatagramPacket datagramPacket : datagrams) {
				socket.send(datagramPacket);
			}
			
		} catch (IOException e) {
			LOG.severe("Could not send the datagram: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void handleTimeout(DTLSFlight flight) {

		final int max = Properties.std.getInt("MAX_RETRANSMIT");

		// check if limit of retransmissions reached
		if (flight.getTries() < max) {

			flight.incrementTries();

			sendFlight(flight);

			// schedule next retransmission
			scheduleRetransmission(flight);

		} else {
			LOG.info("Maximum retransmissions reached.");
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
	
	/**
	 * Cancels the retransmission timer of the previous flight (if available).
	 * 
	 * @param peerAddress
	 *            the peer's address.
	 */
	private void cancelPreviousFlight(EndpointAddress peerAddress) {
		DTLSFlight previousFlight = flights.get(peerAddress.toString());
		if (previousFlight != null) {
			previousFlight.getRetransmitTask().cancel();
			previousFlight.setRetransmitTask(null);
			flights.remove(peerAddress.toString());
		}
	}

	private int initialTimeout() {
		return Properties.std.getInt("RETRANSMISSION_TIMEOUT");
	}

}

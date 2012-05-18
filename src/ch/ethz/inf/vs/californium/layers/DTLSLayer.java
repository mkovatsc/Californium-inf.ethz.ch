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
import java.util.Map;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.dtls.AlertMessage;
import ch.ethz.inf.vs.californium.dtls.ApplicationMessage;
import ch.ethz.inf.vs.californium.dtls.ChangeCipherSpecMessage;
import ch.ethz.inf.vs.californium.dtls.ClientHandshaker;
import ch.ethz.inf.vs.californium.dtls.ContentType;
import ch.ethz.inf.vs.californium.dtls.DTLSMessage;
import ch.ethz.inf.vs.californium.dtls.DTLSSession;
import ch.ethz.inf.vs.californium.dtls.HandshakeMessage;
import ch.ethz.inf.vs.californium.dtls.Handshaker;
import ch.ethz.inf.vs.californium.dtls.Record;
import ch.ethz.inf.vs.californium.dtls.ServerHandshaker;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * 
 * @author Stefan Jucker
 * 
 */
public class DTLSLayer extends Layer {

	protected DatagramSocket socket;

	protected ReceiverThread receiverThread;

	private Map<String, DTLSSession> dtlsSessions;

	private Map<String, Handshaker> handshakers;

	class ReceiverThread extends Thread {

		public ReceiverThread() {
			super("ReceiverThread");
		}

		@Override
		public void run() {
			// always listen for incoming datagrams
			while (true) {

				// allocate buffer
				byte[] buffer = new byte[Properties.std.getInt("RX_BUFFER_SIZE") + 1];

				// initialize new datagram
				DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

				// receive datagram
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
		// initialize members
		this.socket = new DatagramSocket(port);
		this.receiverThread = new ReceiverThread();

		// decide if receiver thread terminates with main thread
		receiverThread.setDaemon(false);

		// start listening right from the beginning
		this.receiverThread.start();

		this.dtlsSessions = new HashMap<String, DTLSSession>();
		this.handshakers = new HashMap<String, Handshaker>();

	}

	public DTLSLayer() throws SocketException {
		this(0, true); // use any available port on the local host machine
	}

	public void setDaemon(boolean on) {
		receiverThread.setDaemon(on);
	}

	@Override
	protected void doSendMessage(Message msg) throws IOException {

		EndpointAddress endpoint = new EndpointAddress(msg.getPeerAddress().getAddress(), msg.getPeerAddress().getPort());

		DTLSSession session = dtlsSessions.get(endpoint.toString());
		Handshaker handshaker = handshakers.get(endpoint.toString());

		if (handshaker == null) {
			long current = System.currentTimeMillis();
			LOG.info("Start Handshake");

			if (session != null) {
				// there is a session available, let's try to resume it
				handshaker = new ClientHandshaker(socket, endpoint, session);

			} else {
				//
				handshaker = new ClientHandshaker(socket, endpoint);
			}
			handshakers.put(endpoint.toString(), handshaker);
			handshaker.startHandshake();

			// wait until handshake completed
			try {
				// TODO handle alerts, check session
				session = handshaker.queue.take();
				LOG.info("End Handshake: Duration " + (System.currentTimeMillis() - current) + "ms");

				dtlsSessions.put(endpoint.getAddress().toString() + endpoint.getPort(), session);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// TODO check handshaker

		DTLSMessage fragment = new ApplicationMessage(msg.toByteArray());
		Record r = new Record(ContentType.APPLICATION_DATA, 0, fragment);

		// retrieve payload
		byte[] payload = r.toByteArray(handshaker);

		// create datagram
		DatagramPacket datagram = new DatagramPacket(payload, payload.length, msg.getPeerAddress().getAddress(), msg.getPeerAddress().getPort());

		// remember when this message was sent for the first time
		// set timestamp only once in order
		// to handle retransmissions correctly
		if (msg.getTimestamp() == -1) {
			msg.setTimestamp(System.nanoTime());
		}

		// send it over the UDP socket
		socket.send(datagram);
	}

	@Override
	protected void doReceiveMessage(Message msg) {

		// pass message to registered receivers
		deliverMessage(msg);
	}

	private void datagramReceived(DatagramPacket datagram) {

		if (datagram.getLength() > 0) {

			// the peer's address
			EndpointAddress endpoint = new EndpointAddress(datagram.getAddress(), datagram.getPort());
			Handshaker handshaker = handshakers.get(endpoint.toString());

			// get current time
			long timestamp = System.nanoTime();

			byte[] data = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getLength());

			Record record = Record.fromByteArray(data);
			ContentType contentType = record.getType();
			DTLSMessage fragment = record.getFragment(handshaker);
			
			Message msg = null;

			LOG.info("DTLS Message received.");
			System.out.println(record.toString());

			switch (contentType) {
			case APPLICATION_DATA:
				ApplicationMessage applicationData = (ApplicationMessage) fragment;

				msg = Message.fromByteArray(applicationData.getData());
				break;
			case ALERT:
				AlertMessage alertMessage = (AlertMessage) fragment;
				
				break;

			case CHANGE_CIPHER_SPEC:
				ChangeCipherSpecMessage changeCipherSpecMessage = (ChangeCipherSpecMessage) fragment;

				break;

			case HANDSHAKE:
				HandshakeMessage message = (HandshakeMessage) fragment;

				if (handshaker == null) {
					switch (message.getMessageType()) {
					case HELLO_REQUEST:
						// TODO client is asked to renegotiate session
						DTLSSession session = dtlsSessions.get(endpoint.toString());
						handshaker = new ClientHandshaker(socket, endpoint, session);
						handshakers.put(endpoint.toString(), handshaker);

						try {
							handshaker.startHandshake();
						} catch (IOException e) {
							e.printStackTrace();
						}
						// handshake started, don't do anything anymore
						return;

					case CLIENT_HELLO:
						handshaker = new ServerHandshaker(socket, endpoint, getCertificates());
						handshakers.put(endpoint.toString(), handshaker);
						break;

					default:
						// TODO What about reordering?
						LOG.severe("Received unexpected first handshake message:");
						System.out.println(message.toString());
						break;
					}

				}
				try {
					handshaker.processMessage(message);
				} catch (IOException e) {
					e.printStackTrace();
				}

				return;

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
			} else {
				LOG.severe("Illegal datagram received:\n" + data.toString());
			}
		}

	}

	public boolean isDaemon() {
		return receiverThread.isDaemon();
	}

	public int getPort() {
		return socket.getLocalPort();
	}

	private X509Certificate[] getCertificates() {
		X509Certificate[] certificates = new X509Certificate[1];

		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			FileInputStream in = new FileInputStream("C:\\Users\\Jucker\\git\\Californium\\src\\ch\\ethz\\inf\\vs\\californium\\dtls\\ec.crt");
			Certificate certificate = cf.generateCertificate(in);
			in.close();

			certificates[0] = (X509Certificate) certificate;
		} catch (Exception e) {
			certificates = null;
		}

		return certificates;
	}
}

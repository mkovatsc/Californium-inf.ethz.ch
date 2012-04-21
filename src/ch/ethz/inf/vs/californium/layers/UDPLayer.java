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
import java.util.Arrays;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class UDPLayer exchanges CoAP messages with remote endpoints using UDP
 * datagrams. It is an unreliable channel and thus datagrams may arrive out of
 * order, appear duplicated, or are lost without any notice, especially on lossy
 * physical layers.
 * <p>
 * The UDPLayer is the base layer of the stack, sub-calssing {@link Layer}. Any
 * {@link UpperLayer} can be stacked on top, using a {@link ch.ethz.inf.vs.californium.coap.Communicator} as
 * stack builder.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class UDPLayer extends Layer {

// Members /////////////////////////////////////////////////////////////////////

	// The UDP socket used to send and receive datagrams
	// TODO Use MulticastSocket
	private DatagramSocket socket;

	// The thread that listens on the socket for incoming datagrams
	private ReceiverThread receiverThread;

// Inner Classes ///////////////////////////////////////////////////////////////

	class ReceiverThread extends Thread {
		
		public ReceiverThread() {
			super("ReceiverThread");
		}
		
		@Override
		public void run() {
			// always listen for incoming datagrams
			while (true) {

				// allocate buffer
				byte[] buffer = new byte[Properties.std.getInt("RX_BUFFER_SIZE")+1]; // +1 to check for > RX_BUFFER_SIZE

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

// Constructors ////////////////////////////////////////////////////////////////

	/*
	 * Constructor for a new UDP layer
	 * 
	 * @param port The local UDP port to listen for incoming messages
	 * @param daemon True if receiver thread should terminate with main thread
	 */
	public UDPLayer(int port, boolean daemon) throws SocketException {
		// initialize members
		this.socket = new DatagramSocket(port);
		this.receiverThread = new ReceiverThread();

		// decide if receiver thread terminates with main thread
		receiverThread.setDaemon(daemon);

		// start listening right from the beginning
		this.receiverThread.start();

	}

	/*
	 * Constructor for a new UDP layer
	 */
	public UDPLayer() throws SocketException {
		this(0, true); // use any available port on the local host machine
	}

// Commands ////////////////////////////////////////////////////////////////////

	/*
	 * Decides if the listener thread persists after the main thread terminates
	 * 
	 * @param on True if the listener thread should stay alive after the main
	 * thread terminates. This is useful for e.g. server applications
	 */
	public void setDaemon(boolean on) {
		receiverThread.setDaemon(on);
	}

// I/O implementation //////////////////////////////////////////////////////////

	@Override
	protected void doSendMessage(Message msg) throws IOException {

		// retrieve payload
		byte[] payload = msg.toByteArray();
		
		// create datagram
		DatagramPacket datagram = new DatagramPacket(payload, payload.length,
			msg.getPeerAddress().getAddress(), msg.getPeerAddress().getPort() );

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

// Internal ////////////////////////////////////////////////////////////////////

	private void datagramReceived(DatagramPacket datagram) {

		if (datagram.getLength() > 0) {
		
			// get current time
			long timestamp = System.nanoTime();
	
			// extract message data from datagram
			byte[] data = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getLength());
	
			// create new message from the received data
			Message msg = Message.fromByteArray(data);
			
			if (msg!=null) {
	
				// remember when this message was received
				msg.setTimestamp(timestamp);
				
				msg.setPeerAddress(new EndpointAddress(datagram.getAddress(), datagram.getPort()));
				
				if (datagram.getLength()>Properties.std.getInt("RX_BUFFER_SIZE")) {
					LOG.info(String.format("Marking large datagram for blockwise transfer: %s", msg.key()));
					msg.requiresBlockwise(true);
				}

				// protect against unknown exceptions
				try {
					
					// call receive handler
					receiveMessage(msg);
					
				} catch (Exception e) {
					StringBuilder builder = new StringBuilder();
					builder.append("Crash: ");
					builder.append(e.getMessage());
					builder.append('\n');
					builder.append("                    ");
					builder.append("Stacktrace for ");
					builder.append(e.getClass().getName());
					builder.append(":\n");
					for (StackTraceElement elem : e.getStackTrace()) {
						builder.append("                    ");
						builder.append(elem.getClassName());
						builder.append('.');
						builder.append(elem.getMethodName());
						builder.append('(');
						builder.append(elem.getFileName());
						builder.append(':');
						builder.append(elem.getLineNumber());
						builder.append(")\n");
					}
					
					LOG.severe(builder.toString());
				}
			} else {
				LOG.severe("Illeagal datagram received:\n" + data.toString());
			}
			
		} else {
			
			LOG.info(String.format("Dropped empty datagram from: %s:%d", datagram.getAddress().getHostName(), datagram.getPort()));
		}
	}

// Queries /////////////////////////////////////////////////////////////////////

	/*
	 * Checks whether the listener thread persists after the main thread
	 * terminates
	 * 
	 * @return True if the listener thread stays alive after the main thread
	 * terminates. This is useful for e.g. server applications
	 */
	public boolean isDaemon() {
		return receiverThread.isDaemon();
	}

	public int getPort() {
		return socket.getLocalPort();
	}
	
	public String getStats() {
		StringBuilder stats = new StringBuilder();

		stats.append("UDP port: ");
		stats.append(getPort());
		stats.append('\n');
		stats.append("Messages sent:     ");
		stats.append(numMessagesSent);
		stats.append('\n');
		stats.append("Messages received: ");
		stats.append(numMessagesReceived);
		
		return stats.toString();
	}
}

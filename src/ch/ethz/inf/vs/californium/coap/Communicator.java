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
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.coap;

import java.io.IOException;
import java.net.SocketException;

import ch.ethz.inf.vs.californium.layers.AdverseLayer;
import ch.ethz.inf.vs.californium.layers.TransactionLayer;
import ch.ethz.inf.vs.californium.layers.MatchingLayer;
import ch.ethz.inf.vs.californium.layers.TransferLayer;
import ch.ethz.inf.vs.californium.layers.UDPLayer;
import ch.ethz.inf.vs.californium.layers.UpperLayer;
import ch.ethz.inf.vs.californium.util.Log;

/**
 * The class Communicator provides the message passing system and builds the
 * communication stack through which messages are sent and received. As a
 * subclass of {@link UpperLayer} it is actually a composite layer that contains
 * the subsequent layers in the order defined in {@link #buildStack()}.
 * <p>
 * Endpoints must register as a receiver using {@link #registerReceiver(MessageReceiver)}.
 * Prior to that, they should configure the Communicator using @link {@link #setup(int, boolean)}.
 * A client only using {@link Request}s are not required to do any of that.
 * Here, {@link Message}s will create the required instance automatically.
 * <p>
 * The Communicator implements the Singleton pattern, as there should only be
 * one stack per endpoint and it is required in different contexts to send a
 * message. It is not using the Enum approach because it still needs to inherit
 * from {@link UpperLayer}.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class Communicator extends UpperLayer {
	
// Static Attributes ///////////////////////////////////////////////////////////
	
	private volatile static Communicator singleton = null;
	private static int udpPort = 0;
	private static boolean runAsDaemon = true;

// Members /////////////////////////////////////////////////////////////////////

	protected TransferLayer transferLayer;
	protected MatchingLayer matchingLayer;
	protected TransactionLayer transactionLayer;
	protected AdverseLayer adverseLayer;
	protected UDPLayer udpLayer;
	
// Constructors ////////////////////////////////////////////////////////////////

	/*
	 * Constructor for a new Communicator
	 * 
	 * @param port The local UDP port to listen for incoming messages
	 * @param daemon True if receiver thread should terminate with main thread
	 * @param defaultBlockSize The default block size used for block-wise transfers
	 *        or -1 to disable outgoing block-wise transfers
	 */	
	private Communicator() throws SocketException {
		
		// initialize layers
		matchingLayer = new MatchingLayer();
		transferLayer = new TransferLayer();
		transactionLayer = new TransactionLayer();
		adverseLayer = new AdverseLayer();
		udpLayer = new UDPLayer(udpPort, runAsDaemon);

		// connect layers
		buildStack();
		
	}
	
	public static Communicator getInstance() {
		
		if (singleton==null) {
			synchronized (Communicator.class) {
				if (singleton==null) {
					try {
						singleton = new Communicator();
					} catch (SocketException e) {
						System.err.printf("Failed to create Communicator: %s\n", e.getMessage());
						System.exit(-1);
					}
				}
			}
		}
		return singleton;
	}
	
	public static void setup(int port, boolean daemon) {
		if (singleton==null) {
			synchronized (Communicator.class) {
				if (singleton==null) {
					udpPort = port;
					runAsDaemon = daemon;
				} else {
					Log.error(Communicator.class, "Communicator already initialized, setup failed");
				}
			}
		}
	}

	// Internal ////////////////////////////////////////////////////////////////

	/*
	 * This method connects the layers in order to build the communication stack
	 * 
	 * It can be overridden by subclasses in order to add further layers, e.g.
	 * for introducing a layer that drops or duplicates messages by a
	 * probabilistic model in order to evaluate the implementation.
	 */
	private void buildStack() {

		this.setLowerLayer(matchingLayer);
		matchingLayer.setLowerLayer(transferLayer);
		transferLayer.setLowerLayer(transactionLayer);
		transactionLayer.setLowerLayer(udpLayer);
		
		//transactionLayer.setLowerLayer(adverseLayer);
		//adverseLayer.setLowerLayer(udpLayer);

	}

	// I/O implementation //////////////////////////////////////////////////////

	@Override
	protected void doSendMessage(Message msg) throws IOException {

		// defensive programming before entering the stack, lower layers should assume a correct message.
		if (msg != null) {
		
			// check message before sending through the stack
			if (msg.getPeerAddress().getAddress()==null) {
				throw new IOException("Remote address not specified");
			}
			
			// delegate to first layer
			sendMessageOverLowerLayer(msg);
		}
	}

	@Override
	protected void doReceiveMessage(Message msg) {

		if (msg instanceof Response) {
			Response response = (Response) msg;

			// initiate custom response handling
			response.handle();
		}

		// pass message to registered receivers
		deliverMessage(msg);

	}

	// Queries /////////////////////////////////////////////////////////////////

	public int port() {
		return udpLayer.getPort();
	}

}

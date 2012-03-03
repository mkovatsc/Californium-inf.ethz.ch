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
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import ch.ethz.inf.vs.californium.layers.AdverseLayer;
import ch.ethz.inf.vs.californium.layers.MessageLayer;
import ch.ethz.inf.vs.californium.layers.TransactionLayer;
import ch.ethz.inf.vs.californium.layers.TransferLayer;
import ch.ethz.inf.vs.californium.layers.UDPLayer;
import ch.ethz.inf.vs.californium.layers.UpperLayer;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class Communicator provides the functionality to build the communication
 * layer stack and to send and receive messages. As a subclass of {@link UpperLayer}
 * it is actually a composite layer that contains the subsequent layers in the
 * top-down order as explained in {@see table}.
 * Hence, the Communicator class is used to encapsulate the various
 * communication layers of the CoAP protocol by providing an appropriate unified
 * interface. Internally, it instantiates the required communication layer
 * classes and connects them accordingly. The Communicator also acts as
 * mediator between endpoint classes and communication layer classes, allowing
 * to specify and query parameters like the UDP port.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class Communicator extends UpperLayer {

	// Constructors ////////////////////////////////////////////////////////////

	/*
	 * Constructor for a new Communicator
	 * 
	 * @param port The local UDP port to listen for incoming messages
	 * @param daemon True if receiver thread should terminate with main thread
	 * @param defaultBlockSize The default block size used for block-wise transfers
	 *        or -1 to disable outgoing block-wise transfers
	 */	
	public Communicator(int port, boolean daemon, int defaultBlockSize) throws SocketException {
		
		// initialize Token Manager
		tokenManager = new TokenManager();
		
		// initialize layers
		transferLayer = new TransferLayer(tokenManager, defaultBlockSize);
		transactionLayer = new TransactionLayer(tokenManager);
		messageLayer = new MessageLayer();
		adverseLayer = new AdverseLayer();
		udpLayer = new UDPLayer(port, daemon);

		// connect layers
		buildStack();
		
	}
	
	/*
	 * Constructor for a new Communicator
	 * 
	 * @param port The local UDP port to listen for incoming messages
	 * @param daemon True if receiver thread should terminate with main thread
	 */
	public Communicator(int port, boolean daemon) throws SocketException {
		this(port, daemon, Properties.std.getInt("DEFAULT_BLOCK_SIZE"));
	}

	/*
	 * Constructor for a new Communicator
	 */
	public Communicator() throws SocketException {
		this(0, true);
	}

	// Internal ////////////////////////////////////////////////////////////////

	/*
	 * This method connects the layers in order to build the communication stack
	 * 
	 * It can be overridden by subclasses in order to add further layers, e.g.
	 * for introducing a layer that drops or duplicates messages by a
	 * probabilistic model in order to evaluate the implementation.
	 */
	protected void buildStack() {

		this.setLowerLayer(transferLayer);
		transferLayer.setLowerLayer(transactionLayer);
		//this.setLowerLayer(transactionLayer);
		transactionLayer.setLowerLayer(messageLayer);
		messageLayer.setLowerLayer(udpLayer);
		//messageLayer.setLowerLayer(adverseLayer);
		//adverseLayer.setLowerLayer(udpLayer);

	}

	// I/O implementation //////////////////////////////////////////////////////

	@Override
	protected void doSendMessage(Message msg) throws IOException {
		
		// check message before sending through the stack
		if (msg.getPeerAddress().getAddress()==null) {
			throw new IOException("Remote address not specified");
		}

		// delegate to first layer
		sendMessageOverLowerLayer(msg);
	}

	@Override
	protected void doReceiveMessage(Message msg) {

		if (msg instanceof Response) {
			Response response = (Response) msg;

			// initiate custom response handling
			response.handle();

		} else if (msg instanceof Request) {
			Request request = (Request) msg;

			request.setCommunicator(this);
		}

		// pass message to registered receivers
		deliverMessage(msg);

	}

	// Queries /////////////////////////////////////////////////////////////////

	public int port() {
		return udpLayer.getPort();
	}

	// Attributes //////////////////////////////////////////////////////////////

	protected TransferLayer transferLayer;
	protected TransactionLayer transactionLayer;
	protected MessageLayer messageLayer;
	protected AdverseLayer adverseLayer;
	protected UDPLayer udpLayer;
	
	protected TokenManager tokenManager;

}

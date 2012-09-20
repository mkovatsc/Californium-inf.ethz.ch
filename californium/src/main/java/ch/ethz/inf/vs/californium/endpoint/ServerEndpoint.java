/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
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

package ch.ethz.inf.vs.californium.endpoint;

import java.net.SocketException;

import ch.ethz.inf.vs.californium.coap.CommunicatorFactory;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class LocalEndpoint provides the functionality of a server endpoint as a
 * subclass of {@link Endpoint}. A server implementation using Cf will override
 * this class to provide custom resources. Internally, the main purpose of this
 * class is to forward received requests to the corresponding resource specified
 * by the Uri-Path option. Furthermore, it implements the root resource to
 * return a brief server description to GET requests with empty Uri-Path.
 * 
 * @author Francesco Corazza
 */
public class ServerEndpoint extends LocalEndpoint {

	private int udpPort = 0;
	private boolean runAsDaemon = false;
	private int transferBlockSize = 0;
	private int requestPerSecond = 0;

	public ServerEndpoint() throws SocketException {
		this(Properties.std.getInt("DEFAULT_PORT"));
	}

	/**
	 * Instantiates a new local endpoint.
	 * 
	 * @param port
	 *            the port
	 * @throws SocketException
	 *             the socket exception
	 */
	public ServerEndpoint(int port) throws SocketException {
		this(port, 0); // let TransferLayer decide default
	}

	/**
	 * Instantiates a new local endpoint.
	 * 
	 * @param port
	 *            the port
	 * @param transferBlockSize
	 *            the default block sze
	 * @throws SocketException
	 *             the socket exception
	 */
	public ServerEndpoint(int port, int transferBlockSize) throws SocketException {
		this(port, transferBlockSize, false, 0); // no runAsDaemon, keep JVM
													// running to
													// handle requests
	}

	/**
	 * Instantiates a new local endpoint.
	 * 
	 * @param port
	 *            the port
	 * @param transferBlockSize
	 *            the default block sze
	 * @param runAsDaemon
	 *            the runAsDaemon
	 * @throws SocketException
	 *             the socket exception
	 */
	public ServerEndpoint(int udpPort, int transferBlockSize, boolean runAsDaemon, int requestPerSecond) throws SocketException {
		super();
		this.udpPort = udpPort;
		this.transferBlockSize = transferBlockSize;
		this.runAsDaemon = runAsDaemon;
		this.requestPerSecond = requestPerSecond;
	}

	@Override
	protected void createCommunicator() {
		// get the communicator factory
		CommunicatorFactory factory = CommunicatorFactory.getInstance();

		// set the parameters of the communicator
		factory.setUdpPort(udpPort);
		factory.setTransferBlockSize(transferBlockSize);
		factory.setRunAsDaemon(runAsDaemon);
		factory.setRequestPerSecond(requestPerSecond);

		// initialize communicator
		Communicator communicator = factory.getCommunicator();

		// register the endpoint as a receiver
		communicator.registerReceiver(this);
	}

	@Override
	protected void responseProduced(Response response) {
		// do nothing
	}
}

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

package ch.ethz.inf.vs.californium.coap;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.layers.CoapStack;
import ch.ethz.inf.vs.californium.layers.HttpStackAsync;
import ch.ethz.inf.vs.californium.layers.Layer;
import ch.ethz.inf.vs.californium.layers.UpperLayer;

/**
 * A factory for creating Communicator objects.
 * 
 * @author Francesco Corazza
 */
public final class CommunicatorFactory {

	protected static final Logger LOG = Logger.getLogger(CommunicatorFactory.class.getName());

	public static CommunicatorFactory getInstance() {
		return CommunicatorFactoryHolder.communicatorFactory;
	}

	private int httpPort = 0;
	private int udpPort = 0;
	private boolean runAsDaemon = false;
	private int transferBlockSize = 0;
	private int requestPerSecond = 0;
	private boolean enableHttp = false;
	private static Communicator COMMUNICATOR;

	public Communicator getCommunicator() {
		if (COMMUNICATOR == null) {
			try {
				if (enableHttp) {
					COMMUNICATOR = new ProxyCommunicator(udpPort, httpPort, runAsDaemon, transferBlockSize, requestPerSecond);
				} else {
					COMMUNICATOR = new CommonCommunicator(udpPort, runAsDaemon, transferBlockSize, requestPerSecond);
				}
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return COMMUNICATOR;
	}

	/**
	 * @param enableHttp
	 *            the enableHttp to set
	 */
	public void setEnableHttp(boolean enableHttp) {
		this.enableHttp = enableHttp;
	}

	/**
	 * @param httpPort
	 *            the httpPort to set
	 */
	public void setHttpPort(int httpPort) {
		if (httpPort < 0) {
			throw new IllegalArgumentException("httpPort < 0");
		}

		enableHttp = true;
		this.httpPort = httpPort;
	}

	/**
	 * @param requestPerSecond
	 *            the requestPerSecond to set
	 */
	public void setRequestPerSecond(int requestPerSecond) {
		if (requestPerSecond < 0) {
			throw new IllegalArgumentException("requestPerSecond < 0");
		}

		this.requestPerSecond = requestPerSecond;
	}

	/**
	 * @param runAsDaemon
	 *            the runAsDaemon to set
	 */
	public void setRunAsDaemon(boolean runAsDaemon) {
		this.runAsDaemon = runAsDaemon;
	}

	/**
	 * @param transferBlockSize
	 *            the transferBlockSize to set
	 */
	public void setTransferBlockSize(int transferBlockSize) {
		if (transferBlockSize < 0) {
			throw new IllegalArgumentException("transferBlockSize < 0");
		}

		this.transferBlockSize = transferBlockSize;
	}

	/**
	 * @param udpPort
	 *            the udpPort to set
	 */
	public void setUdpPort(int udpPort) {
		if (udpPort < 0) {
			throw new IllegalArgumentException("udpPort < 0");
		}

		this.udpPort = udpPort;
	}

	/**
	 * The Interface Communicator.
	 * 
	 * @author Francesco Corazza
	 */
	public static interface Communicator extends Layer {
		int getPort();

		int getPort(boolean isHttpPort);
	}

	/**
	 * The Class CommonCommunicator.
	 * 
	 * @author Francesco Corazza
	 */
	private static class CommonCommunicator extends UpperLayer implements
			Communicator {

		private int udpPort;

		/**
		 * Instantiates a new common communicator.
		 * 
		 * @param udpPort
		 *            the udp port
		 * @param runAsDaemon
		 *            the run as daemon
		 * @param transferBlockSize
		 *            the transfer block size
		 * @param requestPerSecond
		 *            the request per second
		 * @throws SocketException
		 *             the socket exception
		 */
		public CommonCommunicator(int udpPort, boolean runAsDaemon, int transferBlockSize, int requestPerSecond) throws SocketException {
			this.udpPort = udpPort;

			CoapStack coapStack = new CoapStack(udpPort, runAsDaemon, transferBlockSize, requestPerSecond);
			setLowerLayer(coapStack);
		}

		@Override
		public int getPort() {
			return udpPort;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator#
		 * getPort(boolean)
		 */
		@Override
		public int getPort(boolean isHttpPort) {
			return isHttpPort ? -1 : getPort();
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * ch.ethz.inf.vs.californium.layers.UpperLayer#doReceiveMessage(ch.
		 * ethz.inf.vs.californium.coap.Message)
		 */
		@Override
		protected void doReceiveMessage(Message msg) {

			if (msg instanceof Response) {
				Response response = (Response) msg;

				// initiate custom response handling
				if (response.getRequest() != null) {
					response.getRequest().handleResponse(response);
				}
			}

			// pass message to registered receivers
			deliverMessage(msg);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * ch.ethz.inf.vs.californium.layers.UpperLayer#doSendMessage(ch.ethz
		 * .inf.vs.californium.coap.Message)
		 */
		@Override
		protected void doSendMessage(Message msg) throws IOException {

			// defensive programming before entering the stack, lower layers
			// should
			// assume a correct message.
			if (msg != null) {

				// check message before sending through the stack
				if (msg.getPeerAddress().getAddress() == null) {
					throw new IOException("Remote address not specified");
				}

				// delegate to first layer
				sendMessageOverLowerLayer(msg);
			}
		}
	}

	/**
	 * The Class CommunicatorFactoryHolder.
	 * 
	 * @author Francesco Corazza
	 */
	private static class CommunicatorFactoryHolder {
		public static CommunicatorFactory communicatorFactory = new CommunicatorFactory();
	}

	/**
	 * The Class ProxyCommunicator.
	 * 
	 * @author Francesco Corazza
	 */
	private static class ProxyCommunicator extends UpperLayer implements
			Communicator {

		private int udpPort;
		private int httpPort;
		private CoapStack coapStack;
		private HttpStackAsync httpStack;

		/**
		 * Instantiates a new proxy communicator.
		 * 
		 * @param udpPort
		 *            the udp port
		 * @param httpPort
		 *            the http port
		 * @param runAsDaemon
		 *            the run as daemon
		 * @param transferBlockSize
		 *            the transfer block size
		 * @param requestPerSecond
		 *            the request per second
		 * @throws IOException
		 */
		public ProxyCommunicator(int udpPort, int httpPort, boolean runAsDaemon, int transferBlockSize, int requestPerSecond) throws IOException {
			this.udpPort = udpPort;
			this.httpPort = httpPort;

			coapStack = new CoapStack(udpPort, runAsDaemon, transferBlockSize, requestPerSecond);
			httpStack = new HttpStackAsync(httpPort);

			coapStack.registerReceiver(this);
			httpStack.registerReceiver(this);
		}

		@Override
		public int getPort() {
			return udpPort;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator#
		 * getPort(boolean)
		 */
		@Override
		public int getPort(boolean isHttpPort) {
			return isHttpPort ? httpPort : getPort();
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * ch.ethz.inf.vs.californium.layers.UpperLayer#doReceiveMessage(ch.
		 * ethz.inf.vs.californium.coap.Message)
		 */
		@Override
		protected void doReceiveMessage(Message message) {

			if (message instanceof Response) {
				Response response = (Response) message;

				// initiate custom response handling
				if (response.getRequest() != null) {
					response.getRequest().handleResponse(response);
				}
			}

			// pass message to registered receivers
			deliverMessage(message);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * ch.ethz.inf.vs.californium.layers.UpperLayer#doSendMessage(ch.ethz
		 * .inf.vs.californium.coap.Message)
		 */
		@Override
		protected void doSendMessage(Message message) throws IOException {
			// defensive programming before entering the stack, lower layers
			// should assume a correct message.
			if (message != null) {

				// check message before sending through the stack
				if (message.getPeerAddress().getAddress() == null) {
					throw new IOException("Remote address not specified");
				}

				// the ProxyCommunicator can't use the API
				// sendMessageOverLowerLayer because it has two lower layers
				// (i.e., the stacks)
				if (httpStack.isWaiting(message)) {
					httpStack.sendMessage(message);
				} else {
					coapStack.sendMessage(message);
				}
			}
		}
	}
}

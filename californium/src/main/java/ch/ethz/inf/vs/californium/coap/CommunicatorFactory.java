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
import ch.ethz.inf.vs.californium.layers.HttpStack;
import ch.ethz.inf.vs.californium.layers.Layer;
import ch.ethz.inf.vs.californium.layers.UpperLayer;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * A factory for creating Communicator objects.
 * 
 * @author Francesco Corazza
 */
public final class CommunicatorFactory {

	protected static final Logger LOG = Logger.getLogger(CommunicatorFactory.class.getName());

	private int httpPort = 0;

	private int udpPort = 0;
	private boolean runAsDaemon = true;
	private int transferBlockSize = 0;
	private int requestPerSecond = 0;
	private boolean enableHttp = false;
	/**
	 * Determines whether a secured communicator (DTLS enabled) is used. For the
	 * server (DTLS bound to specified port) determined in the properties file,
	 * for clients determined by the URI scheme (coap vs coaps).
	 */
	private boolean secureCommunication = Properties.std.getBool("ENABLE_DTLS");
	/** The unsecured communicator (singleton). */
	private static Communicator COMMUNICATOR = null;
	/**
	 * The secured (DTLS enabled) communicator. Two instances are needed,
	 * because a client can issue requests to servers with DTLS enabled or
	 * disabled at the same time.
	 */
	private static Communicator SECURED_COMMUNICATOR = null;

	/**
	 * Gets the single instance of CommunicatorFactory.
	 * 
	 * @return single instance of CommunicatorFactory
	 */
	public static CommunicatorFactory getInstance() {
		return CommunicatorFactoryHolder.communicatorFactory;
	}
	
	public Communicator getCommunicator() {
		if (secureCommunication) {
			if (SECURED_COMMUNICATOR == null) {
				try {
					if (enableHttp) {
						SECURED_COMMUNICATOR = new ProxyCommunicator(udpPort, httpPort, runAsDaemon, transferBlockSize, requestPerSecond, secureCommunication);
					} else {
						SECURED_COMMUNICATOR = new CommonCommunicator(udpPort, runAsDaemon, transferBlockSize, requestPerSecond, secureCommunication);
					}
				} catch (SocketException e) {
					LOG.severe("Cannot create the communicator, exiting");
					System.exit(-1);
				} catch (IOException e) {
					LOG.severe("Cannot create the communicator, exiting");
					System.exit(-1);
				}
			}

			return SECURED_COMMUNICATOR;
			
		} else {
			if (COMMUNICATOR == null) {
				try {
					if (enableHttp) {
						COMMUNICATOR = new ProxyCommunicator(udpPort, httpPort, runAsDaemon, transferBlockSize, requestPerSecond, secureCommunication);
					} else {
						COMMUNICATOR = new CommonCommunicator(udpPort, runAsDaemon, transferBlockSize, requestPerSecond, secureCommunication);
					}
				} catch (SocketException e) {
					LOG.severe("Cannot create the communicator, exiting");
					System.exit(-1);
				} catch (IOException e) {
					LOG.severe("Cannot create the communicator, exiting");
					System.exit(-1);
				}
			}

			return COMMUNICATOR;
		}
	}
	
	/**
	 * This method returns a secured or unsecured communication stack (DTLS
	 * disabled). By calling this function, the value read from the properties
	 * file (ENABLE_DTLS) is overwritten and should therefore only be called
	 * from the client side as the server should not change is behaviour as the
	 * use of DTLS is bound to a given port number.
	 * 
	 * @param secureCommunication
	 *            indicates whether DTLS should be enabled or not.
	 * @return secured or unsecured communication stack.
	 */
	public Communicator getCommunicator(boolean secureCommunication) {
		this.secureCommunication = secureCommunication;
		return getCommunicator();
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

		/**
		 * Gets the port.
		 * 
		 * @param isHttpPort
		 *            the is http port
		 * @return the port
		 */
		int getPort(boolean isHttpPort);
	}

	/**
	 * The Class CommonCommunicator.
	 * 
	 * @author Francesco Corazza
	 */
	private static class CommonCommunicator extends UpperLayer implements
			Communicator {

		private final int udpPort;

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
		public CommonCommunicator(int udpPort, boolean runAsDaemon, int transferBlockSize, int requestPerSecond, boolean isSecured) throws SocketException {
			this.udpPort = udpPort;

			CoapStack coapStack = new CoapStack(udpPort, runAsDaemon, transferBlockSize, requestPerSecond, isSecured);
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

		private final int udpPort;
		private final int httpPort;
		private final CoapStack coapStack;
		private final HttpStack httpStack;

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
		 *             Signals that an I/O exception has occurred.
		 */
		public ProxyCommunicator(int udpPort, int httpPort, boolean runAsDaemon, int transferBlockSize, int requestPerSecond, boolean isSecured) throws IOException {
			this.udpPort = udpPort;
			this.httpPort = httpPort;

			coapStack = new CoapStack(udpPort, runAsDaemon, transferBlockSize, requestPerSecond, isSecured);
			httpStack = new HttpStack(httpPort);

			coapStack.registerReceiver(this);
			httpStack.registerReceiver(this);

			LOG.info("ProxyCommunicator initialized");
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
				// (i.e., CaapStack and HttpStack)

				if (message instanceof Response) {
					Request request = ((Response) message).getRequest();

					if (httpStack.isWaitingRequest(request)) {
						if (message.isEmptyACK()) {
							// if the message is not the actual response, but
							// only an acknowledge, should not be forwarded
							// (HTTP is on TCP so there is no need for acks in
							// the application layer)
							return;
						}

						LOG.info("Incoming response, sending to http stack");

						httpStack.sendMessage(message);
						return;
					}
				}

				LOG.info("Incoming message, sending to coap stack");
				coapStack.sendMessage(message);
			}
		}
	}
}

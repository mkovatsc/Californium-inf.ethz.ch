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

import java.io.IOException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.util.CoapTranslator;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class represent the container of the resources and the layers used by the
 * proxy.
 * 
 * @author Francesco Corazza
 * 
 */
public class ProxyEndpoint extends Endpoint {

	private static final int THREAD_NUMBER = 10;

	private ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_NUMBER);
	private LocalEndpoint localEndpoint;
	private final CoapClient coapClient = new CoapClient();
	private final CoapCache coapCache = new CoapCache();

	// the proxy resource is used for statistic measures, and then it should be
	// a private field
	private final ProxyResource proxyResource = new ProxyResource();

	/**
	 * Instantiates a new proxy endpoint from the default ports.
	 * 
	 * @throws SocketException
	 *             the socket exception
	 */
	public ProxyEndpoint() throws SocketException {
		this(Properties.std.getInt("DEFAULT_PORT"), Properties.std.getInt("HTTP_PORT"));
	}

	/**
	 * Instantiates a new proxy endpoint.
	 * 
	 * @param udpPort
	 *            the udp port
	 * @param httpPort
	 *            the http port
	 * @throws SocketException
	 *             the socket exception
	 */
	public ProxyEndpoint(int udpPort, int httpPort) throws SocketException {
		this(udpPort, httpPort, 0, false, 0);
	}

	/**
	 * Instantiates a new proxy endpoint.
	 * 
	 * @param udpPort
	 *            the udp port
	 * @param httpPort
	 *            the http port
	 * @param defaultBlockSze
	 *            the default block sze
	 * @param daemon
	 *            the daemon
	 * @param requestPerSecond
	 *            the request per second
	 * @throws SocketException
	 *             the socket exception
	 */
	public ProxyEndpoint(int udpPort, int httpPort, int defaultBlockSze, boolean daemon, int requestPerSecond) throws SocketException {
		// get the communicator factory
		CommunicatorFactory factory = CommunicatorFactory.getInstance();

		// set the parameters of the communicator
		factory.setEnableHttp(true);
		factory.setHttpPort(httpPort);
		factory.setUdpPort(udpPort);
		factory.setTransferBlockSize(defaultBlockSze);
		factory.setRunAsDaemon(daemon);
		factory.setRequestPerSecond(requestPerSecond);

		// initialize communicator
		Communicator communicator = factory.getCommunicator();

		// register the endpoint as a receiver
		communicator.registerReceiver(this);

		// create the localEndpoint to contain the local resources
		// since the communicator is already initialized, the settings made by
		// the localEndpoint have no effect
		localEndpoint = new LocalEndpoint();

		// unregister the localEndpoint because the messages have to pass
		// through the proxyEnpoint
		communicator.unregisterReceiver(localEndpoint);

		// add the resource directory resource
		localEndpoint.addResource(new RDResource());
		localEndpoint.addResource(proxyResource);
	}

	/**
	 * The method checks if the proxy-uri is set and forwards the request to the
	 * default stack if not set and to the proxy stack if set.
	 * 
	 * @param request
	 *            the original request
	 */
	@Override
	public void execute(Request request) {
		// a new thread is in charge of repling to the request
		threadPool.submit(new ProxyRequestHandler(request));
	}

	/**
	 * Gets the port.
	 * 
	 * @param isHttpPort
	 *            the is http port
	 * @return the port
	 */
	public int getPort(boolean isHttpPort) {
		return CommunicatorFactory.getInstance().getCommunicator().getPort(isHttpPort);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ch.ethz.inf.vs.californium.coap.MessageHandler#handleRequest(ch.ethz.
	 * inf.vs.californium.coap.Request)
	 */
	@Override
	public void handleRequest(Request request) {
		execute(request);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ch.ethz.inf.vs.californium.coap.MessageHandler#handleResponse(ch.ethz
	 * .inf.vs.californium.coap.Response)
	 */
	@Override
	public void handleResponse(Response response) {
		// empty?
	}

	/**
	 * The Class CoapCache.
	 * 
	 * @author Francesco Corazza
	 */
	private class CoapCache {

		/**
		 * Checks if is cached.
		 * 
		 * @param request
		 *            the request
		 * @return true, if is cached
		 */
		public boolean isCached(Request request) {
			// TODO Auto-generated method stub
			return false;
		}

		/**
		 * Send cached response.
		 * 
		 * @param request
		 *            the request
		 */
		public void sendCachedResponse(Request request) {
			// TODO Auto-generated method stub
		}

	}

	/**
	 * Utility class to encapsulate the synchronous coap client used to retrieve
	 * the requested coap resource.
	 * 
	 * @author Francesco Corazza
	 */
	private class CoapClient {

		/**
		 * Forward.
		 * 
		 * @param incomingRequest
		 *            the incoming request
		 */
		public void forward(Request incomingRequest) {

			// create the new request to forward to the requested coap server
			Request outgoingRequest = null;
			try {
				// create the new request from the original
				outgoingRequest = incomingRequest.getClass().newInstance();

				// fill the new request to forward
				CoapTranslator.fillRequest(incomingRequest, outgoingRequest);

				// update the statistics
				proxyResource.updateStatistics(outgoingRequest);

				// enable response queue for blocking I/O
				outgoingRequest.enableResponseQueue(true);

				// execute the request
				outgoingRequest.execute();
			} catch (URISyntaxException e) {
				LOG.warning("Proxy-uri option malformed: " + e.getMessage());
				incomingRequest.respondAndSend(Integer.parseInt(CoapTranslator.TRANSLATION_PROPERTIES.getProperty("coap.request.uri.malformed")));
			} catch (IOException e) {
				LOG.warning("Failed to execute request: " + e.getMessage());
				incomingRequest.respondAndSend(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (InstantiationException e) {
				LOG.warning("Failed to create a new request: " + e.getMessage());
				incomingRequest.respondAndSend(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (IllegalAccessException e) {
				LOG.warning("Failed to create a new request: " + e.getMessage());
				incomingRequest.respondAndSend(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			}

			try {
				// receive the response
				Response receivedResponse = outgoingRequest.receiveResponse();

				if (receivedResponse != null) {
					// create the new response
					Response outgoingResponse = receivedResponse.getClass().newInstance();

					// create the real response for the original request
					CoapTranslator.fillResponse(receivedResponse, outgoingResponse);

					// complete the request with the received response and set
					// the parameters of the response according to the request
					incomingRequest.respondAndSend(outgoingResponse);
				} else {
					LOG.warning("No response received.");
					incomingRequest.respondAndSend(Integer.parseInt(CoapTranslator.TRANSLATION_PROPERTIES.getProperty("coap.request.timeout")));
				}

			} catch (InstantiationException e) {
				LOG.warning("Failed to create a new response: " + e.getMessage());
				incomingRequest.respondAndSend(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (IllegalAccessException e) {
				LOG.warning("Failed to create a new response: " + e.getMessage());
				incomingRequest.respondAndSend(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (InterruptedException e) {
				LOG.warning("Receiving of response interrupted: " + e.getMessage());
				incomingRequest.respondAndSend(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			}
		}
	}

	/**
	 * Class used to send a new proxying task to the thread pool.
	 * 
	 * @author Francesco Corazza
	 */
	private class ProxyRequestHandler implements Runnable {
		private Request request;

		/**
		 * Instantiates a new proxy request handler.
		 * 
		 * @param request
		 *            the request
		 */
		public ProxyRequestHandler(Request request) {
			if (request == null) {
				throw new IllegalArgumentException("request == null");
			}

			this.request = request;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			// check if the cache has a saved version of the request
			if (coapCache.isCached(request)) {
				coapCache.sendCachedResponse(request);
			}

			// check for the proxy-uri option
			if (request.isProxyUriSet()) {
				// forward the to the requested coap server
				coapClient.forward(request);
			} else {
				// forward to localEndpoint for the local resources
				localEndpoint.execute(request);
			}
		}
	}
}

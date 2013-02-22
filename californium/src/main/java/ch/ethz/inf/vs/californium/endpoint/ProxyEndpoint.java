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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.CommunicatorFactory;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator;
import ch.ethz.inf.vs.californium.coap.Message.messageType;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyCacheResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyCoapClientResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyHttpClientResource;
import ch.ethz.inf.vs.californium.endpoint.resources.RDResource;
import ch.ethz.inf.vs.californium.endpoint.resources.StatsResource;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class represent the container of the resources and the layers used by the
 * proxy.
 * 
 * @author Francesco Corazza
 * 
 */
public class ProxyEndpoint extends LocalEndpoint {

	private static final String PROXY_COAP_CLIENT = "proxy/coapClient";
	private static final String PROXY_HTTP_CLIENT = "proxy/httpClient";
	private int httpPort = 0;
	private int udpPort = 0;
	private boolean runAsDaemon = false;
	private int transferBlockSize = 0;
	private int requestPerSecond = 0;

	private final ProxyCacheResource cacheResource = new ProxyCacheResource();
	private final StatsResource statsResource = new StatsResource(cacheResource);

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
	 * @param transferBlockSize
	 *            the transfer block size
	 * @param runAsDaemon
	 *            the run as daemon
	 * @param requestPerSecond
	 *            the request per second
	 * @throws SocketException
	 *             the socket exception
	 */
	public ProxyEndpoint(int udpPort, int httpPort, int transferBlockSize, boolean runAsDaemon, int requestPerSecond) throws SocketException {
		super();

		this.udpPort = udpPort;
		this.httpPort = httpPort;
		this.transferBlockSize = transferBlockSize;
		this.runAsDaemon = runAsDaemon;
		this.requestPerSecond = requestPerSecond;

		// add Resource Directory resource
		addResource(new RDResource());
		// add the cache resource
		addResource(cacheResource);
		// add the resource for statistics
		addResource(statsResource);
		// add the coap client
		addResource(new ProxyCoapClientResource());
		// add the http client
		addResource(new ProxyHttpClientResource());
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
	 * ch.ethz.inf.vs.californium.endpoint.LocalEndpoint#handleRequest(ch.ethz
	 * .inf.vs.californium.coap.Request)
	 */
	@Override
	public void handleRequest(Request request) {
		Response response = null;

		// ignore the request if it is reset or acknowledge
		// check if the proxy-uri is defined
		if (request.getType() != messageType.RST && request.getType() != messageType.ACK && request.isProxyUriSet()) {
			// get the response from the cache
			response = cacheResource.getResponse(request);

			// update statistics
			statsResource.updateStatistics(request, response != null);
		}

		// check if the response is present in the cache
		if (response != null) {
			// link the retrieved response with the request to set the
			// parameters request-specific (i.e., token, id, etc)
			request.respondAndSend(response);
			return;
		} else {

			// edit the request to be correctly forwarded if the proxy-uri is
			// set
			if (request.isProxyUriSet()) {
				try {
					manageProxyUriRequest(request);

				} catch (URISyntaxException e) {
					LOG.warning(String.format("Proxy-uri malformed: %s", request.getFirstOption(OptionNumberRegistry.PROXY_URI)));

					request.respond(CodeRegistry.RESP_BAD_OPTION);
					request.sendResponse();
				}
			}

			// handle the request as usual
			execute(request);
		}
	}

	/**
	 * Manage proxy uri request.
	 * 
	 * @param request
	 *            the request
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 */
	private void manageProxyUriRequest(Request request) throws URISyntaxException {
		// check which schema is requested
		URI proxyUri = request.getProxyUri();

		// the local resource that will abstract the client part of the
		// proxy
		String clientPath;

		// switch between the schema requested
		if (proxyUri.getScheme() != null && proxyUri.getScheme().matches("^http.*")) {
			// the local resource related to the http client
			clientPath = PROXY_HTTP_CLIENT;
		} else {
			// the local resource related to the http client
			clientPath = PROXY_COAP_CLIENT;
		}

		// set the path in the request to be forwarded correctly
		List<Option> uriPath = Option.split(OptionNumberRegistry.URI_PATH, clientPath, "/");
		request.setOptions(uriPath);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ch.ethz.inf.vs.californium.endpoint.LocalEndpoint#createCommunicator()
	 */
	@Override
	protected void createCommunicator() {
		// get the communicator factory
		CommunicatorFactory factory = CommunicatorFactory.getInstance();

		// set the parameters of the communicator
		factory.setEnableHttp(true);
		factory.setHttpPort(httpPort);
		factory.setUdpPort(udpPort);
		factory.setTransferBlockSize(transferBlockSize);
		factory.setRunAsDaemon(runAsDaemon);
		factory.setRequestPerSecond(requestPerSecond);

		// initialize communicator
		Communicator communicator = factory.getCommunicator();

		// register the endpoint as a receiver of the communicator
		communicator.registerReceiver(this);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ch.ethz.inf.vs.californium.endpoint.LocalEndpoint#responseProduced(ch
	 * .ethz.inf.vs.californium.coap.Response)
	 */
	@Override
	protected void responseProduced(Response response) {
		// check if the proxy-uri is defined
		if (response.getRequest().isProxyUriSet()) {
			// insert the response in the cache
			cacheResource.cacheResponse(response);
		}
	}
}

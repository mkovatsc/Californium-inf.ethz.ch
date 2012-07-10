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
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.CacheResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyCoapClientResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyHttpClientResource;
import ch.ethz.inf.vs.californium.endpoint.resources.ProxyStatsResource;
import ch.ethz.inf.vs.californium.endpoint.resources.RDResource;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class represent the container of the resources and the layers used by the
 * proxy.
 * 
 * @author Francesco Corazza
 * 
 */
public class ProxyEndpoint extends LocalEndpoint {

	private int httpPort = 0;
	private int udpPort = 0;
	private boolean runAsDaemon = false;
	private int transferBlockSize = 0;
	private int requestPerSecond = 0;

	// the proxy resource is used for statistic measures, and then it should be
	// a private field
	// TODO create hooks
	private final ProxyStatsResource proxyStatResource = new ProxyStatsResource();

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
		// add Resource Directory resource
		addResource(new CacheResource());
		// add the resource for statistics
		addResource(proxyStatResource);
		// add the http client
		addResource(new ProxyHttpClientResource());
		// add the coap client
		addResource(new ProxyCoapClientResource());
	}

	@Override
	public void execute(Request request) {

		// TODO check if the cache has a saved version of the request

		// check for the proxy-uri option
		if (request.isProxyUriSet()) {
			// check which schema is requested
			URI proxyUri = null;
			try {
				proxyUri = request.getProxyUri();
			} catch (URISyntaxException e) {
				// resource does not exist
				LOG.info(String.format("Proxy-uri malformed: %s", request.getFirstOption(OptionNumberRegistry.PROXY_URI)));

				request.respond(CodeRegistry.RESP_BAD_OPTION);
				request.sendResponse();
				return;
			}

			// the local resource that will abstract the client part of the
			// proxyo
			String clientPath;

			// switch between the schema requested
			if (proxyUri.getScheme().matches("^http.*")) {
				// the local resource related to the http client
				clientPath = "proxy/httpClient";
			} else {
				// the local resource related to the http client
				clientPath = "proxy/coapClient";
			}

			// set the path in the request to be forwarded correctly
			List<Option> uriPath = Option.split(OptionNumberRegistry.URI_PATH, clientPath, "/");
			request.setOptions(uriPath);
		}

		// handle by local endpoint
		super.execute(request);
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

		// register the endpoint as a receiver
		communicator.registerReceiver(this);
	}
}

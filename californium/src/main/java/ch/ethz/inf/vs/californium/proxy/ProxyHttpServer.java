package ch.ethz.inf.vs.californium.proxy;

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


import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.Server;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Exchange.Origin;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.resources.ResourceBase;

/**
 * The class represent the container of the resources and the layers used by the
 * proxy.
 * 
 * @author Francesco Corazza
 * 
 */
public class ProxyHttpServer {

	private final static Logger LOG = CalifonriumLogger.getLogger(ProxyHttpServer.class);
	
	private static final String PROXY_COAP_CLIENT = "proxy/coapClient";
	private static final String PROXY_HTTP_CLIENT = "proxy/httpClient";

	private final ProxyCacheResource cacheResource = new ProxyCacheResource(true);
	private final StatsResource statsResource = new StatsResource(cacheResource);
	
	private ProxyCoAPResolver proxyCoapResolver;

	/**
	 * Instantiates a new proxy endpoint from the default ports.
	 * 
	 * @throws SocketException
	 *             the socket exception
	 */
	public ProxyHttpServer(Server server) throws IOException {
		this(NetworkConfig.getStandard().getInt(NetworkConfigDefaults.HTTP_PORT));
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
	// Trying: http://localhost:8080/proxy/coap://localhost:5683/huhu
	public ProxyHttpServer(int httpPort) throws IOException {
		
		// This code was important, when ProxyHttpServer was a CoAPServer itself
		// TODO: remove this now
//		super("proxy");
//		this.httpPort = httpPort;
//		// add Resource Directory resource
//		add(new RDResource());
//		// add the cache resource
//		add(cacheResource);
//		// add the resource for statistics
//		add(statsResource);
//		// add the coap client
//		add(new ProxyCoapClientResource());
//		// add the http client
//		add(new ProxyHttpClientResource());
	
		// TODO: make better
		new HttpStack(httpPort) {
			@Override
			public void doReceiveMessage(final Request request) {
				Exchange exchange = new Exchange(request, Origin.REMOTE) {
					@Override
					public void respond(Response response) {
						LOG.info("Back in ProxyEndpoint, response: "+response);
						try {
							request.setResponse(response);
							responseProduced(request, response);
							/*httpStack.*/doSendResponse(request, response);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				
				exchange.setRequest(request);
				handleRequest(exchange);
			}
		};
	}

	public void handleRequest(Exchange exchange) {
		LOG.info("ProxyEndpoint handles request "+exchange.getRequest());
		Request request = exchange.getRequest();
		Response response = null;

		// ignore the request if it is reset or acknowledge
		// check if the proxy-uri is defined
		if (request.getType() != Type.RST && request.getType() != Type.ACK 
				&& request.getOptions().hasProxyURI()) {
			// get the response from the cache
			response = cacheResource.getResponse(request);
			LOG.info("Cache returned "+response);

			// update statistics
			statsResource.updateStatistics(request, response != null);
		}

		// check if the response is present in the cache
		if (response != null) {
			// link the retrieved response with the request to set the
			// parameters request-specific (i.e., token, id, etc)
			exchange.respond(response);
			return;
		} else {

			// edit the request to be correctly forwarded if the proxy-uri is
			// set
			if (request.getOptions().hasProxyURI()) {
				try {
					manageProxyUriRequest(request);
					LOG.info("after manageProxyUriRequest: "+request);

				} catch (URISyntaxException e) {
					LOG.warning(String.format("Proxy-uri malformed: %s", request.getOptions().getProxyURI()));

					exchange.respond(ResponseCode.BAD_OPTION);
				}
			}

			// handle the request as usual
			proxyCoapResolver.forwardRequest(exchange);
			/*
			 * Martin:
			 * Originally, the request was delivered to the ProxyCoAP2Coap which was at the path
			 * proxy/coapClient or to proxy/httpClient
			 * This approach replaces this implicit fuzzy connection with an explicit
			 * and dynamically changeable one.
			 */
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
		URI proxyUri = new URI(request.getOptions().getProxyURI());

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
		
		LOG.info("Chose "+clientPath+" as clientPath");

		// set the path in the request to be forwarded correctly
		request.getOptions().setURIPath(clientPath);
		
	}

	protected void responseProduced(Request request, Response response) {
		// check if the proxy-uri is defined
		if (request.getOptions().hasProxyURI()) {
			LOG.info("Cache response");
			// insert the response in the cache
			cacheResource.cacheResponse(request, response);
		} else {
			LOG.info("Do not cache response");
		}
	}

	public ProxyCoAPResolver getProxyCoapResolver() {
		return proxyCoapResolver;
	}

	public void setProxyCoapResolver(ProxyCoAPResolver proxyCoapResolver) {
		this.proxyCoapResolver = proxyCoapResolver;
	}
}

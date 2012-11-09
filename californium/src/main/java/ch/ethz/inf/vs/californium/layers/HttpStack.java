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

package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerRegistry;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.util.HttpTranslator;
import ch.ethz.inf.vs.californium.util.InvalidFieldException;
import ch.ethz.inf.vs.californium.util.InvalidMethodException;
import ch.ethz.inf.vs.californium.util.Properties;
import ch.ethz.inf.vs.californium.util.TranslationException;

/**
 * Class encapsulating the logic of a http server. The class create a receiver
 * thread that it is always blocked on the listen primitive. For each connection
 * this thread creates a new thread that handles the client/server dialog.
 * 
 * @author Francesco Corazza
 */
public class HttpStack extends UpperLayer {
	private static final int SOCKET_TIMEOUT = Properties.std.getInt("HTTP_SERVER_SOCKET_TIMEOUT");
	private static final int GATEWAY_TIMEOUT = SOCKET_TIMEOUT * 3 / 4;
	private static final String SERVER_NAME = "Californium Http Proxy";
	private static final int SOCKET_BUFFER_SIZE = Properties.std.getInt("HTTP_SERVER_SOCKET_BUFFER_SIZE");

	/**
	 * Resource associated with the proxying behavior. If a client requests
	 * resource indicated by
	 * http://proxy-address/PROXY_RESOURCE_NAME/coap-server, the proxying
	 * handler will forward the request desired coap server.
	 */
	private static final String PROXY_RESOURCE_NAME = "proxy";

	/**
	 * The resource associated with the local resources behavior. If a client
	 * requests resource indicated by
	 * http://proxy-address/LOCAL_RESOURCE_NAME/coap-resource, the proxying
	 * handler will forward the request to the local resource requested.
	 */
	public static final String LOCAL_RESOURCE_NAME = "local";

	private final ConcurrentHashMap<Request, Exchanger<Response>> exchangeMap = new ConcurrentHashMap<Request, Exchanger<Response>>();

	/**
	 * Instantiates a new http stack on the requested port. It creates an http
	 * listener thread on the port.
	 * 
	 * @param httpPort
	 *            the http port
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public HttpStack(int httpPort) throws IOException {
		new HttpServer(httpPort);
	}

	/**
	 * Checks if a thread is waiting for the arrive of a specific response.
	 * 
	 * @param request
	 *            the request
	 * @return true, if is waiting
	 */
	public boolean isWaitingRequest(Request request) {

		// DEBUG
		// System.out.println(request.hashCode());
		// request.prettyPrint();
		//
		// System.out.println(responseMap.get(request) != null);
		// System.out.println(semaphoreMap.get(request) != null);
		//
		// for (Request r : responseMap.keySet()) {
		// System.out.println(r.hashCode());
		// r.prettyPrint();
		// }
		//
		// for (Request r : semaphoreMap.keySet()) {
		// System.out.println(r.hashCode());
		// r.prettyPrint();
		// }

		// check the presence of the key in both maps
		// TODO check how much is this operation heavy
		// return responseMap.containsKey(request) &&
		// semaphoreMap.containsKey(request);

		return exchangeMap.containsKey(request);
	}

	/**
	 * Send simple http response.
	 * 
	 * @param httpExchange
	 *            the http exchange
	 * @param httpCode
	 *            the http code
	 */
	private void sendSimpleHttpResponse(HttpAsyncExchange httpExchange, int httpCode) {
		// get the empty response from the exchange
		HttpResponse httpResponse = httpExchange.getResponse();

		// create and set the status line
		StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, httpCode, EnglishReasonPhraseCatalog.INSTANCE.getReason(httpCode, Locale.ENGLISH));
		httpResponse.setStatusLine(statusLine);

		// send the error response
		httpExchange.submitResponse();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ch.ethz.inf.vs.californium.layers.UpperLayer#doSendMessage(ch.ethz.inf
	 * .vs.californium.coap.Message)
	 */
	@Override
	protected void doSendMessage(Message message) throws IOException {
		// the http stack is intended to send back only coap responses

		// check if the message is a response
		if (message instanceof Response) {
			// retrieve the request linked to the response
			Response response = (Response) message;
			Request request = response.getRequest();
			LOG.info("Handling response for request: " + request);

			// fill the exchanger with the incoming response
			Exchanger<Response> exchanger = exchangeMap.get(request);
			try {
				exchanger.exchange(response);
			} catch (InterruptedException e) {
				LOG.warning("Exchange interrupted: " + e.getMessage());

				// remove the entry from the map
				exchangeMap.remove(request);
				return;
			}

			LOG.info("Exchanged correctly");
		}
	}

	/**
	 * The Class CoapResponseWorker. This thread waits a response from the lower
	 * layers. It is the consumer of the producer/consumer pattern.
	 * 
	 * @author Francesco Corazza
	 */
	private final class CoapResponseWorker extends Thread {
		private final HttpAsyncExchange httpExchange;
		private final HttpRequest httpRequest;
		private final Request coapRequest;

		/**
		 * Instantiates a new coap response worker.
		 * 
		 * @param name
		 *            the name
		 * @param coapRequest
		 *            the coap request
		 * @param httpExchange
		 *            the http exchange
		 * @param httpRequest
		 *            the http request
		 */
		public CoapResponseWorker(String name, Request coapRequest, HttpAsyncExchange httpExchange, HttpRequest httpRequest) {
			super(name);
			this.coapRequest = coapRequest;
			this.httpExchange = httpExchange;
			this.httpRequest = httpRequest;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			// get the exchanger
			Exchanger<Response> exchanger = exchangeMap.get(coapRequest);

			// if the map does not contain the key, send an error response
			if (exchanger == null) {
				LOG.warning("exchanger == null");
				sendSimpleHttpResponse(httpExchange, HttpStatus.SC_INTERNAL_SERVER_ERROR);
				return;
			}

			// get the response
			Response coapResponse = null;
			try {
				coapResponse = exchanger.exchange(Response.NULL, GATEWAY_TIMEOUT, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				LOG.warning("Timeout occurred");
				// send the timeout error message
				sendSimpleHttpResponse(httpExchange, HttpTranslator.STATUS_TIMEOUT);
				return;
			} catch (InterruptedException e) {
				// if the thread is interrupted, terminate
				if (isInterrupted()) {
					LOG.warning("Thread interrupted");
					sendSimpleHttpResponse(httpExchange, HttpStatus.SC_INTERNAL_SERVER_ERROR);
					return;
				}
			} finally {
				// remove the entry from the map
				exchangeMap.remove(coapRequest);
				LOG.finer("Entry removed from map");
			}

			if (coapResponse == null) {
				LOG.warning("No coap response");
				sendSimpleHttpResponse(httpExchange, HttpTranslator.STATUS_NOT_FOUND);
				return;
			}

			// get the sample http response
			HttpResponse httpResponse = httpExchange.getResponse();

			try {
				// translate the coap response in an http response
				HttpTranslator.getHttpResponse(httpRequest, coapResponse, httpResponse);

				LOG.finer("Outgoing http response: " + httpResponse.getStatusLine());
			} catch (TranslationException e) {
				LOG.warning("Failed to translate coap response to http response: " + e.getMessage());
				sendSimpleHttpResponse(httpExchange, HttpTranslator.STATUS_TRANSLATION_ERROR);
				return;
			}

			// send the response
			httpExchange.submitResponse();
		}
	}

	private class HttpServer {

		public HttpServer(int httpPort) {
			// HTTP parameters for the server
			HttpParams params = new SyncBasicHttpParams();
			params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT).setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, SOCKET_BUFFER_SIZE).setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true).setParameter(CoreProtocolPNames.ORIGIN_SERVER, SERVER_NAME);

			// Create HTTP protocol processing chain
			// Use standard server-side protocol interceptors
			HttpRequestInterceptor[] requestInterceptors = new HttpRequestInterceptor[] { new RequestAcceptEncoding() };
			HttpResponseInterceptor[] responseInterceptors = new HttpResponseInterceptor[] { new ResponseContentEncoding(), new ResponseDate(), new ResponseServer(), new ResponseContent(), new ResponseConnControl() };
			HttpProcessor httpProcessor = new ImmutableHttpProcessor(requestInterceptors, responseInterceptors);

			// Create request handler registry
			HttpAsyncRequestHandlerRegistry registry = new HttpAsyncRequestHandlerRegistry();

			// register the handler that will reply to the proxy requests
			registry.register("/" + PROXY_RESOURCE_NAME + "/*", new ProxyAsyncRequestHandler(PROXY_RESOURCE_NAME, true));
			// register the handler for the frontend
			registry.register("/" + LOCAL_RESOURCE_NAME + "/*", new ProxyAsyncRequestHandler(LOCAL_RESOURCE_NAME, false));
			// register the default handler for root URIs
			// wrapping a common request handler with an async request handler
			registry.register("*", new BasicAsyncRequestHandler(new BaseRequestHandler()));

			// Create server-side HTTP protocol handler
			HttpAsyncService protocolHandler = new HttpAsyncService(httpProcessor, new DefaultConnectionReuseStrategy(), registry, params);

			// Create HTTP connection factory
			NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory = new DefaultNHttpServerConnectionFactory(params);

			// Create server-side I/O event dispatch
			final IOEventDispatch ioEventDispatch = new DefaultHttpServerIODispatch(protocolHandler, connFactory);

			final ListeningIOReactor ioReactor;
			try {
				// Create server-side I/O reactor
				ioReactor = new DefaultListeningIOReactor();
				// Listen of the given port
				ioReactor.listen(new InetSocketAddress(httpPort));

				// create the listener thread
				Thread listener = new Thread("HttpStack listener") {

					@Override
					public void run() {
						// Starts the reactor and initiates the dispatch of I/O
						// event notifications to the given IOEventDispatch.
						try {
							LOG.info("Submitted http listening to thread 'HttpStack listener'");

							ioReactor.execute(ioEventDispatch);
						} catch (IOException e) {
							LOG.severe("Interrupted");
						}

						LOG.info("Shutdown HttpStack");
					}
				};

				listener.setDaemon(false);
				listener.start();
				LOG.info("HttpStack started");
			} catch (IOException e) {
				LOG.severe("I/O error: " + e.getMessage());
			}
		}

		/**
		 * The Class BaseRequestHandler handles simples requests that do not
		 * need the proxying.
		 * 
		 * @author Francesco Corazza
		 */
		private class BaseRequestHandler implements HttpRequestHandler {

			/*
			 * (non-Javadoc)
			 * @see
			 * org.apache.http.protocol.HttpRequestHandler#handle(org.apache
			 * .http .HttpRequest, org.apache.http.HttpResponse,
			 * org.apache.http.protocol.HttpContext)
			 */
			@Override
			public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
				httpResponse.setStatusCode(HttpStatus.SC_OK);
				httpResponse.setEntity(new StringEntity("Californium Proxy server"));

				LOG.finer("Root request handled");
			}
		}

		/**
		 * Class associated with the http service to translate the http requests
		 * in coap requests and to produce the http responses. Even if the class
		 * accepts a string indicating the name of the proxy resource, it is
		 * still thread-safe because the local resource is set in the
		 * constructor and then only read by the methods.
		 * 
		 * @author Francesco Corazza
		 */
		private class ProxyAsyncRequestHandler implements
				HttpAsyncRequestHandler<HttpRequest> {

			private final String localResource;
			private final boolean proxyingEnabled;

			/**
			 * Instantiates a new proxy request handler.
			 * 
			 * @param localResource
			 *            the local resource
			 * @param proxyingEnabled
			 */
			public ProxyAsyncRequestHandler(String localResource, boolean proxyingEnabled) {
				super();

				this.localResource = localResource;
				this.proxyingEnabled = proxyingEnabled;
			}

			/*
			 * (non-Javadoc)
			 * @see
			 * org.apache.http.nio.protocol.HttpAsyncRequestHandler#handle(java.
			 * lang.Object, org.apache.http.nio.protocol.HttpAsyncExchange,
			 * org.apache.http.protocol.HttpContext)
			 */
			@Override
			public void handle(HttpRequest httpRequest, HttpAsyncExchange httpExchange, HttpContext httpContext) throws HttpException, IOException {
				LOG.finer("Incoming http request: " + httpRequest.getRequestLine());

				try {
					// translate the request in a valid coap request
					Request coapRequest = HttpTranslator.getCoapRequest(httpRequest, localResource, proxyingEnabled);

					// fill the maps
					exchangeMap.put(coapRequest, new Exchanger<Response>());
					LOG.finer("Fill exchange with: " + coapRequest);

					// the new thread will wait for the completion of
					// the coap request
					Thread worker = new CoapResponseWorker("HttpStack Worker", coapRequest, httpExchange, httpRequest);

					// starting the "consumer thread" that will sleep waiting
					// for the producer
					worker.start();
					LOG.finer("Started thread 'httpStack worker' to wait the response");

					// send the coap request to the upper layers
					doReceiveMessage(coapRequest);
				} catch (InvalidMethodException e) {
					LOG.warning("Method not implemented" + e.getMessage());
					sendSimpleHttpResponse(httpExchange, HttpTranslator.STATUS_WRONG_METHOD);
					return;
				} catch (InvalidFieldException e) {
					LOG.warning("Request malformed" + e.getMessage());
					sendSimpleHttpResponse(httpExchange, HttpTranslator.STATUS_URI_MALFORMED);
					return;
				} catch (TranslationException e) {
					LOG.warning("Failed to translate the http request in a valid coap request: " + e.getMessage());
					sendSimpleHttpResponse(httpExchange, HttpTranslator.STATUS_TRANSLATION_ERROR);
					return;
				}
			}

			/*
			 * (non-Javadoc)
			 * @see
			 * org.apache.http.nio.protocol.HttpAsyncRequestHandler#processRequest
			 * (org.apache.http.HttpRequest,
			 * org.apache.http.protocol.HttpContext)
			 */
			@Override
			public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
				// Buffer request content in memory for simplicity
				return new BasicAsyncRequestConsumer();
			}
		}

	}

}

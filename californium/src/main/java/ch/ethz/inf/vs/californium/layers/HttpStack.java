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
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpServerConnection;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.util.HttpTranslator;

/**
 * Class encapsulating the logic of a web server. The class create a receiver
 * thread that it is always blocked on the listen primitive. For each connection
 * this thread creates a new thread that handles the client/server dialog.
 * 
 * @author Francesco Corazza
 * 
 */
public class HttpStack extends UpperLayer {

	/**
	 * Instantiates a new http stack on the requested port.
	 * 
	 * @param httpPort
	 *            the http port
	 * @throws IOException
	 */
	public HttpStack(int httpPort) throws IOException {
		// create the listener thread
		Thread thread = new ListenerThread(httpPort);
		thread.setDaemon(false);
		thread.start();
	}

	/**
	 * Task associated for each incoming connection to manage the
	 * request/response exchanges.
	 * 
	 * @author Francesco Corazza
	 */
	private class HttpServiceRunnable implements Runnable {

		private final HttpService httpservice;
		private final HttpServerConnection conn;

		/**
		 * Instantiates a new hTTP service runnable.
		 * 
		 * @param httpservice
		 *            the httpservice
		 * @param conn
		 *            the conn
		 */
		public HttpServiceRunnable(final HttpService httpservice, final HttpServerConnection conn) {
			this.httpservice = httpservice;
			this.conn = conn;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			LOG.info("New connection thread");

			// create the context
			HttpContext context = new BasicHttpContext(null);

			try {
				while (!Thread.interrupted() && conn.isOpen()) {
					// delegate the http service to handle the incoming requests
					httpservice.handleRequest(conn, context);
				}
			} catch (ConnectionClosedException ex) {
				LOG.warning("Client closed connection");
			} catch (IOException ex) {
				LOG.warning("I/O error: " + ex.getMessage());
			} catch (HttpException ex) {
				LOG.warning("Unrecoverable HTTP protocol violation: " + ex.getMessage());
			} finally {
				try {
					// close the open connection
					conn.shutdown();
				} catch (IOException ignore) {
				}
			}
		}
	}

	/**
	 * The Class is listening in the server socket and for each new connection
	 * creates a new thread to handle it.
	 * 
	 * @author Francesco Corazza
	 */
	private class ListenerThread extends Thread {
		private static final String SERVER_NAME = "Californium Proxy";
		private final ServerSocket serversocket;
		private final HttpParams params;
		private final HttpService httpService;

		/**
		 * Instantiates a new listener thread.
		 * 
		 * @param port
		 *            the port
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
		public ListenerThread(int port) throws IOException {
			super("HTTP RequestListener");
			// create the socket
			serversocket = new ServerSocket(port);

			// create and setup parameters
			params = new SyncBasicHttpParams();
			params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024).setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true).setParameter(CoreProtocolPNames.ORIGIN_SERVER, SERVER_NAME);

			// Set up the HTTP protocol processor
			HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] { new ResponseDate(), new ResponseServer(), new ResponseContent(), new ResponseConnControl() });

			// Set up request handlers
			HttpRequestHandlerRegistry registry = new HttpRequestHandlerRegistry();
			// indicating "*", the request handler is linked to the / (root)
			// context of the http server and the request handler will answer to
			// every query
			registry.register("*", new ProxyRequestHandler());

			// Set up the HTTP service
			httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(), new DefaultHttpResponseFactory(), registry, params);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			LOG.config("Listening on port " + serversocket.getLocalPort());
			while (!Thread.interrupted()) {
				try {
					// Set up incoming HTTP connection
					Socket insocket = serversocket.accept();
					DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
					System.out.println("Incoming connection from " + insocket.getInetAddress());
					conn.bind(insocket, params);

					// if (threadPool != null) {
					// // submit the task to the thread pool
					// threadPool.submit(new HTTPServiceRunnable(httpService,
					// conn));
					// } else {
					// }

					// Start worker thread to take in charge the incoming
					// request
					Thread serviceThread = new Thread(new HttpServiceRunnable(httpService, conn));
					serviceThread.setDaemon(true);
					serviceThread.setName("serviceThread");
					serviceThread.start();

				} catch (InterruptedIOException ex) {
					break;
				} catch (IOException e) {
					LOG.warning("I/O error initialising connection thread: " + e.getMessage());
					break;
				}
			}
		}
	}

	/**
	 * Class associated with the http service to translate the http requests in
	 * coap requests.
	 * 
	 * @author Francesco Corazza
	 */
	private class ProxyRequestHandler implements HttpRequestHandler {
		/**
		 * Instantiates a new proxy request handler.
		 */
		public ProxyRequestHandler() {
			super();
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.protocol.HttpRequestHandler#handle(org.apache.http
		 * .HttpRequest, org.apache.http.HttpResponse,
		 * org.apache.http.protocol.HttpContext)
		 */
		@Override
		public void handle(final HttpRequest httpRequest, final HttpResponse httpResponse, final HttpContext context) throws HttpException, IOException {
			// DEBUG
			System.out.println(">> Request: " + httpRequest);

			// get the http method
			String httpMethod = httpRequest.getRequestLine().getMethod().toLowerCase();

			// translate the http method
			String coapMethod = HttpTranslator.TRANSLATION_PROPERTIES.getProperty("http.request.method." + httpMethod);
			if (coapMethod.contains("error")) {
				// TODO check the exception
				throw new MethodNotSupportedException(httpMethod + " method not supported");
			}

			// create the coap request
			Request coapRequest = null;
			try {
				Message message = CodeRegistry.getMessageClass(Integer.parseInt(coapMethod)).newInstance();

				// safe cast
				if (message instanceof Request) {
					coapRequest = (Request) message;
				} else {
					LOG.severe("Failed to convert request number " + coapMethod);
					throw new HttpException(coapMethod + " not recognized"); // TODO
				}
			} catch (NumberFormatException e) {
				LOG.severe("Failed to convert request number " + coapMethod + ": " + e.getMessage());
				throw new HttpException("Error in creating the request: " + coapMethod, e); // TODO
			} catch (InstantiationException e) {
				LOG.severe("Failed to convert request number " + coapMethod + ": " + e.getMessage());
				throw new HttpException("Error in creating the request: " + coapMethod, e); // TODO
			} catch (IllegalAccessException e) {
				LOG.severe("Failed to convert request number " + coapMethod + ": " + e.getMessage());
				throw new HttpException("Error in creating the request: " + coapMethod, e); // TODO
			}

			// fill the coap request
			HttpTranslator.fillCoapRequest(httpRequest, coapRequest);

			// doReceiveMessage(coapRequest);

			// enable response queue for synchronous I/O
			coapRequest.enableResponseQueue(true);

			// execute the request
			try {
				coapRequest.execute();
			} catch (IOException e) {
				LOG.severe("Failed to execute request: " + e.getMessage());
				throw new HttpException("Failed to execute request", e);
			}

			// receive response
			Response coapResponse = null;
			try {
				coapResponse = coapRequest.receiveResponse();

			} catch (InterruptedException e) {
				LOG.severe("Receiving of response interrupted: " + e.getMessage());
				throw new HttpException("Receiving of response interrupted", e);
			}

			if (coapResponse != null) {
				String method = httpRequest.getRequestLine().getMethod().toLowerCase();
				boolean head = method.equalsIgnoreCase("HEAD");
				// translate the received response to the http response
				HttpTranslator.fillHttpResponse(httpResponse, coapResponse, head);
			} else {
				LOG.severe("No response received.");
				throw new NoHttpResponseException("No response received.");
			}

			// DEBUG
			System.out.println("<< Response: " + httpResponse);
		}
	}
}

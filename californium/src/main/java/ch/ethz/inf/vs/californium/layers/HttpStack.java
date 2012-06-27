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
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.impl.nio.SSLNHttpServerConnectionFactory;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.protocol.AbstractAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerRegistry;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerResolver;
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
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
	private static final String LOCAL_RESOURCE_NAME = "proxy";

	private ConcurrentHashMap<Request, ProxyResponseProducer> pendingResponsesMap = new ConcurrentHashMap<Request, HttpStack.ProxyResponseProducer>();

	/**
	 * Instantiates a new http stack on the requested port.
	 * 
	 * @param httpPort
	 *            the http port
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public HttpStack(int httpPort) throws IOException {

		// create the listener thread
		Thread thread = new ListenerThread(httpPort);
		thread.setDaemon(false);
		thread.start();
	}

	/**
	 * Checks if is waiting.
	 * 
	 * @param message
	 *            the message
	 * @return true, if is waiting
	 */
	public boolean isWaiting(Message message) {
		if (!(message instanceof Response)) {
			return false;
		}

		Request request = ((Response) message).getRequest();
		return pendingResponsesMap.containsKey(request);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ch.ethz.inf.vs.californium.layers.UpperLayer#doSendMessage(ch.ethz.inf
	 * .vs.californium.coap.Message)
	 */
	@Override
	protected void doSendMessage(Message message) throws IOException {
		// check only if the message is a response
		if (message instanceof Response) {
			Response coapResponse = (Response) message;
			// retrieve the request linked to the response
			Request coapResquest = coapResponse.getRequest();
			// get the producer from the reuqest sent
			ProxyResponseProducer responseProducer = pendingResponsesMap.get(coapResquest);
			// set the response in order to send the corresponding http response
			if (responseProducer != null) {
				responseProducer.setResponse(coapResponse);
			}

			// delete the entry from the map
			pendingResponsesMap.remove(coapResquest);
		}
	}

	/**
	 * The Class BaseRequestHandler.
	 * 
	 * @author Francesco Corazza
	 */
	private class BaseRequestHandler implements
			HttpAsyncRequestHandler<HttpRequest> {

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncRequestHandler#handle(java.
		 * lang.Object, org.apache.http.nio.protocol.HttpAsyncExchange,
		 * org.apache.http.protocol.HttpContext)
		 */
		@Override
		public void handle(HttpRequest httpRequest, HttpAsyncExchange httpExchange, HttpContext context) throws HttpException, IOException {
			HttpResponse httpResponse = httpExchange.getResponse();
			httpResponse.setStatusCode(HttpStatus.SC_OK);
			httpResponse.setEntity(new StringEntity("Californium Proxy server"));
			httpExchange.submitResponse(new BasicAsyncResponseProducer(httpResponse));
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncRequestHandler#processRequest
		 * (org.apache.http.HttpRequest, org.apache.http.protocol.HttpContext)
		 */
		@Override
		public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context) throws HttpException, IOException {
			// Buffer request content in memory for simplicity
			return new BasicAsyncRequestConsumer();
		}

	}

	/**
	 * The Class ListenerThread.
	 * 
	 * @author Francesco Corazza
	 */
	private class ListenerThread extends Thread {

		private int httpPort;

		/**
		 * Instantiates a new listener thread.
		 * 
		 * @param httpPort
		 *            the http port
		 */
		public ListenerThread(int httpPort) {
			super("ListenerThread");
			this.httpPort = httpPort;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {

			// HTTP parameters for the server
			HttpParams params = new SyncBasicHttpParams();
			params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024).setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true).setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpTest/1.1");

			// Create HTTP protocol processing chain
			HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] {
					// Use standard server-side protocol interceptors
			new ResponseDate(), new ResponseServer(), new ResponseContent(), new ResponseConnControl() });
			// Create request handler registry
			HttpAsyncRequestHandlerRegistry registry = new HttpAsyncRequestHandlerRegistry();

			// Register the default handler for all URIs
			registry.register("/" + LOCAL_RESOURCE_NAME + "/*", new ProxyRequestHandler(LOCAL_RESOURCE_NAME));
			registry.register("*", new BaseRequestHandler());

			// Create server-side HTTP protocol handler
			HttpAsyncService protocolHandler = new ProxyAsyncService(httpproc, new DefaultConnectionReuseStrategy(), registry, params);

			// Create HTTP connection factory
			NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory = null;
			if (httpPort == 8443) { // TODO not 443?
				// Initialize SSL context
				ClassLoader cl = HttpStack.class.getClassLoader();
				URL url = cl.getResource("my.keystore");
				if (url == null) {
					System.out.println("Keystore not found");
					System.exit(1);
				}
				try {
					KeyStore keystore = KeyStore.getInstance("jks");
					keystore.load(url.openStream(), "secret".toCharArray());
					KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
					kmfactory.init(keystore, "secret".toCharArray());
					KeyManager[] keymanagers = kmfactory.getKeyManagers();
					SSLContext sslcontext = SSLContext.getInstance("TLS");
					sslcontext.init(keymanagers, null, null);
					connFactory = new SSLNHttpServerConnectionFactory(sslcontext, null, params);
				} catch (KeyManagementException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KeyStoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CertificateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnrecoverableKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				connFactory = new DefaultNHttpServerConnectionFactory(params);
			}

			// Create server-side I/O event dispatch
			IOEventDispatch ioEventDispatch = new DefaultHttpServerIODispatch(protocolHandler, connFactory);

			try {
				// Create server-side I/O reactor
				ListeningIOReactor ioReactor = new DefaultListeningIOReactor();
				// Listen of the given port
				ioReactor.listen(new InetSocketAddress(httpPort));
				// Starts the reactor and initiates the dispatch of I/O event
				// notifications to the given IOEventDispatch.
				ioReactor.execute(ioEventDispatch);
			} catch (InterruptedIOException ex) {
				System.err.println("Interrupted");
			} catch (IOException e) {
				System.err.println("I/O error: " + e.getMessage());
			}

			System.out.println("Shutdown");
		}
	}

	/**
	 * The Class ProxyAsyncService.
	 * 
	 * @author Francesco Corazza
	 */
	private final class ProxyAsyncService extends HttpAsyncService {

		/**
		 * Instantiates a new proxy async service.
		 * 
		 * @param httpProcessor
		 *            the http processor
		 * @param connStrategy
		 *            the conn strategy
		 * @param handlerResolver
		 *            the handler resolver
		 * @param params
		 *            the params
		 */
		private ProxyAsyncService(HttpProcessor httpProcessor, ConnectionReuseStrategy connStrategy, HttpAsyncRequestHandlerResolver handlerResolver, HttpParams params) {
			super(httpProcessor, connStrategy, handlerResolver, params);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncService#closed(org.apache.http
		 * .nio.NHttpServerConnection)
		 */
		@Override
		public void closed(final NHttpServerConnection conn) {
			System.out.println(conn + ": connection closed");
			super.closed(conn);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncService#connected(org.apache
		 * .http.nio.NHttpServerConnection)
		 */
		@Override
		public void connected(final NHttpServerConnection conn) {
			System.out.println(conn + ": connection open");
			super.connected(conn);
		}
	}

	/**
	 * The Class ProxyRequestConsumer.
	 * 
	 * @author Francesco Corazza
	 */
	private final class ProxyRequestConsumer extends
			AbstractAsyncRequestConsumer<Request> {
		private volatile Request coapRequest;
		private volatile boolean completed;
		private volatile ByteBuffer data;
		private String localResource;

		/**
		 * Instantiates a new proxy request consumer.
		 * 
		 * @param localResource
		 *            the local resource
		 */
		public ProxyRequestConsumer(String localResource) {
			this.localResource = localResource;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.AbstractAsyncRequestConsumer#buildResult
		 * (org.apache.http.protocol.HttpContext)
		 */
		@Override
		protected Request buildResult(HttpContext context) throws Exception {
			return coapRequest;
		}

		/*
		 * (non-Javadoc)
		 * @see org.apache.http.nio.protocol.AbstractAsyncRequestConsumer#
		 * onContentReceived(org.apache.http.nio.ContentDecoder,
		 * org.apache.http.nio.IOControl)
		 */
		@Override
		protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
			if (!completed) {
				// Read data in
				// TODO check
				ByteBuffer buffer = ByteBuffer.allocate(2048);
				decoder.read(buffer);

				// add the bytes
				int bufferLength = 0;
				if (data != null) {
					bufferLength = data.position();
				}
				ByteBuffer oldData = data;
				data = ByteBuffer.allocate(buffer.position() + bufferLength + 1);
				if (oldData != null) {
					data.put(oldData.array(), 0, bufferLength);
				}
				data.put(buffer.array(), bufferLength, buffer.position());

				// Decode will be marked as complete when
				// the content entity is fully transferred
				if (decoder.isCompleted()) {
					completed = true;
					coapRequest.setPayload(data.array());
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.apache.http.nio.protocol.AbstractAsyncRequestConsumer#
		 * onEntityEnclosed(org.apache.http.HttpEntity,
		 * org.apache.http.entity.ContentType)
		 */
		@Override
		protected void onEntityEnclosed(HttpEntity entity, ContentType contentType) throws IOException {
			HttpTranslator.setCoapContentType(entity, contentType, coapRequest);
		}

		/*
		 * (non-Javadoc)
		 * @see org.apache.http.nio.protocol.AbstractAsyncRequestConsumer#
		 * onRequestReceived(org.apache.http.HttpRequest)
		 */
		@Override
		protected void onRequestReceived(HttpRequest httpRequest) throws HttpException, IOException {
			// get the http method
			String httpMethod = httpRequest.getRequestLine().getMethod().toLowerCase();

			// translate the http method
			String coapMethod = HttpTranslator.TRANSLATION_PROPERTIES.getProperty("http.request.method." + httpMethod);
			if (coapMethod.contains("error")) {
				// TODO check the exception
				throw new MethodNotSupportedException(httpMethod + " method not supported");
			}

			// create the coap request
			try {
				Message message = CodeRegistry.getMessageClass(Integer.parseInt(coapMethod)).newInstance();

				// safe cast
				if (message instanceof Request) {
					coapRequest = (Request) message;
				} else {
					LOG.severe("Failed to convert request number " + coapMethod);
					throw new HttpException(coapMethod + " not recognized");
				}
			} catch (NumberFormatException e) {
				LOG.severe("Failed to convert request number " + coapMethod + ": " + e.getMessage());
				throw new HttpException("Error in creating the request: " + coapMethod, e);
			} catch (InstantiationException e) {
				LOG.severe("Failed to convert request number " + coapMethod + ": " + e.getMessage());
				throw new HttpException("Error in creating the request: " + coapMethod, e);
			} catch (IllegalAccessException e) {
				LOG.severe("Failed to convert request number " + coapMethod + ": " + e.getMessage());
				throw new HttpException("Error in creating the request: " + coapMethod, e);
			}

			// fill the coap request
			HttpTranslator.setCoapUri(localResource, httpRequest, coapRequest, true);
			HttpTranslator.setCoapOptions(httpRequest, coapRequest);
		}

		/*
		 * (non-Javadoc)
		 * @see org.apache.http.nio.protocol.AbstractAsyncRequestConsumer#
		 * releaseResources()
		 */
		@Override
		protected void releaseResources() {
			coapRequest = null;
			data = null;
			localResource = null;
		}
	}

	/**
	 * Class associated with the http service to translate the http requests in
	 * coap requests.
	 * 
	 * @author Francesco Corazza
	 */
	private class ProxyRequestHandler implements
			HttpAsyncRequestHandler<Request> {
		private String localResource;

		/**
		 * Instantiates a new proxy request handler.
		 * 
		 * @param localResource
		 *            the local resource
		 */
		public ProxyRequestHandler(String localResource) {
			super();

			this.localResource = localResource;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncRequestHandler#handle(java.
		 * lang.Object, org.apache.http.nio.protocol.HttpAsyncExchange,
		 * org.apache.http.protocol.HttpContext)
		 */
		@Override
		public void handle(Request coapRequest, HttpAsyncExchange httpExchange, HttpContext httpContext) throws HttpException, IOException {

			// HttpResponse httpResponse = httpExchange.getResponse();
			// handleInternal(coapRequest, httpResponse);
			// httpExchange.submitResponse(new
			// BasicAsyncResponseProducer(httpResponse));

			// send the request in the upper layer
			doReceiveMessage(coapRequest);

			// create the producer of the response
			ProxyResponseProducer responseProducer = new ProxyResponseProducer();

			// add the request to the pending requests
			pendingResponsesMap.put(coapRequest, responseProducer);

			// submit the response
			httpExchange.submitResponse(responseProducer);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncRequestHandler#processRequest
		 * (org.apache.http.HttpRequest, org.apache.http.protocol.HttpContext)
		 */
		@Override
		public HttpAsyncRequestConsumer<Request> processRequest(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
			// DEBUG
			System.out.println(">> Request: " + httpRequest);

			// Buffer request content in memory for simplicity
			return new ProxyRequestConsumer(localResource);
		}
	}

	/**
	 * The Class ProxyResponseProducer.
	 * 
	 * @author Francesco Corazza
	 */
	private final class ProxyResponseProducer implements
			HttpAsyncResponseProducer {
		private static final long TIMEOUT = 3; // TODO set
		private HttpResponse httpResponse;
		private volatile boolean completed;
		private Response coapResponse;
		private Semaphore semaphore = new Semaphore(0);
		private int bytesRead = 0;

		/*
		 * (non-Javadoc)
		 * @see java.io.Closeable#close()
		 */
		@Override
		public void close() throws IOException {
			httpResponse = null;
			semaphore = null;
			coapResponse = null;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncResponseProducer#failed(java
		 * .lang.Exception)
		 */
		@Override
		public void failed(Exception ex) {
			return;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncResponseProducer#generateResponse
		 * ()
		 */
		@Override
		public HttpResponse generateResponse() {

			try {
				// try to acquire the semaphore
				if (!semaphore.tryAcquire(TIMEOUT, TimeUnit.SECONDS)) {
					// if the timeout is triggered, create an ad-hoc response
					int httpCode = HttpStatus.SC_REQUEST_TIMEOUT;
					httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, httpCode, EnglishReasonPhraseCatalog.INSTANCE.getReason(httpCode, Locale.ENGLISH));
				} else {
					// create the response
					httpResponse = HttpTranslator.getHttpResponse(coapResponse);
				}
			} catch (InterruptedException e) {
				semaphore.release();
			}

			return httpResponse;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncResponseProducer#produceContent
		 * (org.apache.http.nio.ContentEncoder, org.apache.http.nio.IOControl)
		 */
		@Override
		public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
			// TODO CHECK
			if (coapResponse != null) {
				ByteBuffer buffer = ByteBuffer.wrap(coapResponse.getPayload(), bytesRead, coapResponse.getPayload().length);

				bytesRead += encoder.write(buffer);
				buffer.compact();

				if (buffer.hasRemaining()) {
					if (buffer.position() == 0) {
						if (completed) {
							encoder.complete();
						} else {
							// Input buffer is empty. Wait until the origin
							// fills up
							// the buffer
							ioctrl.suspendOutput();
						}
					}
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * org.apache.http.nio.protocol.HttpAsyncResponseProducer#responseCompleted
		 * (org.apache.http.protocol.HttpContext)
		 */
		@Override
		public void responseCompleted(HttpContext context) {
			completed = true;
		}

		/**
		 * Sets the response.
		 * 
		 * @param coapResponse
		 *            the new response
		 */
		public void setResponse(Response coapResponse) {
			// wait for the consumer thread
			while (semaphore.hasQueuedThreads()) {
				;
			}

			this.coapResponse = coapResponse;
			semaphore.release();
		}
	}
}

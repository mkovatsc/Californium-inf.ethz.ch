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
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.util.CoapTranslator;
import ch.ethz.inf.vs.californium.util.HttpTranslator;
import ch.ethz.inf.vs.californium.util.HttpTranslator.TranslationException;
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
		localEndpoint = new LocalEndpoint(true);

		// unregister the localEndpoint because the messages have to pass
		// through the proxyEnpoint
		communicator.unregisterReceiver(localEndpoint);

		// add the resource directory resource
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
		 * Send cached response.
		 * 
		 * @param request
		 *            the request
		 * @return the cached response
		 */
		public Response getCachedResponse(Request request) {
			return null;
			// TODO Auto-generated method stub
		}

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
		 * @return the response
		 */
		public Response forward(Request incomingRequest) {
			Response outgoingResponse = null;

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
				return new Response(Integer.parseInt(CoapTranslator.TRANSLATION_PROPERTIES.getProperty("coap.request.uri.malformed")));
			} catch (IOException e) {
				LOG.warning("Failed to execute request: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (InstantiationException e) {
				LOG.warning("Failed to create a new request: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (IllegalAccessException e) {
				LOG.warning("Failed to create a new request: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			}

			try {
				// receive the response
				Response receivedResponse = outgoingRequest.receiveResponse();

				if (receivedResponse != null) {
					// create the new response
					outgoingResponse = receivedResponse.getClass().newInstance();

					// create the real response for the original request
					CoapTranslator.fillResponse(receivedResponse, outgoingResponse);
				} else {
					LOG.warning("No response received.");
					return new Response(Integer.parseInt(CoapTranslator.TRANSLATION_PROPERTIES.getProperty("coap.request.timeout")));
				}

			} catch (InstantiationException e) {
				LOG.warning("Failed to create a new response: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (IllegalAccessException e) {
				LOG.warning("Failed to create a new response: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (InterruptedException e) {
				LOG.warning("Receiving of response interrupted: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			}

			return outgoingResponse;
		}
	}

	// test with http://httpbin.org/
	/**
	 * The Class HttpClient.
	 * 
	 * @author Francesco Corazza
	 */
	private class HttpClient {

		private SyncBasicHttpParams httpParams;
		private ImmutableHttpProcessor httpProcessor;
		private HttpRequestExecutor httpExecutor;
		private BasicHttpContext httpContext;

		/**
		 * Instantiates a new http client.
		 */
		public HttpClient() {
			// init
			httpParams = new SyncBasicHttpParams();
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
			// TODO check the user agent
			HttpProtocolParams.setUserAgent(httpParams, "Mozilla/5.0");
			HttpProtocolParams.setUseExpectContinue(httpParams, true);

			httpProcessor = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
					// Required protocol interceptors
			new RequestContent(), new RequestTargetHost(),
					// Recommended protocol interceptors
			new RequestConnControl(), new RequestUserAgent(), new RequestExpectContinue() });

			httpExecutor = new HttpRequestExecutor();

			httpContext = new BasicHttpContext(null);
		}

		/**
		 * Forward.
		 * 
		 * @param coapRequest
		 *            the coap request
		 * @return the response
		 */
		public Response forward(Request coapRequest) {
			Response coapResponse = null;

			URI httpUri;
			try {
				httpUri = coapRequest.getProxyUri();
			} catch (URISyntaxException e1) {
				return new Response(CodeRegistry.RESP_BAD_OPTION);
			}

			// get the requested host
			// if the port is not specified, it returns -1, but it is coherent
			// with the HttpHost object
			HttpHost httpHost = new HttpHost(httpUri.getHost(), httpUri.getPort(), httpUri.getScheme());

			DefaultHttpClientConnection connection = new DefaultHttpClientConnection();
			ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();

			httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, connection);
			httpContext.setAttribute(ExecutionContext.HTTP_TARGET_HOST, httpHost);

			// create the connection if not already active
			if (!connection.isOpen()) {
				// TODO edit the port based on the scheme chosen

				/* connection */
				try {
					Socket socket = new Socket(httpHost.getHostName(), httpHost.getPort() == -1 ? 80 : httpHost.getPort());
					connection.bind(socket, httpParams);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			/* request */
			HttpRequest httpRequest = null;
			try {
				httpRequest = HttpTranslator.getHttpRequest(coapRequest);

				// DEBUG
				System.out.println(">> Request: " + httpRequest.getRequestLine());

				// preprocess the request
				httpRequest.setParams(httpParams);
				httpExecutor.preProcess(httpRequest, httpProcessor, httpContext);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TranslationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/* response */
			try {
				// send the request
				HttpResponse httpResponse = httpExecutor.execute(httpRequest, connection, httpContext);
				httpResponse.setParams(httpParams);
				httpExecutor.postProcess(httpResponse, httpProcessor, httpContext);

				// DEBUG
				System.out.println("<< Response: " + httpResponse.getStatusLine());
				// the entity of the response, if non repeatable, could be
				// consumed only one time, so do not debug it!
				// System.out.println(EntityUtils.toString(httpResponse.getEntity()));

				// translate the received http response in a coap response
				coapResponse = HttpTranslator.getCoapResponse(httpResponse);

				// close the connection if not keepalive
				if (!connStrategy.keepAlive(httpResponse, httpContext)) {
					connection.close();
				} else {
					System.out.println("Connection kept alive...");
				}

				// }
			} catch (UnsupportedEncodingException e) {
				LOG.warning("Failed to create a new response: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (ParseException e) {
				LOG.warning("Failed to create a new request: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (IOException e) {
				LOG.warning("Failed to create a new request: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (HttpException e) {
				LOG.warning("Failed to create a new request: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			} catch (TranslationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					connection.close();
				} catch (IOException e) {
				}
			}

			return coapResponse;
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
			Response response = null;

			// check if the cache has a saved version of the request
			if (coapCache.isCached(request)) {
				response = coapCache.getCachedResponse(request);
			}

			// check for the proxy-uri option
			if (request.isProxyUriSet()) {
				// check which schema is requested
				int proxyUriOptNumber = OptionNumberRegistry.PROXY_URI;
				Option proxyUriOption = request.getFirstOption(proxyUriOptNumber);
				String proxyUriString = proxyUriOption.getStringValue();
				if (proxyUriString.matches("^http.*")) {
					// forward the to the requested coap client
					HttpClient httpClient = new HttpClient();
					response = httpClient.forward(request);
				} else {
					// forward the to the requested coap client
					CoapClient coapClient = new CoapClient();
					response = coapClient.forward(request);
				}
			} else {
				// forward to localEndpoint for the local resources
				localEndpoint.execute(request);
			}

			// send the response
			request.respond(response);
			request.sendResponse();
		}
	}
}

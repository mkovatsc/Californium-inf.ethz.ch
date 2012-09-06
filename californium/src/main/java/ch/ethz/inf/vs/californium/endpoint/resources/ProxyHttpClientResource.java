/**
 * 
 */

package ch.ethz.inf.vs.californium.endpoint.resources;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestDate;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.CoapTranslator;
import ch.ethz.inf.vs.californium.util.HttpTranslator;
import ch.ethz.inf.vs.californium.util.InvalidFieldException;
import ch.ethz.inf.vs.californium.util.TranslationException;

/**
 * // test with http://httpbin.org/
 * 
 * @author Francesco Corazza
 * 
 */
public class ProxyHttpClientResource extends ForwardingResource {

	private static final int KEEP_ALIVE = 5000;
	// DefaultHttpClient is thread safe. It is recommended that the same
	// instance of this class is reused for multiple request executions.
	private static final AbstractHttpClient HTTP_CLIENT = new DefaultHttpClient();

	static {
		HTTP_CLIENT.addRequestInterceptor(new RequestAcceptEncoding());
		HTTP_CLIENT.addRequestInterceptor(new RequestConnControl());
		HTTP_CLIENT.addRequestInterceptor(new RequestContent());
		HTTP_CLIENT.addRequestInterceptor(new RequestDate());
		HTTP_CLIENT.addRequestInterceptor(new RequestExpectContinue());
		HTTP_CLIENT.addRequestInterceptor(new RequestTargetHost());
		HTTP_CLIENT.addRequestInterceptor(new RequestUserAgent());

		HTTP_CLIENT.addResponseInterceptor(new ResponseContentEncoding());

		HTTP_CLIENT.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
			@Override
			public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				long keepAlive = super.getKeepAliveDuration(response, context);
				if (keepAlive == -1) {
					// Keep connections alive if a keep-alive value
					// has not be explicitly set by the server
					keepAlive = KEEP_ALIVE;
				}
				return keepAlive;
			}

		});
	}

	public ProxyHttpClientResource() {
		// set the resource hidden
		super("proxy/httpClient", true);
		setTitle("Forward the requests to a HTTP server.");
	}

	@Override
	public Response forwardRequest(final Request incomingCoapRequest) {
		// check the invariant: the request must have the proxy-uri set
		if (!incomingCoapRequest.hasOption(OptionNumberRegistry.PROXY_URI)) {
			LOG.warning("Proxy-uri option not set.");
			return new Response(CodeRegistry.RESP_BAD_OPTION);
		}

		// remove the fake uri-path
		incomingCoapRequest.removeOptions(OptionNumberRegistry.URI_PATH); // HACK

		// get the proxy-uri set in the incoming coap request
		URI proxyUri;
		try {
			proxyUri = incomingCoapRequest.getProxyUri();
		} catch (URISyntaxException e) {
			LOG.warning("Proxy-uri option malformed: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_FIELD_MALFORMED);
		}

		// get the requested host, if the port is not specified, the constructor
		// sets it to -1
		HttpHost httpHost = new HttpHost(proxyUri.getHost(), proxyUri.getPort(), proxyUri.getScheme());

		HttpRequest httpRequest = null;
		try {
			// get the mapping to http for the incoming coap request
			httpRequest = HttpTranslator.getHttpRequest(incomingCoapRequest);
			LOG.info("Outgoing http request: " + httpRequest.getRequestLine());
		} catch (InvalidFieldException e) {
			LOG.warning("Problems during the http/coap translation: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_FIELD_MALFORMED);
		} catch (TranslationException e) {
			LOG.warning("Problems during the http/coap translation: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_TRANSLATION_ERROR);
		}

		ResponseHandler<Response> httpResponseHandler = new ResponseHandler<Response>() {
			@Override
			public Response handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
				LOG.info("Incoming http response: " + httpResponse.getStatusLine());
				// the entity of the response, if non repeatable, could be
				// consumed only one time, so do not debug it!
				// System.out.println(EntityUtils.toString(httpResponse.getEntity()));

				// translate the received http response in a coap response
				try {
					return HttpTranslator.getCoapResponse(httpResponse, incomingCoapRequest);
				} catch (InvalidFieldException e) {
					LOG.warning("Problems during the http/coap translation: " + e.getMessage());
					return new Response(CoapTranslator.STATUS_FIELD_MALFORMED);
				} catch (TranslationException e) {
					LOG.warning("Problems during the http/coap translation: " + e.getMessage());
					return new Response(CoapTranslator.STATUS_TRANSLATION_ERROR);
				}
			}
		};

		Response coapResponse = null;
		try {
			// execute the request
			coapResponse = HTTP_CLIENT.execute(httpHost, httpRequest, httpResponseHandler, new BasicHttpContext(null));
		} catch (IOException e) {
			LOG.warning("Failed to get the http response: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		}

		// accept the request sending a separate response to avoid the timeout
		// in the requesting client
		incomingCoapRequest.accept();
		LOG.info("Acknowledge message sent");

		return coapResponse;
	}

	public Response forwardRequestOld(Request incomingCoapRequest) {

		// check the invariant: the request must have the proxy-uri set
		if (!incomingCoapRequest.hasOption(OptionNumberRegistry.PROXY_URI)) {
			LOG.warning("Proxy-uri option not set.");
			return new Response(CodeRegistry.RESP_BAD_OPTION);
		}

		// accept the request sending a separate response to avoid the timeout
		// in the requesting client
		incomingCoapRequest.accept();

		// remove the fake uri-path
		incomingCoapRequest.removeOptions(OptionNumberRegistry.URI_PATH); // HACK

		// init
		HttpParams httpParams = new SyncBasicHttpParams();
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
		// The user agent is set with a common value
		HttpProtocolParams.setUserAgent(httpParams, "Mozilla/5.0");
		HttpProtocolParams.setUseExpectContinue(httpParams, true);

		HttpProcessor httpProcessor = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
				// Required protocol interceptors
		new RequestContent(), new RequestTargetHost(),
				// Recommended protocol interceptors
		new RequestConnControl(), new RequestUserAgent(), new RequestExpectContinue() });

		HttpRequestExecutor httpExecutor = new HttpRequestExecutor();

		HttpContext httpContext = new BasicHttpContext(null);

		// get the proxy-uri set in the incoming coap request
		URI proxyUri;
		try {
			proxyUri = incomingCoapRequest.getProxyUri();
		} catch (URISyntaxException e) {
			LOG.warning("Proxy-uri option malformed: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_FIELD_MALFORMED);
		}

		// get the requested host, if the port is not specified, the constructor
		// sets it to -1
		HttpHost httpHost = new HttpHost(proxyUri.getHost(), proxyUri.getPort(), proxyUri.getScheme());

		DefaultHttpClientConnection connection = new DefaultHttpClientConnection();
		ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();

		httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, connection);
		httpContext.setAttribute(ExecutionContext.HTTP_TARGET_HOST, httpHost);

		// create the connection if not already active
		if (!connection.isOpen()) {

			try {
				// create the client socket to the specified host's port
				// if the port is not set, assume the default http port
				Socket socket = new Socket(httpHost.getHostName(), httpHost.getPort() == -1 ? 80 : httpHost.getPort());
				connection.bind(socket, httpParams);
				LOG.info("Created client http socket: " + socket);
			} catch (UnknownHostException e) {
				// it means that the socket cannot be build because the the IP
				// is not well formed
				LOG.warning("Could not determinate the host: " + e.getMessage());
				return new Response(CoapTranslator.STATUS_FIELD_MALFORMED);
			} catch (IOException e) {
				LOG.severe("Failed to create the socket: " + e.getMessage());
				return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			}
		}

		Response coapResponse = null;
		HttpRequest httpRequest = null;
		try {
			// get the mapping to http for the incoming coap request
			httpRequest = HttpTranslator.getHttpRequest(incomingCoapRequest);
			LOG.info("Outgoing http request: " + httpRequest.getRequestLine());

			// pre-process the request
			httpRequest.setParams(httpParams);
			httpExecutor.preProcess(httpRequest, httpProcessor, httpContext);
			// send the request
			HttpResponse httpResponse = httpExecutor.execute(httpRequest, connection, httpContext);
			// process the response
			httpResponse.setParams(httpParams);
			httpExecutor.postProcess(httpResponse, httpProcessor, httpContext);

			LOG.info("Incoming http response: " + httpResponse.getStatusLine());
			// the entity of the response, if non repeatable, could be
			// consumed only one time, so do not debug it!
			// System.out.println(EntityUtils.toString(httpResponse.getEntity()));

			// translate the received http response in a coap response
			coapResponse = HttpTranslator.getCoapResponse(httpResponse, incomingCoapRequest);

			// close the connection if not keepalive
			if (!connStrategy.keepAlive(httpResponse, httpContext)) {
				connection.close();
			} else {
				LOG.info("Connection kept alive...");
			}
		} catch (InvalidFieldException e) {
			LOG.warning("Problems during the http/coap translation: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_FIELD_MALFORMED);
		} catch (TranslationException e) {
			LOG.warning("Problems during the http/coap translation: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_TRANSLATION_ERROR);
		} catch (HttpException e) {
			LOG.warning("Violation of http protocol: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			LOG.warning("Failed to get the http response: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		} finally {
			try {
				connection.close();
			} catch (IOException e) {
				// empty
			}
		}

		return coapResponse;
	}
}

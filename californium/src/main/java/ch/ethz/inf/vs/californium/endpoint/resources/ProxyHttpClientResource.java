/**
 * 
 */

package ch.ethz.inf.vs.californium.endpoint.resources;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.apache.http.ParseException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
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
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.HttpTranslator;
import ch.ethz.inf.vs.californium.util.TranslationException;

/**
 * // test with http://httpbin.org/
 * 
 * @author Francesco Corazza
 * 
 */
public class ProxyHttpClientResource extends LocalResource {

	public ProxyHttpClientResource() {
		super("proxy/httpClient");

	}

	/**
	 * Forward.
	 * 
	 * @param coapRequest
	 *            the coap request
	 * @return the response
	 */
	public Response forward(Request coapRequest) {
		// remove the fake uri-path
		coapRequest.removeOptions(OptionNumberRegistry.URI_PATH);

		// init
		HttpParams httpParams = new SyncBasicHttpParams();
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
		// TODO check the user agent
		HttpProtocolParams.setUserAgent(httpParams, "Mozilla/5.0");
		HttpProtocolParams.setUseExpectContinue(httpParams, true);

		HttpProcessor httpProcessor = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
				// Required protocol interceptors
		new RequestContent(), new RequestTargetHost(),
				// Recommended protocol interceptors
		new RequestConnControl(), new RequestUserAgent(), new RequestExpectContinue() });

		HttpRequestExecutor httpExecutor = new HttpRequestExecutor();

		HttpContext httpContext = new BasicHttpContext(null);

		Response coapResponse = null;

		URI proxyUri;
		try {
			proxyUri = coapRequest.getProxyUri();
		} catch (URISyntaxException e1) {
			return new Response(CodeRegistry.RESP_BAD_OPTION);
		}

		// get the requested host
		// if the port is not specified, it returns -1, but it is coherent
		// with the HttpHost object
		HttpHost httpHost = new HttpHost(proxyUri.getHost(), proxyUri.getPort(), proxyUri.getScheme());

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
				LOG.warning("Failed to create the socket: " + e.getMessage());
				return new Response(CodeRegistry.RESP_BAD_OPTION);
			} catch (IOException e) {
				LOG.warning("Failed to create the socket: " + e.getMessage());
				return new Response(CodeRegistry.RESP_BAD_OPTION);
			}
		}

		/* request */
		HttpRequest httpRequest = null;
		try {
			httpRequest = HttpTranslator.getHttpRequest(coapRequest);

			// DEBUG
			LOG.info(">> Request: " + httpRequest.getRequestLine());

			// preprocess the request
			httpRequest.setParams(httpParams);
			httpExecutor.preProcess(httpRequest, httpProcessor, httpContext);
		} catch (URISyntaxException e) {
			LOG.warning("Failed to create a new request: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		} catch (TranslationException e) {
			LOG.warning("Failed to create a new request: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		} catch (HttpException e) {
			LOG.warning("Failed to create a new request: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			LOG.warning("Failed to create a new request: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		}

		/* response */
		try {
			// send the request
			HttpResponse httpResponse = httpExecutor.execute(httpRequest, connection, httpContext);
			httpResponse.setParams(httpParams);
			httpExecutor.postProcess(httpResponse, httpProcessor, httpContext);

			// DEBUG
			LOG.info("<< Response: " + httpResponse.getStatusLine());
			// the entity of the response, if non repeatable, could be
			// consumed only one time, so do not debug it!
			// System.out.println(EntityUtils.toString(httpResponse.getEntity()));

			// translate the received http response in a coap response
			coapResponse = HttpTranslator.getCoapResponse(httpResponse);

			// close the connection if not keepalive
			if (!connStrategy.keepAlive(httpResponse, httpContext)) {
				connection.close();
			} else {
				LOG.info("Connection kept alive...");
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
			LOG.warning("Failed to create a new request: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		} finally {
			try {
				connection.close();
			} catch (IOException e) {
			}
		}

		return coapResponse;
	}

	@Override
	public void performDELETE(DELETERequest request) {
		Response response = forward(request);
		request.respond(response);
	}

	@Override
	public void performGET(GETRequest request) {
		Response response = forward(request);
		request.respond(response);
	}

	@Override
	public void performPOST(POSTRequest request) {
		Response response = forward(request);
		request.respond(response);
	}

	@Override
	public void performPUT(PUTRequest request) {
		Response response = forward(request);
		request.respond(response);
	}

}

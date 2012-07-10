/**
 * 
 */

package ch.ethz.inf.vs.californium.endpoint.resources;

import java.io.IOException;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.CoapTranslator;

/**
 * @author Francesco Corazza
 * 
 */
public class ProxyCoapClientResource extends LocalResource {

	public ProxyCoapClientResource() {
		super("proxy/coapClient");
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

	private Response forward(Request incomingRequest) {
		// remove the fake uri-path
		incomingRequest.removeOptions(OptionNumberRegistry.URI_PATH);

		// TODO check the incoming request well formedness

		Response outgoingResponse = null;

		// create the new request to forward to the requested coap server
		Request outgoingRequest = null;
		try {
			// create the new request from the original
			outgoingRequest = incomingRequest.getClass().newInstance();

			// fill the new request to forward
			CoapTranslator.fillRequest(incomingRequest, outgoingRequest);

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

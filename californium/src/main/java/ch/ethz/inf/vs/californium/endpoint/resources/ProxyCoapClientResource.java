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
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.CoapTranslator;

/**
 * @author Francesco Corazza
 * 
 */
public class ProxyCoapClientResource extends LocalResource {

	public ProxyCoapClientResource() {
		// set the resource hidden
		super("proxy/coapClient", true);
		setTitle("Forward the requests to a CoAP server.");
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
		incomingRequest.removeOptions(OptionNumberRegistry.URI_PATH); // HACK

		// create a new request to forward to the requested coap server
		Request outgoingRequest = null;
		try {
			// create the new request from the original
			outgoingRequest = CoapTranslator.getRequest(incomingRequest);

			// enable response queue for blocking I/O
			outgoingRequest.enableResponseQueue(true);

			// get the token from the manager
			outgoingRequest.setToken(TokenManager.getInstance().acquireToken());

			// execute the request
			LOG.finer("Sending coap request.");
			outgoingRequest.execute();
		} catch (URISyntaxException e) {
			LOG.warning("Proxy-uri option malformed: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_URI_MALFORMED);
		} catch (IOException e) {
			LOG.warning("Failed to execute request: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		}

		try {
			// receive the response
			Response receivedResponse = outgoingRequest.receiveResponse();

			if (receivedResponse != null) {
				LOG.finer("Coap response received.");

				// create the real response for the original request
				Response outgoingResponse = CoapTranslator.getResponse(receivedResponse);
				return outgoingResponse;
			} else {
				LOG.warning("No response received.");
				return new Response(CoapTranslator.STATUS_TIMEOUT);
			}
		} catch (InterruptedException e) {
			LOG.warning("Receiving of response interrupted: " + e.getMessage());
			return new Response(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
		}
	}
}

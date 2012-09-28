/**
 * 
 */

package ch.ethz.inf.vs.californium.endpoint.resources;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.CoapTranslator;
import ch.ethz.inf.vs.californium.util.TranslationException;

/**
 * Resource that forwards a coap request with the proxy-uri option set to the
 * desired coap server.
 * 
 * @author Francesco Corazza
 * 
 */
public class ProxyCoapClientResource extends ForwardingResource {

	public ProxyCoapClientResource() {
		// set the resource hidden
		super("proxy/coapClient", true);
		setTitle("Forward the requests to a CoAP server.");
	}

	@Override
	public Response forwardRequest(Request incomingRequest) {

		// check the invariant: the request must have the proxy-uri set
		if (!incomingRequest.hasOption(OptionNumberRegistry.PROXY_URI)) {
			LOG.warning("Proxy-uri option not set.");
			return new Response(CodeRegistry.RESP_BAD_OPTION);
		}

		// remove the fake uri-path
		// FIXME: HACK
		incomingRequest.removeOptions(OptionNumberRegistry.URI_PATH);

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

			// accept the request sending a separate response to avoid the
			// timeout in the requesting client
			incomingRequest.accept();
			LOG.finer("Acknowledge message sent");
		} catch (TranslationException e) {
			LOG.warning("Proxy-uri option malformed: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_FIELD_MALFORMED);
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

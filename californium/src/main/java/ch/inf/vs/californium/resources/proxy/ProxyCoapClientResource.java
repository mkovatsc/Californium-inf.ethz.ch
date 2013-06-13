package ch.inf.vs.californium.resources.proxy;

import java.util.logging.Logger;

import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.CalifonriumLogger;

/**
 * Resource that forwards a coap request with the proxy-uri option set to the
 * desired coap server.
 * 
 * @author Francesco Corazza
 * 
 */
public class ProxyCoapClientResource extends ForwardingResource {

	private final static Logger LOGGER = CalifonriumLogger.getLogger(ProxyCoapClientResource.class);
	
	public ProxyCoapClientResource() {
		this("proxy/coapClient");
	} 
	
	public ProxyCoapClientResource(String name) {
		// set the resource hidden
		super(name, true);
		getInfo().setTitle("Forward the requests to a CoAP server.");
	}

	@Override
	public Response forwardRequest(Exchange exchange) {
		Request incomingRequest = exchange.getRequest();

		// check the invariant: the request must have the proxy-uri set
		if (!incomingRequest.getOptions().hasProxyURI()) {
			LOGGER.warning("Proxy-uri option not set.");
			return new Response(ResponseCode.BAD_OPTION);
		}

		// remove the fake uri-path
		// FIXME: HACK // TODO: why? still necessary in new Cf?
		incomingRequest.getOptions().clearURIPaths();

		// create a new request to forward to the requested coap server
		Request outgoingRequest = null;
		try {
			// create the new request from the original
			outgoingRequest = CoapTranslator.getRequest(incomingRequest);

//			// enable response queue for blocking I/O
//			outgoingRequest.enableResponseQueue(true);

			// get the token from the manager // TODO: necessary?
//			outgoingRequest.setToken(TokenManager.getInstance().acquireToken());

			// execute the request
			LOGGER.finer("Sending coap request.");
//			outgoingRequest.execute();
			exchange.getEndpoint().sendRequest(outgoingRequest);

			// accept the request sending a separate response to avoid the
			// timeout in the requesting client
			exchange.accept();
			LOGGER.finer("Acknowledge message sent");
		} catch (TranslationException e) {
			LOGGER.warning("Proxy-uri option malformed: " + e.getMessage());
			return new Response(CoapTranslator.STATUS_FIELD_MALFORMED);
		} catch (Exception e) {
			LOGGER.warning("Failed to execute request: " + e.getMessage());
			return new Response(ResponseCode.INTERNAL_SERVER_ERROR);
		}

		try {
			// receive the response // TODO: don't wait for ever
			Response receivedResponse = outgoingRequest.waitForResponse();

			if (receivedResponse != null) {
				LOGGER.finer("Coap response received.");

				// create the real response for the original request
				Response outgoingResponse = CoapTranslator.getResponse(receivedResponse);

				return outgoingResponse;
			} else {
				LOGGER.warning("No response received.");
				return new Response(CoapTranslator.STATUS_TIMEOUT);
			}
		} catch (InterruptedException e) {
			LOGGER.warning("Receiving of response interrupted: " + e.getMessage());
			return new Response(ResponseCode.INTERNAL_SERVER_ERROR);
		}
	}
}

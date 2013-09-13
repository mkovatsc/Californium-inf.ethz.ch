package ch.ethz.inf.vs.californium.server;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;

/**
 * The MessageDeliverer is the main delivery mechanism to deliver requests and
 * responses to a {@link Server}. To deliver a request, the MessageDeliverer has
 * to find the appropriate {@link Resource} or respond with a response with code
 * {@link ResponseCode#NOT_FOUND}. To deliver a response, the MessageDeliverer
 * usually just adds the response to the request.
 * <p>
 * {@link EndpointManager} uses a simple implementation of this interface to
 * deliver incoming responses to requests but rejects incoming requests. It is
 * only useful to a client-only application.
 * 
 * @see ServerMessageDeliverer
 */
public interface MessageDeliverer {

	/**
	 * Delivers the specified request to the appropriate {@link Resource}.
	 * 
	 * @param exchange
	 *            the exchange
	 */
	public void deliverRequest(Exchange exchange);
	
	/**
	 * Delivers the specified response. Usually, the MessageDeliverer just adds
	 * the response to the origin request.
	 * 
	 * @param exchange
	 *            the exchange
	 * @param response
	 *            the response
	 */
	public void deliverResponse(Exchange exchange, Response response);
	
}

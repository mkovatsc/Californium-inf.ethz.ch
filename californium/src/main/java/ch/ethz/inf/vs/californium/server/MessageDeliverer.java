package ch.ethz.inf.vs.californium.server;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;

/**
 * A strategy for delivering inbound CoAP messages to an appropriate processor.
 * 
 * Implementations should try to deliver incoming CoAP requests to a published
 * resource matching the request's URI. If no such resource exists, implementations
 * should respond with a CoAP {@link ResponseCode#NOT_FOUND}. An incoming CoAP response
 * message should be delivered to its corresponding outbound request.
 */
public interface MessageDeliverer {

	/**
	 * Delivers an inbound CoAP request to an appropriate resource.
	 * 
	 * @param exchange
	 *            the exchange containing the inbound {@code Request}
	 */
	public void deliverRequest(Exchange exchange);
	
	/**
	 * Delivers an inbound CoAP response message to its corresponding request.
	 * 
	 * @param exchange
	 *            the exchange containing the originating CoAP request
	 * @param response
	 *            the inbound CoAP response message
	 */
	public void deliverResponse(Exchange exchange, Response response);
	
}

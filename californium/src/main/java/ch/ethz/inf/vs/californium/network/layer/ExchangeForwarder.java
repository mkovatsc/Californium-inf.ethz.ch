package ch.ethz.inf.vs.californium.network.layer;

import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;

public interface ExchangeForwarder {

	/**
	 * Sends the specified request over the connector that the stack is
	 * connected to.
	 * 
	 * @param exchange
	 *            the exchange
	 * @param request
	 *            the request
	 */
	public void sendRequest(Exchange exchange, Request request);

	/**
	 * Sends the specified response over the connector that the stack is
	 * connected to.
	 * 
	 * @param exchange
	 *            the exchange
	 * @param response
	 *            the response
	 */
	public void sendResponse(Exchange exchange, Response response);

	/**
	 * Sends the specified empty message over the connector that the stack is
	 * connected to.
	 * 
	 * @param exchange
	 *            the exchange
	 * @param emptyMessage
	 *            the empty message
	 */
	public void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);
	
}

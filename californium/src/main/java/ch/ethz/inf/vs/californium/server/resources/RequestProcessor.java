package ch.ethz.inf.vs.californium.server.resources;

import ch.ethz.inf.vs.californium.network.Exchange;

/**
 * A RequestProcessor is able to process requests in the sense of a server that
 * responds to the request with a response message.
 */
public interface RequestProcessor {

	/**
	 * Process the request from the specified exchange.
	 *
	 * @param exchange the exchange with the request
	 */
	public void processRequest(Exchange exchange);
	
}

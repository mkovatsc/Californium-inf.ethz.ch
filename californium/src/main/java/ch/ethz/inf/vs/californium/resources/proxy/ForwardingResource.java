package ch.ethz.inf.vs.californium.resources.proxy;

/**
 * 
 */

import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.resources.ResourceBase;

/**
 * @author Francesco Corazza
 * 
 */
public abstract class ForwardingResource extends ResourceBase {

	public ForwardingResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	public ForwardingResource(String resourceIdentifier, boolean hidden) {
		super(resourceIdentifier, hidden);
	}

	@Override
	public void processRequest(Exchange exchange) {
		Response response = forwardRequest(exchange);
		exchange.respond(response);
	}

//	@Override
//	public void processGET(Exchange exchange) {
//		Response response = forwardRequest(exchange);
//		exchange.respond(response);
//	}
//
//	@Override
//	public void processDELETE(Exchange exchange) {
//		Response response = forwardRequest(exchange);
//		exchange.respond(response);
//	}
//
//	@Override
//	public void processPOST(Exchange exchange) {
//		Response response = forwardRequest(exchange);
//		exchange.respond(response);
//	}
//
//	@Override
//	public void processPUT(Exchange exchange) {
//		Response response = forwardRequest(exchange);
//		exchange.respond(response);
//	}

	protected abstract Response forwardRequest(Exchange exchange);
}

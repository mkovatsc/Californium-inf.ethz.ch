package ch.inf.vs.californium;

import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;

public interface MessageDeliverer {

	public void deliverRequest(Exchange exchange);
	
	public void deliverResponse(Exchange exchange, Response response);
	
}

package ch.ethz.inf.vs.californium.proxy;

import ch.ethz.inf.vs.californium.coap.Request;

public interface RequestHandler {

	public void handleRequest(Request request);
	
}

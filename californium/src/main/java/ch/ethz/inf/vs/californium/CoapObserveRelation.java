package ch.ethz.inf.vs.californium;

import ch.ethz.inf.vs.californium.coap.Request;

public class CoapObserveRelation {

	private Request request;
	
	protected CoapObserveRelation(Request request) {
		this.request = request;
	}
	
	public void cancel() {
		request.cancel();
	}
	
}

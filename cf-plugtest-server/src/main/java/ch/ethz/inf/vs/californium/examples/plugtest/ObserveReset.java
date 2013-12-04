package ch.ethz.inf.vs.californium.examples.plugtest;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class ObserveReset extends ResourceBase {

	public ObserveReset() {
		super("obs-reset");
	}
	
	@Override
	public void handlePOST(Exchange exchange) {
		if (exchange.getRequest().getPayloadString().equals("sesame")) {
			System.out.println("Obs reset received POST. Clearing observers");
			
			Observe obs = (Observe) this.getParent().getChild("obs");
			ObserveNon obsNon = (ObserveNon) this.getParent().getChild("obs-non");
			obs.clearObserveRelations();
			obsNon.clearObserveRelations();
			
			exchange.respond(new Response(ResponseCode.CHANGED));
			
		} else {
			exchange.respond(ResponseCode.FORBIDDEN);
		}
	}
	
}

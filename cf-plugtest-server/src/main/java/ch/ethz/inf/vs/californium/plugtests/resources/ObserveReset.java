package ch.ethz.inf.vs.californium.plugtests.resources;

import static ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode.*;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class ObserveReset extends ResourceBase {

	public ObserveReset() {
		super("obs-reset");
	}
	
	@Override
	public void handlePOST(CoapExchange exchange) {
		if (exchange.getRequestText().equals("sesame")) {
			System.out.println("obs-reset received POST. Clearing observers");
			
			// clear observers of the obs resources
			Observe obs = (Observe) this.getParent().getChild("obs");
			ObserveNon obsNon = (ObserveNon) this.getParent().getChild("obs-non");
			obs.clearObserveRelations();
			obsNon.clearObserveRelations();
			
			exchange.respond(CHANGED);
			
		} else {
			exchange.respond(FORBIDDEN);
		}
	}
	
}

package ch.ethz.inf.vs.californium.plugtests.resources;

import static ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode.*;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class Shutdown extends ResourceBase {

	public Shutdown() {
		super("shutdown");
	}
	
	@Override
	public void handlePOST(CoapExchange exchange) {
		if (exchange.getRequestText().equals("sesame")) {
			exchange.respond(CHANGED);
			
			System.out.println("Shutdown resource received POST. Exiting");
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
			
		} else {
			exchange.respond(FORBIDDEN);
		}
	}
	
}

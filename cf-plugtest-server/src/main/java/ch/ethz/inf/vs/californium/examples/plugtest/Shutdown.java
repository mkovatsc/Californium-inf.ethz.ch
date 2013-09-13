package ch.ethz.inf.vs.californium.examples.plugtest;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.resources.ResourceBase;

public class Shutdown extends ResourceBase {

	public Shutdown() {
		super("shutdown");
	}
	
	@Override
	public void processPOST(Exchange exchange) {
		exchange.respond(new Response(ResponseCode.CHANGED));
		System.out.println("Shutdown resource received POST. Exiting");
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
}

package ch.ethz.inf.vs.californium.benchmark;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class ShutDownResource extends ResourceBase {

	public ShutDownResource(String name) {
		super(name);
	}
	
	public void handleGET(CoapExchange exchange) {
		exchange.respond("Send a POST request to this resource to shutdown the server");
	}
	
	public void handlePOST(CoapExchange exchange) {
		System.out.println("Shutting down everything in 1 second");
		exchange.respond(ResponseCode.CHANGED, "Shutting down");
		try{
			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

}

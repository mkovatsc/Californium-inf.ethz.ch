package ch.ethz.inf.vs.californium.examples.api;

import static ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode.*;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class CoAPResourceExample extends ResourceBase {

	public CoAPResourceExample(String name) {
		super(name);
	}
	
	@Override
	public void handleGET(CoapExchange exchange) {
		exchange.respond("hello world");
	}

	@Override
	public void handlePOST(CoapExchange exchange) {
		exchange.accept();
		
		if (exchange.getRequestOptions().hasContentFormat(MediaTypeRegistry.TEXT_XML)) {
			String xml = exchange.getRequestText();
			exchange.respond(CREATED, xml.toUpperCase());
			
		} else {
			// ...
			exchange.respond(CREATED);
		}
	}

	@Override
	public void handlePUT(CoapExchange exchange) {
		// ...
		exchange.respond(CHANGED);
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		delete();
		exchange.respond(DELETED);
	}

	public static void main(String[] args) {
		Server server = new Server();
		server.add(new CoAPResourceExample("example"));
		server.start();
	}

}

package ch.ethz.inf.vs.californium.examples.resources;

import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource responds with a kind "hello world" to GET requests.
 * 
 * @author Martin Lanter
 */
public class HelloWorldResource extends ResourceBase {

	public HelloWorldResource(String name) {
		super(name);
	}
	
	@Override
	public void handleGET(CoapExchange exchange) {
		exchange.respond("hello world");
	}

}

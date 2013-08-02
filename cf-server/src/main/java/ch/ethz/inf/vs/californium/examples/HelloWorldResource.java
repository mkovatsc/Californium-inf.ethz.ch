package ch.ethz.inf.vs.californium.examples;

import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.resources.ResourceBase;

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
	public void processGET(Exchange exchange) {
		exchange.respond("hello world");
	}

}

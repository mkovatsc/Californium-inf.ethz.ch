package example;

import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.ResourceBase;

public class HelloWorldResource extends ResourceBase {

	public HelloWorldResource(String name) {
		super(name);
	}
	
	@Override
	public void processGET(Exchange exchange) {
		exchange.respond("hello world");
	}

}

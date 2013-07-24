package example;

import java.util.List;

import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.Resource;
import ch.inf.vs.californium.resources.ResourceBase;

public class GreetingResource extends ResourceBase {

	public GreetingResource(String name) {
		super(name);
	}
	
	@Override
	public Resource getChild(String name) {
		return this;
	}
	
	@Override
	public void processGET(Exchange exchange) {
		List<String> path = exchange.getRequest().getOptions().getURIPaths();
		StringBuilder buffer = new StringBuilder("resource ");
		for (String p:path)
			buffer.append(p).append("/");
		buffer.append(" sais hi");
		exchange.respond(buffer.toString());
	}
}

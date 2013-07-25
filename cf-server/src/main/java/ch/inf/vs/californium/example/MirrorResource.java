package ch.inf.vs.californium.example;

import java.util.List;

import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.Resource;
import ch.inf.vs.californium.resources.ResourceBase;

public class MirrorResource extends ResourceBase {

	public MirrorResource(String name) {
		super(name);
	}
	
	@Override
	public Resource getChild(String name) {
		return this;
	}
	
	@Override
	public void processGET(Exchange exchange) {
		Request request = exchange.getRequest();
		StringBuilder buffer = new StringBuilder("resource ");
		buffer.append("Received GET request:")
			.append("\n").append("Address: ").append(request.getSource()).append(":").append(request.getSourcePort())
			.append("\n").append("Type: ").append(request.getType())
			.append("\n").append("MID: ").append(request.getMID())
			.append("\n").append("Token: ").append(request.getTokenString())
			.append("\n").append(request.getOptions());
		
		exchange.respond(buffer.toString());
	}
}

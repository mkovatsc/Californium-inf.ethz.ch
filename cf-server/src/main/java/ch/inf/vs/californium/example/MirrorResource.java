package ch.inf.vs.californium.example;

import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.Resource;
import ch.inf.vs.californium.resources.ResourceBase;

/**
 * This resource responds with the data from a request in its payload. This
 * resource responds to GET, POST, PUT and DELETE requests.
 * 
 * @author Martin Lanter
 */
public class MirrorResource extends ResourceBase {

	public MirrorResource(String name) {
		super(name);
	}
	
	@Override
	public Resource getChild(String name) {
		return this;
	}
	
	@Override
	public void processRequest(Exchange exchange) {
		Request request = exchange.getRequest();
		StringBuilder buffer = new StringBuilder();
		buffer.append("resource ").append(getURI()).append(" received request")
			.append("\n").append("Code: ").append(request.getCode())
			.append("\n").append("Source: ").append(request.getSource()).append(":").append(request.getSourcePort())
			.append("\n").append("Type: ").append(request.getType())
			.append("\n").append("MID: ").append(request.getMID())
			.append("\n").append("Token: ").append(request.getTokenString())
			.append("\n").append(request.getOptions());
		
		exchange.respond(buffer.toString());
	}
}

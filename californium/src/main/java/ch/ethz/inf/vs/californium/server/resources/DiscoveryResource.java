package ch.ethz.inf.vs.californium.server.resources;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;

public class DiscoveryResource extends ResourceBase {

	public static final String CORE = "core";
	
	private final Resource root;
	
	public DiscoveryResource(Resource root) {
		super(CORE);
		this.root = root;
	}
	
	@Override
	public void processGET(Exchange exchange) {
		String tree = discoverTree(root, exchange.getRequest().getOptions().getURIQueries());
		Response response = new Response(ResponseCode.CONTENT);
		response.setPayload(tree);
		response.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
		exchange.respond(response);
	}
	
	public String discoverTree(Resource root, List<String> queries) {
		StringBuilder buffer = new StringBuilder();
		for (Resource child:root.getChildren()) {
			LinkFormat.serializeTree(child, queries, buffer);
		}
		
		// remove last comma ',' of the buffer
		if (buffer.length()>1)
			buffer.delete(buffer.length()-1, buffer.length());
		
		return buffer.toString();
	}
}

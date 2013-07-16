package ch.inf.vs.californium.resources;

import java.util.List;

import ch.inf.vs.californium.coap.LinkFormat;
import ch.inf.vs.californium.network.Exchange;

public class DiscoveryResource extends ResourceBase {

	public static final String CORE = "core";
	
	private final Resource root;
	
	public DiscoveryResource(Resource root) {
		super(CORE);
		this.root = root;
	}
	
	@Override
	public void processGET(Exchange exchange) {
		exchange.accept();
		
		String tree = discoverTree(root, exchange.getRequest().getOptions().getURIQueries());
		exchange.respond(tree);
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

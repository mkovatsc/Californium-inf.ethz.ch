package ch.inf.vs.californium.resources;

import java.util.LinkedList;
import java.util.List;

import ch.inf.vs.californium.coap.LinkFormat;
import ch.inf.vs.californium.network.Exchange;

public class DiscoveryResource extends ResourceBase {

	// TODO: We might improve this with some caching of the resource tree
	
	public static final String CORE = "core";
	
	private final Resource root;
	
	public DiscoveryResource(Resource root) {
		super(CORE);
		this.root = root;
	}
	
	@Override
	public void processGET(Exchange exchange) {
		exchange.accept();
//		String test = "</abc/def>,</huhu>,</rd>,</stats>;title=\"Keeps track of the requests served by the proxy.\",</stats/cache>,</stats/proxy>";
		
		String tree = discoverTree(root, exchange.getRequest().getOptions().getURIQueries());
		exchange.respond(tree);
	}
	
	public String discoverTree(Resource root, List<String> queries) {
		StringBuffer buffer = new StringBuffer();
		StringBuffer path = new StringBuffer("/");
		for (Resource child:root.getChildren()) {
			path.delete(1, path.length());
			discoverTree(child, path, queries, buffer);
		}
		// remove last comma ',' of the buffer
		if (buffer.length()>1)
			buffer.delete(buffer.length()-1, buffer.length());
		
		return buffer.toString();
	}
	
	public void discoverTree(Resource current, StringBuffer path, List<String> queries, StringBuffer buffer) {
		String name = current.getName();
		
		// add the current resource to the buffer
		if (current.isVisible()
				&& LinkFormat.matches(current, queries)) {
			String attrs = LinkFormat.serializeAttributes(current);
			buffer.append("<").append(path).append(name).append(">")
				.append(attrs).append(",");
		}
		
		// create the path for children
		int originalLength = path.length();
		path.append(name).append("/");
		for (Resource child:current.getChildren()) {
			discoverTree(child, path, queries, buffer);
		}
		path.delete(originalLength, path.length()); // cut the path back
	}
}

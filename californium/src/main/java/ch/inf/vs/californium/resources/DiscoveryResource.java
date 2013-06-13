package ch.inf.vs.californium.resources;

import ch.inf.vs.californium.network.Exchange;

public class DiscoveryResource extends ResourceBase {

	// TODO: We might improve this with some caching of the resource tree
	// TODO: filters
	
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
		
		StringBuffer buffer = new StringBuffer();
		StringBuffer path = new StringBuffer("/");
		for (Resource child:root.getChildren()) {
			path.delete(1, path.length());
			createTree(buffer, path, child);
		}
		// remove last comma ',' of the buffer
		if (buffer.length()>1)
			buffer.delete(buffer.length()-1, buffer.length());
		
		System.out.println("created path "+buffer.toString());
		
		exchange.respond(buffer.toString());
	}
	
	private void createTree(StringBuffer buffer, StringBuffer path, Resource current) {
		if (current.isHidden()) return;

		// add the current resource to the buffer
		String name = current.getName();
		buffer.append("<").append(path).append(name).append(">,");
		
		// create the path for children
		int originalLength = path.length();
		path.append(name).append("/");
		for (Resource child:current.getChildren()) {
			createTree(buffer, path, child);
		}
		path.delete(originalLength, path.length()); // cut the path back
	}
}

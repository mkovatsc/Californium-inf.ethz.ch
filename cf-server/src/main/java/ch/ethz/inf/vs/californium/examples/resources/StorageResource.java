package ch.ethz.inf.vs.californium.examples.resources;

import java.util.Arrays;
import java.util.LinkedList;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource allows to store an arbitrary payload in any subresource. If the
 * target subresource does not yet exist it will be created. Therefore, such a
 * resource can be though off as having all possible children.
 * <p>
 * <ul>
 * <li />A GET request receives the currently stored data within the target
 * resource.
 * <li />A POST request creates the specified resources from the payload.
 * <li />A PUT request stores the payload within the target resource.
 * <li />A DELETE request deletes the target resource.
 * </ul>
 * <p>
 * Assume a single instance of this resource called "storage". Assume a client
 * sends a PUT request with Payload "foo" to the URI storage/A/B/C. When the
 * resource storage receives the request, it creates the resources A, B and C
 * and delivers the request to the resource C. Resource C will process the PUT
 * request and stare "foo". If the client sends a consecutive GET request to the
 * URI storage/A/B/C, resource C will respond with the payload "foo".
 * 
 * @author Martin Lanter
 */
public class StorageResource extends ResourceBase {

	private String content;
	
	public StorageResource(String name) {
		super(name);
	}
	
	@Override
	public void handleGET(Exchange exchange) {
		if (content != null) {
			exchange.respond(content);
		} else {
			String subtree = LinkFormat.serializeTree(this);
			Response response = new Response(ResponseCode.CONTENT);
			response.setPayload(subtree);
			response.getOptions().setContentFormat(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
			exchange.respond(response);
		}
	}

	@Override
	public void handlePOST(Exchange exchange) {
		Request request = exchange.getRequest();
		String payload = request.getPayloadString();
		String[] parts = payload.split("\\?");
		String[] path = parts[0].split("/");
		Resource resource = create(new LinkedList<String>(Arrays.asList(path)));
		
		Response response = new Response(ResponseCode.CREATED);
		response.getOptions().setLocationPath(resource.getURI());
		exchange.respond(response);
	}

	@Override
	public void handlePUT(Exchange exchange) {
		content = exchange.getRequest().getPayloadString();
		exchange.respond(new Response(ResponseCode.CHANGED));
	}

	@Override
	public void handleDELETE(Exchange exchange) {
		this.delete();
		exchange.respond(new Response(ResponseCode.DELETED));
	}

	/**
	 * Find the requested child. If the child does not exist yet, create it.
	 */
	@Override
	public Resource getChild(String name) {
		Resource resource = super.getChild(name);
		if (resource == null) {
			resource = new StorageResource(name);
			add(resource);
		}
		return resource;
	}
	
	/**
	 * Create a resource hierarchy with according to the specified path.
	 * @param path the path
	 * @return the lowest resource from the hierarchy
	 */
	private Resource create(LinkedList<String> path) {
		String segment;
		do {
			if (path.size() == 0)
				return this;
		
			segment = path.removeFirst();
		} while (segment.isEmpty() || segment.equals("/"));
		
		StorageResource resource = new StorageResource(segment);
		add(resource);
		return resource.create(path);
	}

}

package ch.inf.vs.californium.example;

import java.util.Arrays;
import java.util.LinkedList;

import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.LinkFormat;
import ch.inf.vs.californium.coap.MediaTypeRegistry;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.Resource;
import ch.inf.vs.californium.resources.ResourceBase;

/**
 * @author Dominique Im Obersteg & Daniel Pauli
 */
public class StorageResource extends ResourceBase {

	private String content;
	
	public StorageResource(String name) {
		super(name);
	}
	
	@Override
	public void processGET(Exchange exchange) {
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
	public void processPOST(Exchange exchange) {
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
	public void processPUT(Exchange exchange) {
		content = exchange.getRequest().getPayloadString();
		exchange.respond(new Response(ResponseCode.CHANGED));
	}

	@Override
	public void processDELETE(Exchange exchange) {
		this.delete();
		exchange.respond(new Response(ResponseCode.DELETED));
	}
	
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

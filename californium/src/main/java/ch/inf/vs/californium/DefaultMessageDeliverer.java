package ch.inf.vs.californium;

import java.util.LinkedList;
import java.util.List;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.Resource;

public class DefaultMessageDeliverer implements MessageDeliverer {

	private final Resource root;
	
	public DefaultMessageDeliverer(Resource root) {
		this.root = root;
	}
	
	@Override
	public void deliverRequest(Exchange exchange) {
		Request request = exchange.getRequest();
		Resource resource = findResource(request.getOptions().getURIPaths());
		if (resource != null) {
			switch (request.getCode()) {
				case GET: resource.processGET(exchange); break;
				case POST: resource.processPOST(exchange); break;
				case PUT: resource.processPUT(exchange); break;
				case DELETE: resource.processDELETE(exchange); break;
			}
		} else {
			exchange.respond(new Response(ResponseCode.NOT_FOUND));
		}
	}
	
	private Resource findResource(List<String> list) {
		LinkedList<String> path = new LinkedList<>(list);
		Resource current = root;
		while (!path.isEmpty()) {
			String name = path.removeFirst();
			Resource next = current.getChild(name);
			if (next == null) {
				if (current.isAcceptRequestForChild())
					return current;
			} else {
				current = next;
			}
		}
		return current;
	}

	@Override
	public void deliverResponse(Exchange exchange, Response response) {
		if (exchange == null)
			throw new NullPointerException();
		if (exchange.getRequest() == null)
			throw new NullPointerException();
		if (response == null)
			throw new NullPointerException();
		exchange.getRequest().setResponse(response);
	}

}

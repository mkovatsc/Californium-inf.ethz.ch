package ch.inf.vs.californium;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.observe.ObserveRelation;
import ch.inf.vs.californium.resources.AbstractResource;
import ch.inf.vs.californium.resources.Resource;

public class DefaultMessageDeliverer implements MessageDeliverer {

	/** The logger. */
	private final static Logger LOGGER = Logger.getLogger(DefaultMessageDeliverer.class.getName());
	
	private final Resource root;
	
	public DefaultMessageDeliverer(Resource root) {
		this.root = root;
	}
	
	@Override
	public void deliverRequest(Exchange exchange) {
		Request request = exchange.getRequest();
		Resource resource = findResource(request.getOptions().getURIPaths());
		if (resource != null) {
			checkForObserveOption(exchange, resource);
			resource.processRequest(exchange);
		} else {
			exchange.respond(new Response(ResponseCode.NOT_FOUND));
		}
	}
	
	private void checkForObserveOption(Exchange exchange, Resource resource) {
		Request request = exchange.getRequest();
		if (request.getCode() != Code.GET)
			return;
		
		if (request.getOptions().hasObserve()) {
			LOGGER.info(" Request has observe option");
			if (resource.isObservable()) {
				// Requests wants to observe and resource allows it :-)
				ObserveRelation relation = findObserveRelation(exchange);
				exchange.setObserveRelation(relation);
				resource.addObserveRelation(relation);
				relation.addResource(resource);
			} 
			/*
			 * else, request wants to observe but resource has no use for it.
			 * The only consequence to that is that the response will not
			 * contain an observe option.
			 */
			
		} else {
			LOGGER.info(" Request has no observe option");
			// There is no observe option. Therefore, we have to remove it from
			// the resource (if it is actually there).
			ObserveRelation relation = getObserveRelation(exchange);
			if (relation != null) {
				// Indeed, we need to remove it
				resource.removeObserveRelation(relation);
				relation.removeResource(resource);
			}
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
	
	private ObserveRelation findObserveRelation(Exchange exchange) {
		// TODO
		return new ObserveRelation(exchange);
	}
	
	private ObserveRelation getObserveRelation(Exchange exchange) {
		throw new NullPointerException();
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

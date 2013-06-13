package ch.inf.vs.californium;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.observe.ObserveManager;
import ch.inf.vs.californium.observe.ObserveRelation;
import ch.inf.vs.californium.observe.ObservingEndpoint;
import ch.inf.vs.californium.resources.CalifonriumLogger;
import ch.inf.vs.californium.resources.Resource;

public class DefaultMessageDeliverer implements MessageDeliverer {

	/** The logger. */
	private final static Logger LOGGER = CalifonriumLogger.getLogger(DefaultMessageDeliverer.class);
	
	private final Resource root;

	private ObserveManager observeManager = new ObserveManager();
	
	public DefaultMessageDeliverer(Resource root) {
		this.root = root;
	}
	
	@Override
	public void deliverRequest(Exchange exchange) {
		Request request = exchange.getRequest();
		List<String> path = request.getOptions().getURIPaths();
		Resource resource = findResource(path);
		if (resource != root) {
			LOGGER.info("Found resource "+resource.getName()+" for path "+path.toString());
			checkForObserveOption(exchange, resource, path);
			resource.processRequest(exchange);
		} else {
			LOGGER.info("Did not find resource "+path.toString());
			exchange.respond(new Response(ResponseCode.NOT_FOUND));
		}
	}
	
	private void checkForObserveOption(Exchange exchange, Resource resource, List<String> path) {
		// path might be a wildcard. /a/b/c might be the same resource as /a/b/xy
		Request request = exchange.getRequest();
		if (request.getCode() != Code.GET)
			return;

		EndpointAddress source = new EndpointAddress(request.getSource(), request.getSourcePort());
		
		if (request.getOptions().hasObserve()) {
			if (resource.isObservable()) {
				// Requests wants to observe and resource allows it :-)
				LOGGER.info("establish observe relation between "+request.getSource()+":"+request.getSourcePort()+" and resource "+resource.getPath());
				ObservingEndpoint endpoint = observeManager.findObservingEndpoint(source);
				ObserveRelation relation = endpoint.findObserveRelation(path, resource);
				relation.setExchange(exchange);
				exchange.setRelation(relation);
				exchange.setObserveOrderer(relation.getOrderr());
				
				resource.addObserveRelation(relation);
			} 
			/*
			 * else, request wants to observe but resource has no use for it.
			 * The only consequence to that is that the response will not
			 * contain an observe option.
			 */
			
		} else {
			// There is no observe option. Therefore, we have to remove it from
			// the resource (if it is actually there).
			ObservingEndpoint endpoint = observeManager.getObservingEndpoint(source);
			if (endpoint == null) return; // because no relation can exist
			ObserveRelation relation = endpoint.getObserveRelation(path);
			if (relation == null) return; // because no relation can exist
			// Otherwise, we need to remove it
			relation.cancel();
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

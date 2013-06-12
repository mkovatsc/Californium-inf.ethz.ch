package ch.inf.vs.californium.observe;

import java.util.List;

import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.Resource;

public class ObserveRelation {

	private final ObserveNotificationOrderer orderr = new ObserveNotificationOrderer();
	
	private final ObservingEndpoint endpoint;
	
	private final Resource resource;
	private final List<String> path;
	
	private Exchange exchange;
	
	/**
	 * multiple mats may lead to the same resource
	 * @param resource
	 * @param path
	 */
	public ObserveRelation(ObservingEndpoint endpoint, Resource resource, List<String> path) {
		if (endpoint == null)
			throw new NullPointerException();
		if (resource == null)
			throw new NullPointerException();
		if (path == null)
			throw new NullPointerException();
		this.endpoint = endpoint;
		this.resource = resource;
		this.path = path;
	}
	
	public void cancel() {
		resource.removeObserveRelation(this);
		endpoint.removeObserveRelation(this);
	}
	
	public void notifyObservers() {
		resource.processRequest(exchange);
	}
	
	public Resource getResource() {
		return resource;
	}

	public List<String> getPath() {
		return path;
	}

	public ObserveNotificationOrderer getOrderr() {
		return orderr;
	}

	public Exchange getExchange() {
		return exchange;
	}

	public void setExchange(Exchange exchange) {
		this.exchange = exchange;
	}
}

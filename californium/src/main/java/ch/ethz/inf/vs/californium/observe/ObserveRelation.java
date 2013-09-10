package ch.ethz.inf.vs.californium.observe;

import java.util.List;

import ch.ethz.inf.vs.californium.network.EndpointAddress;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.resources.Resource;

public class ObserveRelation {

	// TODO: Only accept observe if response successful
	
	private final ObserveNotificationOrderer orderr = new ObserveNotificationOrderer();
	
	private final ObservingEndpoint endpoint;
	
	private final Resource resource;
	private final List<String> path;
	
	private Exchange exchange;
	
	private boolean established;
	
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
		this.established = false;
	}
	
	public boolean isEstablished() {
		return established;
	}
	
	public void setEstablished(boolean established) {
		this.established = established;
	}
	
	public void cancel() {
		this.established = false;
		resource.removeObserveRelation(this);
		endpoint.removeObserveRelation(this);
	}
	
	public void cancelAll() {
		endpoint.cancelAll();
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

	public ObserveNotificationOrderer getOrderer() {
		return orderr;
	}

	public Exchange getExchange() {
		return exchange;
	}

	public void setExchange(Exchange exchange) {
		this.exchange = exchange;
	}
	
	public EndpointAddress getSource() {
		return endpoint.getAddress();
	}
}

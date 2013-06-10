package ch.inf.vs.californium.observe;

import java.util.List;
import java.util.logging.Logger;

import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.Resource;

public class ObserveRelation {

	private final static Logger LOGGER = Logger.getLogger(ObserveRelation.class.getName());

	private final ObserveNotificationOrderer orderr = new ObserveNotificationOrderer();
	
	private final Resource resource;
	private final List<String> path;
	
	private Exchange exchange;
	
	/**
	 * multiple mats may lead to the same resource
	 * @param resource
	 * @param path
	 */
	public ObserveRelation(Resource resource, List<String> path) {
		if (resource == null)
			throw new NullPointerException();
		if (path == null)
			throw new NullPointerException();
		this.resource = resource;
		this.path = path;
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
	
//	@Override // TODO
//	public int hashCode() {
//		return exchange.getRequest().getSourcePort();
//	}
//	
//	@Override // TODO
//	public boolean equals(Object o) {
//		if (! (o instanceof ObserveRelation))
//			return false;
//		ObserveRelation relation = (ObserveRelation) o;
//		return exchange.getRequest().getSourcePort() ==
//				relation.exchange.getRequest().getSourcePort();
//	}
}

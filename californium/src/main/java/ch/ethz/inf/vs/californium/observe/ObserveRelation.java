package ch.ethz.inf.vs.californium.observe;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;

public class ObserveRelation {

	private static final Logger LOGGER = CalifonriumLogger.getLogger(ObserveRelation.class);

	private final ObserveNotificationOrderer orderr = new ObserveNotificationOrderer();
	
	private final ObservingEndpoint endpoint;
	private final Resource resource;
	private final Exchange exchange;
	
	private boolean established;
	
	/**
	 * multiple mats may lead to the same resource
	 * @param resource
	 */
	public ObserveRelation(ObservingEndpoint endpoint, Resource resource, Exchange exchange) {
		if (endpoint == null)
			throw new NullPointerException();
		if (resource == null)
			throw new NullPointerException();
		if (exchange == null)
			throw new NullPointerException();
		this.endpoint = endpoint;
		this.resource = resource;
		this.exchange = exchange;
		this.established = false;
	}
	
	public boolean isEstablished() {
		return established;
	}
	
	public void setEstablished(boolean established) {
		this.established = established;
	}
	
	public void cancel() {
		LOGGER.info("Cancel observe relation from "+endpoint.getAddress()+" with "+resource.getURI());
		this.established = false;
		resource.removeObserveRelation(this);
		endpoint.removeObserveRelation(this);
	}
	
	public void cancelAll() {
		endpoint.cancelAll();
	}
	
	public void notifyObservers() {
		resource.handleRequest(exchange);
	}
	
	public Resource getResource() {
		return resource;
	}

	public ObserveNotificationOrderer getOrderer() {
		return orderr;
	}

	public Exchange getExchange() {
		return exchange;
	}

	public InetSocketAddress getSource() {
		return endpoint.getAddress();
	}
}

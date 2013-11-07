package ch.ethz.inf.vs.californium.observe;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.resources.Resource;

public class ObserveRelation {

	private static final Logger LOGGER = CalifonriumLogger.getLogger(ObserveRelation.class);

	private final ObserveNotificationOrderer orderer = new ObserveNotificationOrderer();
	
	private final long CHECK_INTERVAL_TIME = NetworkConfig.getStandard().getLong(NetworkConfigDefaults.NOTIFICATION_CHECK_INTERVAL_TIME);
	private final int CHECK_INTERVAL_COUNT = NetworkConfig.getStandard().getInt(NetworkConfigDefaults.NOTIFICATION_CHECK_INTERVAL_COUNT);
	
	private final ObservingEndpoint endpoint;
	private final Resource resource;
	private final Exchange exchange;
	
	private boolean established;
	
	private long interestCheckTimer = System.currentTimeMillis();
	private int interestCheckCounter = 1;
	
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
		return orderer;
	}

	public Exchange getExchange() {
		return exchange;
	}

	public InetSocketAddress getSource() {
		return endpoint.getAddress();
	}

	public boolean check() {
		return (interestCheckTimer + CHECK_INTERVAL_TIME < System.currentTimeMillis())
				|| (++interestCheckCounter >= CHECK_INTERVAL_COUNT);
	}

	public void resetCheck() {
		this.interestCheckTimer = System.currentTimeMillis();
		this.interestCheckCounter = 0;
	}
}

package ch.ethz.inf.vs.californium.observe;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.resources.Resource;

/**
 * The ObserveRelation represents a relation between a client endpoint and a
 * resource on this server.
 */
public class ObserveRelation {

	/** The logger. */
	private final static Logger LOGGER = Logger.getLogger(ObserveRelation.class.getCanonicalName());

	private final ObserveNotificationOrderer orderer = new ObserveNotificationOrderer();
	
	private final long CHECK_INTERVAL_TIME = NetworkConfig.getStandard().getLong(NetworkConfigDefaults.NOTIFICATION_CHECK_INTERVAL_TIME);
	private final int CHECK_INTERVAL_COUNT = NetworkConfig.getStandard().getInt(NetworkConfigDefaults.NOTIFICATION_CHECK_INTERVAL_COUNT);
	
	private final ObservingEndpoint endpoint;

	/** The resource that is observed */
	private final Resource resource;
	
	/** The exchange that has established the observe relationship */
	private final Exchange exchange;
	
	private Response recentControlNotification;
	private Response nextControlNotification;

	/*
	 * This value is false at first and must be set to true by the resource if
	 * it accepts the observe relation (the response code must be successful).
	 */
	/** Indicates if the relation is established */
	private boolean established;
	
	private long interestCheckTimer = System.currentTimeMillis();
	private int interestCheckCounter = 1;
	
	/**
	 * Constructs a new observe relation.
	 * 
	 * @param endpoint the observing endpoint
	 * @param resource the observed resource
	 * @param exchange the exchange that tries to establish the observe relation
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
	
	/**
	 * Returns true if this relation has been established.
	 * @return true if this relation has been established
	 */
	public boolean isEstablished() {
		return established;
	}
	
	/**
	 * Sets the established field.
	 *
	 * @param established true if the relation has been established
	 */
	public void setEstablished(boolean established) {
		this.established = established;
	}
	
	/**
	 * Cancel this observe relation. This methods invokes the cancel methods of
	 * the resource and the endpoint.
	 */
	public void cancel() {
		LOGGER.info("Cancel observe relation from "+endpoint.getAddress()+" with "+resource.getURI());
		this.established = false;
		resource.removeObserveRelation(this);
		endpoint.removeObserveRelation(this);
	}
	
	/**
	 * Cancel all observer relations that this server has established with this'
	 * realtion's endpoint.
	 */
	public void cancelAll() {
		endpoint.cancelAll();
	}
	
	/**
	 * Notifies the observing endpoint that the resource has been changed. This
	 * method makes the resource process the same request again.
	 */
	public void notifyObservers() {
		resource.handleRequest(exchange);
	}
	
	/**
	 * Gets the resource.
	 *
	 * @return the resource
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * Gets the exchange.
	 *
	 * @return the exchange
	 */
	public ObserveNotificationOrderer getOrderer() {
		return orderer;
	}

	public Exchange getExchange() {
		return exchange;
	}

	/**
	 * Gets the source address of the observing endpoint.
	 *
	 * @return the source address
	 */
	public InetSocketAddress getSource() {
		return endpoint.getAddress();
	}

	public boolean check() {
		boolean check = false;
		check |= this.interestCheckTimer + CHECK_INTERVAL_TIME < System.currentTimeMillis();
		check |= (++interestCheckCounter >= CHECK_INTERVAL_COUNT);
		if (check) {
			this.interestCheckTimer = System.currentTimeMillis();
			this.interestCheckCounter = 0;
		}
		return check;
	}

	public Response getCurrentControlNotification() {
		return recentControlNotification;
	}

	public void setCurrentControlNotification(Response recentControlNotification) {
		this.recentControlNotification = recentControlNotification;
	}

	public Response getNextControlNotification() {
		return nextControlNotification;
	}

	public void setNextControlNotification(Response nextControlNotification) {
		this.nextControlNotification = nextControlNotification;
	}
}

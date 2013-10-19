package ch.ethz.inf.vs.californium.observe;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The observe manager holds a mapping of endpoint addresses to
 * {@link ObservingEndpoint}s. It makes sure that there be only one
 * ObservingEndpoint that represents the observe relations from one endpoint to
 * this server. This important in case we want to cancel all relations to a
 * specific endpoint, e.g., when a confirmable notification timeouts.
 * <p>
 * Notice that each server has its own ObserveManager. If a server binds to
 * multiple endpoints, the ObserveManager keeps the observe relations for all of
 * them.
 */
//TODO: find a better name... how about ObserveObserver -.-
public class ObserveManager {

	/** The mapping from endpoint addresses to ObservingEndpoints */
	private final ConcurrentHashMap<InetSocketAddress, ObservingEndpoint> endpoints;
	
	/**
	 * Constructs a new ObserveManager for this server.
	 */
	public ObserveManager() {
		endpoints = new ConcurrentHashMap<InetSocketAddress, ObservingEndpoint>();
	}
	
	/**
	 * Find the ObservingEndpoint for the specified endpoint address or create
	 * a new one if none exists yet. Does not return null.
	 * 
	 * @param address the address
	 * @return the ObservingEndpoint for the address
	 */
	public ObservingEndpoint findObservingEndpoint(InetSocketAddress address) {
		ObservingEndpoint ep = endpoints.get(address);
		if (ep == null)
			ep = createObservingEndpoint(address);
		return ep;
	}
	
	/**
	 * Return the ObservingEndpoint for the specified endpoint address or null
	 * if none exists.
	 * 
	 * @param address the address
	 * @return the ObservingEndpoint or null
	 */
	public ObservingEndpoint getObservingEndpoint(InetSocketAddress address) {
		return endpoints.get(address);
	}
	
	/**
	 * Atomically creates a new ObservingEndpoint for the specified address.
	 * 
	 * @param address the address
	 * @return the ObservingEndpoint
	 */
	private ObservingEndpoint createObservingEndpoint(InetSocketAddress address) {
		ObservingEndpoint ep = new ObservingEndpoint(address);
		
		// Make sure, there is exactly one ep with the specified address (atomic creation)
		ObservingEndpoint previous = endpoints.putIfAbsent(address, ep);
		if (previous != null) {
			return previous; // and forget ep again
		} else {
			return ep;
		}
	}
	
}


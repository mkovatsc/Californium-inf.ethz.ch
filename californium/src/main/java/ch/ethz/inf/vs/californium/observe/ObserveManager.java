package ch.ethz.inf.vs.californium.observe;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;


// TODO: find a better name... how about ObserveObserver -.-
public class ObserveManager {

	private final ConcurrentHashMap<InetSocketAddress, ObservingEndpoint> endpoints;
	
	public ObserveManager() {
		endpoints = new ConcurrentHashMap<InetSocketAddress, ObservingEndpoint>();
	}
	
	public ObservingEndpoint findObservingEndpoint(InetSocketAddress address) {
		ObservingEndpoint ep = endpoints.get(address);
		if (ep == null)
			ep = createObservingEndpoint(address);
		return ep;
	}
	
	public ObservingEndpoint getObservingEndpoint(InetSocketAddress address) {
		return endpoints.get(address);
	}
	
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


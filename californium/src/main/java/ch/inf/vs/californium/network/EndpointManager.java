package ch.inf.vs.californium.network;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import ch.inf.vs.californium.MessageDeliverer;
import ch.inf.vs.californium.coap.Response;

public class EndpointManager {

	private final static Logger LOGGER = Logger.getLogger(EndpointManager.class.getName());
	
	public static final int default_port = 5683;
	
	private static EndpointManager manager = new EndpointManager();
	
	public static EndpointManager getEndpointManager() {
		return manager;
	}
	
	private Endpoint default_endpoint;
	
	private ConcurrentHashMap<EndpointAddress, Endpoint> endpoints = new ConcurrentHashMap<>();
	
	public Endpoint getDefaultEndpoint() {
		if (default_endpoint == null) {
			final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			default_endpoint = new Endpoint(default_port);
			default_endpoint.setMessageDeliverer(new DefaultMessageDeliverer());
			default_endpoint.setExecutor(executor);
			default_endpoint.addObserver(new EndpointObserver() {
				public void started(Endpoint endpoint) { }
				public void stopped(Endpoint endpoint) { }
				public void destroyed(Endpoint endpoint) {
					executor.shutdown(); // TODO: should this be done in stopped; how to start again?
				}
			});
			default_endpoint.start();
			LOGGER.info("--- Created and started default endpoint on port "+default_port+" ---");
		}
		return default_endpoint;
	}

	/*
	 * Endpoint register themselves after start
	 */
	public void registerEndpoint(Endpoint endpoint) {
		assert(endpoint.getAddress() != null);
		assert(endpoint.getAddress().getInetAddress() != null);
		assert(endpoint.getAddress().getPort() != 0);
		endpoints.put(endpoint.getAddress(), endpoint);
	}
	
	public void unregisterEndpoint(Endpoint endpoint) {
		assert(endpoint.getAddress() != null);
		assert(endpoint.getAddress().getInetAddress() != null);
		assert(endpoint.getAddress().getPort() != 0);
		endpoints.remove(endpoint.getAddress());
	}
	
	public Endpoint getEndpointByAddress(EndpointAddress address) {
		return endpoints.get(address);
	}
	
	public Collection<Endpoint> getEndpoints() {
		return endpoints.values();
	}
	
	private static class DefaultMessageDeliverer implements MessageDeliverer {
		
		@Override
		public void deliverRequest(Exchange exchange) {
			LOGGER.severe("Default endpoint has received a request. What should happen now?");
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
}

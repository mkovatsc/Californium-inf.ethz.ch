package ch.inf.vs.californium.network;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.MessageDeliverer;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.connector.Connector;
import ch.inf.vs.californium.network.connector.DTLSConnector;

public class EndpointManager {

	private final static Logger LOGGER = Logger.getLogger(EndpointManager.class.getName());

	public static final int DEFAULT_PORT = 5683;
	public static final int DEFAULTDLTS_PORT = 5684; // To be defined by draft
	
	private static EndpointManager manager = new EndpointManager();
	
	public static EndpointManager getEndpointManager() {
		return manager;
	}
	
	private Endpoint default_endpoint;
	
	private Endpoint default_dtls_endpoint;
	
	private ConcurrentHashMap<EndpointAddress, Endpoint> endpoints = new ConcurrentHashMap<>();
	
	public Endpoint getDefaultEndpoint() {
		try {
			if (default_endpoint == null) {
				final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
				default_endpoint = new Endpoint(DEFAULT_PORT);
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
				LOGGER.info("--- Created and started default endpoint on port "+DEFAULT_PORT+" ---");
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while getting the default endpoint", e);
		}
		return default_endpoint;
	}
	
	public Endpoint getDefaultDtlsEndpoint() {
		if (default_dtls_endpoint == null) {
			final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			EndpointAddress address = new EndpointAddress(null, DEFAULTDLTS_PORT);
			Connector dtlsConnector = new DTLSConnector(address);
			default_dtls_endpoint = new Endpoint(dtlsConnector, address, new NetworkConfig());
			default_dtls_endpoint.setExecutor(executor);
			default_dtls_endpoint.addObserver(new EndpointObserver() {
				public void started(Endpoint endpoint) { }
				public void stopped(Endpoint endpoint) { }
				public void destroyed(Endpoint endpoint) {
					executor.shutdown(); // TODO: should this be done in stopped; how to start again?
				}
			});
			default_dtls_endpoint.start();
			LOGGER.info("--- Created and started default DTLS endpoint on port "+DEFAULTDLTS_PORT+" ---");
		}
		return default_dtls_endpoint;
	}

	/*
	 * Endpoint register themselves after start
	 */
	public void registerEndpoint(Endpoint endpoint) {
		assert(endpoint.getAddress() != null);
		assert(endpoint.getAddress().getAddress() != null);
		assert(endpoint.getAddress().getPort() != 0);
		endpoints.put(endpoint.getAddress(), endpoint);
	}
	
	public void unregisterEndpoint(Endpoint endpoint) {
		assert(endpoint.getAddress() != null);
		assert(endpoint.getAddress().getAddress() != null);
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
			LOGGER.info("Default deliverer: Request: "+exchange.getRequest()+"\n"+"	 Response: "+response);
			exchange.getRequest().setResponse(response);
		}
		
	}
}

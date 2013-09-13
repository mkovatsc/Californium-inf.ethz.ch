package ch.ethz.inf.vs.californium.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.server.MessageDeliverer;
import ch.ethz.inf.vs.californium.server.Server;

/**
 * The class EndpointManager manages Endpoints. Its exact role has yet to be
 * defined.
 * <p>
 * The EndpointManager contains the default endpoint for coap (on port 5683) and
 * coaps (CoAP over DTLS). When an application serves only as client but not
 * server it can just use the default endpoint to send requests. When the
 * application sends a request by calling {@link Request#send()} the send method
 * sends itself over the default endpoint.
 * <p>
 * To make a server listen for requests on the default endpoint, call
 * <pre>{@code
 *  Server server = new Server(EndpointManager.DEFAULT_PORT);
 * }</pre>
 * or more explicit
 * <pre>{@code
 *  Endpoint endpoint = EndpointManager.getEndpointManager().getDefaultEndpoint();
 *  Server server = new Server();
 *  server.addEndpoint(endpoint);
 * }</pre>
 */
public class EndpointManager {
	
	/** The logger */
	private final static Logger LOGGER = CalifonriumLogger.getLogger(EndpointManager.class);

	/** The default CoAP port for normal CoAP communication (not secure) */
	public static final int DEFAULT_COAP_PORT = 5683;
	
	/** The default CoAP port for secure CoAP communication (coaps) */
	/* Will be chosen by the system and will be different between different runs of the program*/
	public static final int DEFAULT_COAP_SECURE_PORT = 0; // To be defined by draft
	
	/** The singleton manager instance */
	private static EndpointManager manager = new EndpointManager();
	
	/**
	 * Gets the singleton manager.
	 *
	 * @return the endpoint manager
	 */
	public static EndpointManager getEndpointManager() {
		return manager;
	}
	
	/** The default endpoint for CoAP (port 5683) */
	private Endpoint default_endpoint;
	
	/** The default endpoint for secure CoAP (port TBD)*/
	private Endpoint default_dtls_endpoint;
	
	// TODO Role not yet defined
	private ConcurrentHashMap<EndpointAddress, Endpoint> endpoints = new ConcurrentHashMap<EndpointAddress, Endpoint>();
	
	
	/**
	 * Gets the default endpoint (listening on port 5683). By default, the
	 * endpoint has a single-threaded executor and is started. It is possible to
	 * send requests over the endpoint and receive responses. It is not possible
	 * to receive requests by default. If a request arrives at the endpoint, the
	 * {@link ClientMessageDeliverer} rejects it. To receive requests, the
	 * endpoint must be added to an instance of {@link Server}. Be careful with
	 * stopping or destroying the default endpoint as it affects all messages
	 * that are supposed to be sent over it.
	 * 
	 * @return the default endpoint
	 */
	public Endpoint getDefaultEndpoint() {
		try {
			if (default_endpoint == null) {
				createDefaultEndpoint();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while getting the default endpoint", e);
		}
		return default_endpoint;
	}
	
	private synchronized void createDefaultEndpoint() throws UnknownHostException {
		if (default_endpoint != null) return;
		
		LOGGER.info("Create default endpoint");
		int threadCount = NetworkConfig.getStandard().getInt(
				NetworkConfigDefaults.DEFAULT_ENDPOINT_THREAD_COUNT);
		final ScheduledExecutorService executor = Executors.newScheduledThreadPool(threadCount);
		/*
		 * FIXME: With host=null, the default endpoint binds to 0.0.0.0. When
		 * sending it chooses to send over 192.168.1.37. A server that binds
		 * itself explicitly to .37 might send packets as well. If they both use
		 * the same MIDs they will interfere with each other.
		 * However, if we use host=getLocalHost(), the endpoint binds explicitly to
		 * 192.168.1.37. If we then try to send a packet to localhost, an exception
		 * raises. It seems that we cannot send a packet over .37 to localhost.
		 */
		InetAddress localhost = null; // or InetAddress.getLocalHost(); ?
		int port = 0;
		EndpointAddress address = new EndpointAddress(localhost, port);
		default_endpoint = new Endpoint(address);
		default_endpoint.setMessageDeliverer(new ClientMessageDeliverer());
		default_endpoint.setExecutor(executor);
		default_endpoint.addObserver(new EndpointObserver() {
			public void started(Endpoint endpoint) { }
			public void stopped(Endpoint endpoint) { }
			public void destroyed(Endpoint endpoint) {
				executor.shutdown();
			}
		});
		default_endpoint.start();
		// sleep(50) makes sure, that all log-entries have been written before the "---"
		try { Thread.sleep(50); } catch (Exception e) {} // TODO: remove
		LOGGER.info("--- Created and started default endpoint "+default_endpoint.getAddress()+" ---");
	}
	
	public List<Endpoint> getDefaultEndpointsFromAllInterfaces() {
		getDefaultEndpoint(); // ensure it exists
		return getEndpointsFromAllInterfaces(DEFAULT_COAP_PORT);
	}
	
	public List<Endpoint> getDefaultSecureEndpointsFromAllInterfaces() {
		getDefaultSecureEndpoint(); // ensure it exists
		return getEndpointsFromAllInterfaces(DEFAULT_COAP_SECURE_PORT);
	}
	
	public List<Endpoint> getEndpointsFromAllInterfaces(int port) {
		List<Endpoint> list = new LinkedList<Endpoint>();
		for (InetAddress ip:getNetworkInterfaces()) {
			EndpointAddress addr = new EndpointAddress(ip, port);
			// Check if there is already an endpoint, e.g. default ep
			Endpoint endpoint = endpoints.get(addr);
			if (endpoint == null)
				endpoint = new Endpoint(addr);
			list.add(endpoint);
		}
		return list;
	}
	
	/**
	 * Gets the default endpoint for coaps (listening on a system chosen port).
	 * By default, the endpoint has a single-threaded executor and is started.
	 * It is possible to send requests over the endpoint and receive responses.
	 * It is not possible to receive requests by default. If a request arrives
	 * at the endpoint, the {@link ClientMessageDeliverer} rejects it. To
	 * receive requests, the endpoint must be added to an instance of
	 * {@link Server}. Be careful with stopping or destroying the default
	 * endpoint as it affects all messages that are supposed to be sent over it.
	 * 
	 * @return the default endpoint
	 */
	public Endpoint getDefaultSecureEndpoint() {
		try {
			if (default_dtls_endpoint == null) {
				createDefaultSecureEndpoint();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception while getting the default secure endpoint", e);
		}
		return default_dtls_endpoint;
	}
	
	private synchronized void createDefaultSecureEndpoint() throws UnknownHostException {
		LOGGER.severe("Secure endpoint is not implemented yet. Return normal endpoint");
		// TODO with DTLS Connector
		createDefaultEndpoint();
	}

	/**
	 * Endpoint register themselves after start.
	 */
	public void registerEndpoint(Endpoint endpoint) {
		assert(endpoint.getAddress() != null);
		assert(endpoint.getAddress().getAddress() != null);
		assert(endpoint.getAddress().getPort() != 0);
		endpoints.put(endpoint.getAddress(), endpoint);
	}
	
	/**
	 * Endpoints unregister themselves after stop.
	 */
	public void unregisterEndpoint(Endpoint endpoint) {
		assert(endpoint.getAddress() != null);
		assert(endpoint.getAddress().getAddress() != null);
		assert(endpoint.getAddress().getPort() != 0);
		endpoints.remove(endpoint.getAddress());
	}
	
	/**
	 * Get the {@link Endpoint} for the specified address.
	 */
	public Endpoint getEndpointByAddress(EndpointAddress address) {
		return endpoints.get(address);
	}
	
	public Collection<InetAddress> getNetworkInterfaces() {
		Collection<InetAddress> interfaces = new LinkedList<InetAddress>();
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
	        for (NetworkInterface netint : Collections.list(nets)) {
	        	Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
	        	if (inetAddresses.hasMoreElements())
	        		interfaces.add(inetAddresses.nextElement());
	        }
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return interfaces;
	}
	
	/**
	 * Get all registered {@link Endpoint}.
	 */
	public Collection<Endpoint> getEndpoints() {
		return endpoints.values();
	}
	
	// Needed for JUnit Tests to remove state for deduplication
	/**
	 * Clear the state for deduplication in both default endpoints.
	 */
	public static void clear() {
		EndpointManager it = getEndpointManager();
		if (it.default_endpoint != null)
			it.default_endpoint.clear();
		if (it.default_dtls_endpoint != null)
			it.default_dtls_endpoint.clear();
	}
	
	/**
	 * ClientMessageDeliverer is a simple implementation of the interface
	 * {@link MessageDeliverer}. When a response arrives it adds it to the
	 * corresponding request. If requests arrive, however, the
	 * ClientMessageDeliverer rejects them.
	 */
	public static class ClientMessageDeliverer implements MessageDeliverer {
		
		/* (non-Javadoc)
		 * @see ch.inf.vs.californium.MessageDeliverer#deliverRequest(ch.inf.vs.californium.network.Exchange)
		 */
		@Override
		public void deliverRequest(Exchange exchange) {
			LOGGER.severe("Default endpoint has received a request. What should happen now?");
			exchange.reject();
		}
		
		/* (non-Javadoc)
		 * @see ch.inf.vs.californium.MessageDeliverer#deliverResponse(ch.inf.vs.californium.network.Exchange, ch.inf.vs.californium.coap.Response)
		 */
		@Override
		public void deliverResponse(Exchange exchange, Response response) {
			if (exchange == null) throw new NullPointerException();
			if (exchange.getRequest() == null) throw new NullPointerException();
			if (response == null) throw new NullPointerException();
			LOGGER.fine("Deliver response to request");
			exchange.getRequest().setResponse(response);
		}
	}
}

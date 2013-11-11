package ch.ethz.inf.vs.californium.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.Matcher;
import ch.ethz.inf.vs.californium.network.MessageIntercepter;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.network.layer.BlockwiseLayer;
import ch.ethz.inf.vs.californium.network.layer.ObserveLayer;
import ch.ethz.inf.vs.californium.network.layer.ReliabilityLayer;
import ch.ethz.inf.vs.californium.network.layer.TokenLayer;
import ch.ethz.inf.vs.californium.server.resources.DiscoveryResource;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;
import ch.ethz.inf.vs.elements.Connector;

/**
 * A server contains a resource structure and can listen to one or more
 * endpoints to handle requests. Resources of a server can send requests over
 * any endpoint the server is associated with.
 * <hr><blockquote><pre>
 * +--------------------------------------- Server ----------------------------------------+
 * |                                                                                       |
 * |                               +-----------------------+                               |
 * |                               | {@link MessageDeliverer}      +--> (Resource Tree)            |
 * |                               +---------A-A-A---------+                               |
 * |                                         | | |                                         |
 * |                                         | | |                                         |
 * |                 .-------->>>------------' | '--------<<<------------.                 |
 * |                /                          |                          \                |
 * |               |                           |                           |               |
 * |             * A                         * A                         * A               |
 * | +-{@link Endpoint}--+-A---------+   +-{@link Endpoint}--+-A---------+   +-{@link Endpoint}--+-A---------+     |
 * | |           v A         |   |           v A         |   |           v A         |     |
 * | |           v A         |   |           v A         |   |           v A         |     |
 * | | +---------v-+-------+ |   | +---------v-+-------+ |   | +---------v-+-------+ |     |
 * | | | Stack Top         | |   | | Stack Top         | |   | | Stack Top         | |     |
 * | | +-------------------+ |   | +-------------------+ |   | +-------------------+ |     |
 * | | | {@link TokenLayer}        | |   | | {@link TokenLayer}        | |   | | {@link TokenLayer}        | |     |
 * | | +-------------------+ |   | +-------------------+ |   | +-------------------+ |     |
 * | | | {@link ObserveLayer}      | |   | | {@link ObserveLayer}      | |   | | {@link ObserveLayer}      | |     |
 * | | +-------------------+ |   | +-------------------+ |   | +-------------------+ |     |
 * | | | {@link BlockwiseLayer}    | |   | | {@link BlockwiseLayer}    | |   | | {@link BlockwiseLayer}    | | ... |
 * | | +-------------------+ |   | +-------------------+ |   | +-------------------+ |     |
 * | | | {@link ReliabilityLayer}  | |   | | {@link ReliabilityLayer}  | |   | | {@link ReliabilityLayer}  | |     |
 * | | +-------------------+ |   | +-------------------+ |   | +-------------------+ |     |
 * | | | Stack Bottom      | |   | | Stack Bottom      | |   | | Stack Bottom      | |     |
 * | | +--------+-+--------+ |   | +--------+-+--------+ |   | +--------+-+--------+ |     |
 * | |          v A          |   |          v A          |   |          v A          |     |
 * | |          v A          |   |          v A          |   |          v A          |     |
 * | |        {@link Matcher}        |   |        {@link Matcher}        |   |        {@link Matcher}        |     |
 * | |          v A          |   |          v A          |   |          v A          |     |
 * | |      {@link MessageIntercepter Intercepter}      |   |      {@link MessageIntercepter Intercepter}      |   |      {@link MessageIntercepter Intercepter}      |     |
 * | |          v A          |   |          v A          |   |          v A          |     |
 * | |          v A          |   |          v A          |   |          v A          |     |
 * | | +--------v-+--------+ |   | +--------v-+--------+ |   | +--------v-+--------+ |     |
 * +-+-| {@link Connector}         |-+ - +-| {@link Connector}         |-+ - +-| {@link Connector}         |-+ ,,, +
 *     +--------+-A--------+       +--------+-A--------+       +--------+-A--------+   
 *              v A                         v A                         v A            
 *              v A                         v A                         v A         
 *           (Network)                   (Network)                   (Network) 
 *  </pre></blockquote><hr>
 * TODO: more description and an example
 **/
public class Server implements ServerInterface {

	private final static Logger LOGGER = CalifonriumLogger.getLogger(Server.class);

	private final Resource root;
	
	private MessageDeliverer deliverer;
	
	private final List<Endpoint> endpoints;
	private ScheduledExecutorService executor;
	
	/**
	 * Constructs a default server. The server starts after the method
	 * {@link #start()} is called. If a server starts and has no specific ports
	 * assigned, it will bind to CoAp's default port 5683.
	 */
	public Server() {
		this(NetworkConfig.getStandard());
	}
	
	/**
	 * Constructs a server that listens to the specified port after method
	 * {@link #start()} is called.
	 * 
	 * @param ports the ports
	 */
	public Server(int... ports) {
		this(NetworkConfig.getStandard(), ports);
	}
	
	public Server(NetworkConfig config, int... ports) {
		this.root = createRoot();
		this.endpoints = new ArrayList<Endpoint>();
		this.executor = Executors.newScheduledThreadPool(
				config.getInt(NetworkConfigDefaults.SERVER_THRESD_NUMER));
		this.deliverer = new ServerMessageDeliverer(root);
		
		ResourceBase well_known = new ResourceBase(".well-known");
		well_known.setVisible(false);
		well_known.add(new DiscoveryResource(root));
		root.add(well_known);
		
		for (int port:ports)
			bind(port);
	}
	
	public void bind(int port) {
		// Martin: That didn't work out well :-/
//		if (port == EndpointManager.DEFAULT_PORT) {
//			for (Endpoint ep:EndpointManager.getEndpointManager().getDefaultEndpointsFromAllInterfaces())
//					addEndpoint(ep);
//		} else if (port == EndpointManager.DEFAULT_DTLS_PORT) {
//			for (Endpoint ep:EndpointManager.getEndpointManager().getDefaultSecureEndpointsFromAllInterfaces())
//					addEndpoint(ep);
//		} else {
//			for (InetAddress addr:EndpointManager.getEndpointManager().getNetworkInterfaces()) {
//				addEndpoint(new Endpoint(new InetSocketAddress(addr, port)));
//			}
//		}
//		addEndpoint(new Endpoint(port));
		
		// This endpoint binds to all interfaces. But there is no way (in Java)
		// of knowing to which interface address the packet actually has been
		// sent.
		bind(new InetSocketAddress((InetAddress) null, port));
	}
	
	public void bind(InetSocketAddress address) {
		Endpoint endpoint = new CoAPEndpoint(address);
		addEndpoint(endpoint);
	}
	
	public void setExecutor(ScheduledExecutorService executor) {
		this.executor = executor;
		for (Endpoint ep:endpoints)
			ep.setExecutor(executor);
	}
	
	/**
	 * Starts the server by starting all endpoints this server is assigned to.
	 * Each endpoint binds to its port. If no endpoint is assigned to the
	 * server, the server binds to CoAP0's default port 5683.
	 */
	public void start() {
		LOGGER.info("Start server");
		if (endpoints.isEmpty()) {
			LOGGER.info("Server has no endpoints yet and takes default endpoint");
			bind(EndpointManager.DEFAULT_COAP_PORT);
		}
		for (Endpoint ep:endpoints) {
			try {
				ep.start();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Exception in thread \"" + Thread.currentThread().getName() + "\"", e);
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * Stops the server, i.e. unbinds it from all ports. Frees as much system
	 * resources as possible to still be able to be started.
	 */
	public void stop() {
		LOGGER.info("Stop server");
		for (Endpoint ep:endpoints)
			ep.stop();
	}
	
	/**
	 * Destroys the server, i.e. unbinds from all ports and frees all system
	 * resources.
	 */
	public void destroy() {
		LOGGER.info("Destroy server");
		for (Endpoint ep:endpoints)
			ep.destroy();
		executor.shutdown(); // cannot be started again
		try {
			boolean succ = executor.awaitTermination(5, TimeUnit.SECONDS);
			if (!succ)
				LOGGER.warning("Stack executor did not shutdown in time");
		} catch (InterruptedException e) {
			LOGGER.log(Level.WARNING, "Exception while terminating stack executor", e);
		}
	}
	
	public void setMessageDeliverer(MessageDeliverer deliverer) {
		this.deliverer = deliverer;
		for (Endpoint endpoint:endpoints)
			endpoint.setMessageDeliverer(deliverer);
	}
	
	public MessageDeliverer getMessageDeliverer() {
		return deliverer;
	}
	
	public void addEndpoint(Endpoint endpoint) {
		endpoint.setMessageDeliverer(deliverer);
		endpoint.setExecutor(executor);
		endpoints.add(endpoint);
	}
	
	public List<Endpoint> getEndpoints() {
		return endpoints;
	}

	/**
	 * Add a resource to the server.
	 * @param resource the resource
	 * @return the server
	 */
	public Server add(Resource... resources) {
		for (Resource r:resources)
			root.add(r);
		return this;
	}
	
	public boolean remove(Resource resource) {
		return root.remove(resource);
	}

	public Resource getRoot() {
		return root;
	}
	
	protected Resource createRoot() {
		return new RootResource();
	}
	
	private class RootResource extends ResourceBase {
		
		public RootResource() {
			super("");
		}
		
		public List<Endpoint> getEndpoints() {
			return Server.this.getEndpoints();
		}
	}
	
}

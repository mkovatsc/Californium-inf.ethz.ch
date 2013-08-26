package ch.ethz.inf.vs.californium;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointAddress;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.Matcher;
import ch.ethz.inf.vs.californium.network.MessageIntercepter;
import ch.ethz.inf.vs.californium.network.connector.Connector;
import ch.ethz.inf.vs.californium.network.layer.BlockwiseLayer;
import ch.ethz.inf.vs.californium.network.layer.ObserveLayer;
import ch.ethz.inf.vs.californium.network.layer.ReliabilityLayer;
import ch.ethz.inf.vs.californium.network.layer.TokenLayer;
import ch.ethz.inf.vs.californium.resources.DiscoveryResource;
import ch.ethz.inf.vs.californium.resources.Resource;
import ch.ethz.inf.vs.californium.resources.ResourceBase;

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
 * TODO: more description
 **/
public class Server implements ServerInterface {

	public static boolean LOG_ENABLED = true;
	
	private final static Logger LOGGER = CalifonriumLogger.getLogger(Server.class);

	private final Resource root;
	
	private final List<Endpoint> endpoints;
	
	private ScheduledExecutorService executor;
	private MessageDeliverer deliverer;
	
	/**
	 * Constructs a default server. The server starts after the method
	 * {@link #start()} is called. If a server starts and has no specific ports
	 * assigned, it will bind to CoAp's default port 5683.
	 */
	public Server() {
		this.root = new ResourceBase("");
		this.endpoints = new ArrayList<Endpoint>();
		this.executor = Executors.newScheduledThreadPool(4);
		this.deliverer = new ServerMessageDeliverer(root);
		
		Resource well_known = new ResourceBase(".well-known");
		well_known.add(new DiscoveryResource(root));
		root.add(well_known);
	}
	
	/**
	 * Constructs a server that listens to the specified port after method
	 * {@link #start()} is called.
	 * 
	 * @param ports the ports
	 */
	public Server(int... ports) {
		this();
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
//				addEndpoint(new Endpoint(new EndpointAddress(addr, port)));
//			}
//		}
//		addEndpoint(new Endpoint(port));
		
		// This endpoint binds to all interfaces. But there is no way (in Java)
		// of knowing to which interface address the packet actually has been
		// sent.
		bind(new EndpointAddress(null, port));
	}
	
	public void bind(EndpointAddress address) {
		Endpoint endpoint = new Endpoint(address);
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
			bind(EndpointManager.DEFAULT_PORT);
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
	public Server add(Resource resource) {
		root.add(resource);
		return this;
	}
	
	public boolean remove(Resource resource) {
		return root.remove(resource);
	}

	public Resource getRoot() {
		return root;
	}
}

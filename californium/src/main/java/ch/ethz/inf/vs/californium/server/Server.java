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
 * <p>
 * A server holds a tree of {@link Resource} that react to incoming requests. A
 * server uses an {@link Endpoint} to bind to the network. Typically, a
 * {@link CoAPEndpoint} is used. A server can be started and stopped. When the
 * server stops the endpoint should free the port it is listening on.
 * <p>
 * The following is a simple example of a server with a resource that responds
 * with a "hello world" to GET requests.
 * <pre>
 *   Server server = new Server(port);
 *   server.add(new ResourceBase(&quot;hello-world&quot;) {
 * 	   public void handleGET(CoapExchange exchange) {
 * 	  	 exchange.respond(ResponseCode.CONTENT, &quot;hello world&quot;);
 * 	   }
 *   });
 *   server.start();
 * </pre>
 * 
 * The following figure shows the server architecture.
 * 
 * <pre>
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
 * </pre>
 **/
public class Server implements ServerInterface {

	/** The logger. */
	private final static Logger LOGGER = CalifonriumLogger.getLogger(Server.class);

	/** The root resource. */
	private final Resource root;
	
	/** The message deliverer. */
	private MessageDeliverer deliverer;
	
	/** The list of endpoints the server connectes to the network. */
	private final List<Endpoint> endpoints;
	
	/** The executor of the server for its endpoints (can be null). */
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
	 * Constructs a server that listens to the specified ports after method.
	 *
	 * @param ports the ports
	 * {@link #start()} is called.
	 */
	public Server(int... ports) {
		this(NetworkConfig.getStandard(), ports);
	}
	
	/**
	 * Constructs a server with the specified configuration that listens to the
	 * specified ports after method {@link #start()} is called.
	 *
	 * @param config the configuration
	 * @param ports the ports
	 */
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
	
	/**
	 * Binds the server to the specified port.
	 *
	 * @param port the port
	 */
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
	
	/**
	 * Binds the server to a ephemeral port on the secified address.
	 *
	 * @param address the address
	 */
	public void bind(InetSocketAddress address) {
		Endpoint endpoint = new CoAPEndpoint(address);
		addEndpoint(endpoint);
	}
	
	/**
	 * Sets the executor of this server. This method also sets the specified
	 * executor to all endpoints that the server currently uses.
	 *
	 * @param executor the new executor
	 */
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
	
	/**
	 * Sets the message deliverer.
	 *
	 * @param deliverer the new message deliverer
	 */
	public void setMessageDeliverer(MessageDeliverer deliverer) {
		this.deliverer = deliverer;
		for (Endpoint endpoint:endpoints)
			endpoint.setMessageDeliverer(deliverer);
	}
	
	/**
	 * Gets the message deliverer.
	 *
	 * @return the message deliverer
	 */
	public MessageDeliverer getMessageDeliverer() {
		return deliverer;
	}
	
	/**
	 * Adds the specified endpoint.
	 *
	 * @param endpoint the endpoint
	 */
	public void addEndpoint(Endpoint endpoint) {
		endpoint.setMessageDeliverer(deliverer);
		endpoint.setExecutor(executor);
		endpoints.add(endpoint);
	}
	
	/**
	 * Gets the list of endpoints this server is connected to.
	 *
	 * @return the endpoints
	 */
	public List<Endpoint> getEndpoints() {
		return endpoints;
	}

	/**
	 * Add a resource to the server.
	 *
	 * @param resources the resources
	 * @return the server
	 */
	public Server add(Resource... resources) {
		for (Resource r:resources)
			root.add(r);
		return this;
	}
	
	/**
	 * Removes the specified resource.
	 *
	 * @param resource the resource
	 * @return true, if the resource was found
	 */
	public boolean remove(Resource resource) {
		return root.remove(resource);
	}

	/**
	 * Gets the root of this server.
	 *
	 * @return the root
	 */
	public Resource getRoot() {
		return root;
	}
	
	/**
	 * Creates a root for this server. Can be overridden to create another root.
	 *
	 * @return the resource
	 */
	protected Resource createRoot() {
		return new RootResource();
	}
	
	/**
	 * Represents the root of a resource tree.
	 */
	private class RootResource extends ResourceBase {
		
		/**
		 * Instantiates a new root resource.
		 */
		public RootResource() {
			super("");
		}
		
		/* (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.server.resources.ResourceBase#getEndpoints()
		 */
		public List<Endpoint> getEndpoints() {
			return Server.this.getEndpoints();
		}
	}
}

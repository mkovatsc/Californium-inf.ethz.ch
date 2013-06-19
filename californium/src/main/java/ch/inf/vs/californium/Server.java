package ch.inf.vs.californium;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointManager;
import ch.inf.vs.californium.network.Matcher;
import ch.inf.vs.californium.network.MessageIntercepter;
import ch.inf.vs.californium.network.connector.Connector;
import ch.inf.vs.californium.network.layer.BlockwiseLayer;
import ch.inf.vs.californium.network.layer.ObserveLayer;
import ch.inf.vs.californium.network.layer.ReliabilityLayer;
import ch.inf.vs.californium.network.layer.TokenLayer;
import ch.inf.vs.californium.resources.CalifonriumLogger;
import ch.inf.vs.californium.resources.DiscoveryResource;
import ch.inf.vs.californium.resources.Resource;
import ch.inf.vs.californium.resources.ResourceBase;

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

	public static final boolean LOG_ENABLED = true;
	
	private final static Logger LOGGER = CalifonriumLogger.getLogger(Server.class);

	private final Resource root;
	
	private final List<Endpoint> endpoints;
	
	private ScheduledExecutorService stackExecutor;
	private MessageDeliverer deliverer;
	
	public Server() {
		ResourceBase theRoot = new ResourceBase("");
		theRoot.setDoesAcceptRequestForChild(false);
		this.root = theRoot;
		this.endpoints = new ArrayList<Endpoint>();
		this.stackExecutor = Executors.newScheduledThreadPool(4);
		this.deliverer = new ServerMessageDeliverer(root);
		
		Resource well_known = new ResourceBase(".well-known");
		well_known.add(new DiscoveryResource(root));
		root.add(well_known);
	}
	
	public Server(int... ports) {
		this();
		for (int port:ports)
			registerEndpoint(port);
	}
	
	public void start() {
		LOGGER.info("Start server");
		for (Endpoint ep:endpoints) {
			try {
				ep.start();
				Thread.sleep(50); // TODO: remove
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.log(Level.WARNING, "Exception in thread \"" + Thread.currentThread().getName() + "\"", e);
			}
		}
	}
	
	public void stop() {
		LOGGER.info("Stop server");
		for (Endpoint ep:endpoints)
			ep.stop();
	}
	
	public void destroy() {
		LOGGER.info("Destroy server");
		for (Endpoint ep:endpoints)
			ep.destroy();
		stackExecutor.shutdown(); // cannot be started again
		try {
			boolean succ = stackExecutor.awaitTermination(1, TimeUnit.SECONDS);
			if (!succ)
				LOGGER.warning("Stack executor did not shutdown in time");
		} catch (InterruptedException e) {
			LOGGER.log(Level.WARNING, "Exception while terminating stack executor", e);
		}
	}
	
	public void registerEndpoint(/*InetAddress, */ int port) {
		Endpoint endpoint;
		if (port == EndpointManager.DEFAULT_PORT)
			endpoint = EndpointManager.getEndpointManager().getDefaultEndpoint();
		else if (port == EndpointManager.DEFAULTDLTS_PORT)
			endpoint = EndpointManager.getEndpointManager().getDefaultSecureEndpoint();
		else
			endpoint = new Endpoint(port);
		addEndpoint(endpoint);
	}
	
	public void setMessageDeliverer(MessageDeliverer deliverer) {
		this.deliverer = deliverer;
		for (Endpoint endpoint:endpoints)
			endpoint.setMessageDeliverer(deliverer);
	}
	
	public void addEndpoint(Endpoint endpoint) {
		endpoint.setMessageDeliverer(deliverer);
		endpoint.setExecutor(stackExecutor);
		endpoints.add(endpoint);
	}

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

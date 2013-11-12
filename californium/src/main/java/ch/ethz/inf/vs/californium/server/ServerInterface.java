
package ch.ethz.inf.vs.californium.server;

import java.util.concurrent.ScheduledExecutorService;

import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.server.resources.Resource;

/**
 * Interface for a server. A server contains a resource structure and can listen
 * to one or more endpoints to handle requests. Resources of a server can send
 * requests over any endpoint the server is associated to.
 */
public interface ServerInterface {

	// be a server
	
	/**
	 * Starts the server by starting all endpoints this server is assigned to.
	 * Each endpoint binds to its port. If no endpoint is assigned to the
	 * server, the server binds to CoAP0's default port 5683.
	 */
	void start();

	/**
	 * Stops the server, i.e. unbinds it from all ports. Frees as much system
	 * resources as possible to still be able to be started.
	 */
	void stop();
	
	/**
	 * Destroys the server, i.e. unbinds from all ports and frees all system
	 * resources.
	 */
	void destroy();
	
	/**
	 * Adds one or more resources to the server.
	 * 
	 * @param resource the resource
	 * @return the server
	 */
	Server add(Resource... resources);
	
	/**
	 * Removes a resource from the server.
	 * 
	 * @param resource the resource to be removed
	 * @return <code>true</code> if the resource has been removed successfully
	 */
	boolean remove(Resource resource);
	
	/**
	 * Adds an endpoint listening on a particular network interface and port.
	 *  
	 * @param endpoint the endpoint specification
	 * @throws NullPointerException if the endpoint is <code>null</code>
	 */
	void addEndpoint(Endpoint endpoint);
	
	/**
	 * Sets the strategy for executing request handling threads. This strategy
	 * is used for all endpoints configured for the sever. This method also sets
	 * the specified executor to all endpoints that the server currently uses.
	 * 
	 * @param executor the executor
	 * @throws NullPointerException if the executor is <code>null</code>
	 */
	void setExecutor(ScheduledExecutorService executor);
}


package ch.ethz.inf.vs.californium.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.interceptors.MessageInterceptor;
import ch.ethz.inf.vs.californium.server.MessageDeliverer;

/**
 * A communication endpoint multiplexing CoAP message exchanges between (potentially multiple) clients and servers.
 * 
 * An Endpoint is bound to a particular IP address and port.
 * Clients use an Endpoint to send a request to a server. Servers bind resources to one or more Endpoints
 * in order for them to be requested over the network by clients.
 */
public interface Endpoint {

	/**
	 * Start this endpoint and all its components.. The starts its connector. If
	 * no executor has been set yet, the endpoint uses a single-threaded
	 * executor.
	 * 
	 * @throws IOException if the endpoint could not be started, e.g. because
	 * the endpoint's port is already in use.
	 */
	public void start() throws IOException;

	/**
	 * Stop this endpoint and all its components, e.g., the connector. A
	 * stopped endpoint can be started again.
	 */
	public void stop();

	/**
	 * Destroys the endpoint and all its components. A destroyed endpoint cannot
	 * be started again.
	 */
	public void destroy();

	// Needed for tests to remove duplicates
	public void clear();

	/**
	 * Checks if the endpoint has started.
	 *
	 * @return true, if has started
	 */
	public boolean isStarted();

	/**
	 * Sets the executor for this endpoint and all its components.
	 *
	 * @param executor the new executor
	 */
	public void setExecutor(ScheduledExecutorService executor);

	/**
	 * Adds the observer to the list of observers. This has nothing to do with
	 * CoAP observe relations.
	 * 
	 * @param obs the observer
	 */
	public void addObserver(EndpointObserver obs);

	/**
	 * Removes the endpoint observer.This has nothing to do with
	 * CoAP observe relations.
	 *
	 * @param obs the observer
	 */
	public void removeObserver(EndpointObserver obs);

	/**
	 * Adds the interceptor.
	 *
	 * @param interceptor the interceptor
	 */
	public void addInterceptor(MessageInterceptor interceptor);

	/**
	 * Removes the interceptor.
	 *
	 * @param interceptor the interceptor
	 */
	public void removeInterceptor(MessageInterceptor interceptor);

	/**
	 * Gets the list of interceptors.
	 *
	 * @return the interceptors
	 */
	public List<MessageInterceptor> getInterceptors();

	/**
	 * Send the specified request.
	 *
	 * @param request the request
	 */
	public void sendRequest(Request request);

	/**
	 * Send the specified response.
	 *
	 * @param exchange the exchange
	 * @param response the response
	 */
	public void sendResponse(Exchange exchange, Response response);

	/**
	 * Send the specified empty message.
	 *
	 * @param exchange the exchange
	 * @param message the message
	 */
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message);

	/**
	 * Sets the message deliverer.
	 *
	 * @param deliverer the new message deliverer
	 */
	public void setMessageDeliverer(MessageDeliverer deliverer);

	/**
	 * Gets the address this endpoint is associated with.
	 *
	 * @return the address
	 */
	public InetSocketAddress getAddress();

	/**
	 * Gets this endpoint's configuration.
	 *
	 * @return the configuration
	 */
	public NetworkConfig getConfig();

}


package ch.ethz.inf.vs.californium.network;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.server.MessageDeliverer;

/**
 * The Endpoint encapsulates the protocol that the server uses. By default, this
 * is the CoAP protocol, i.e. {@link CoAPEndpoint}. An endpoint has its own
 * executor to process incoming and outgoing messages. The endpoint forwards
 * incoming requests and responses to a {@link MessageDeliverer} and sends
 * outgoing messages over the network.
 */
public interface Endpoint {

	/**
	 * Start this endpoint and all its components.. The starts its connector. If
	 * no executor has been set yet, the endpoint uses a single-threaded
	 * executor.
	 */
	public void start();

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
	public void addInterceptor(MessageIntercepter interceptor);

	/**
	 * Removes the interceptor.
	 *
	 * @param interceptor the interceptor
	 */
	public void removeInterceptor(MessageIntercepter interceptor);

	/**
	 * Gets the list of interceptors.
	 *
	 * @return the interceptors
	 */
	public List<MessageIntercepter> getInterceptors();

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

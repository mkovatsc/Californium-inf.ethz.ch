package ch.ethz.inf.vs.californium.osgi;

import java.net.InetSocketAddress;

import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;

/**
 * A factory for creating {@link Endpoint}s.
 * 
 * @author Kai Hudalla
 */
public interface EndpointFactory {

	/**
	 * Gets a communication endpoint bound to a given IP address and port.
	 * 
	 * The endpoints returned by this method are <em>not</em> started yet.
	 * 
	 * @param config the configuration properties to be used for creating the
	 * endpoint or <code>null</code> if default values should be used
	 * @param address the IP address and port to bind to
	 * @return the endpoint
	 */
	Endpoint getEndpoint(NetworkConfig config, InetSocketAddress address);
	
	/**
	 * Gets an Endpoint that uses DTLS for secure communication.
	 * 
	 * The endpoints returned by this method are <em>not</em> started yet.
	 * 
	 * @param config the configuration properties to be used for creating the
	 * endpoint or <code>null</code> if default values should be used
	 * @param address the address
	 * @return the secure endpoint or <code>null</code> if this factory
	 * does not support secure endpoints
	 */
	Endpoint getSecureEndpoint(NetworkConfig config, InetSocketAddress address);
}

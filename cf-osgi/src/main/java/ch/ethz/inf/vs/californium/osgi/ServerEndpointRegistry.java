package ch.ethz.inf.vs.californium.osgi;

import java.net.InetSocketAddress;

import ch.ethz.inf.vs.californium.network.Endpoint;

public interface ServerEndpointRegistry {
	
	/**
	 * Gets the endpoint bound to a particular address.
	 * 
	 * @param address the address
	 * @return the endpoint or <code>null</code> if none of the
	 * server's endpoints is bound to the given address
	 */
	Endpoint getEndpoint(InetSocketAddress address);
	
	/**
	 * Gets the endpoint bound to a particular port.
	 * 
	 * @param port the port
	 * @return the endpoint or <code>null</code> if none of the
	 * server's endpoints is bound to the given port on any of its
	 * network interfaces
	 */
	Endpoint getEndpoint(int port);	
}

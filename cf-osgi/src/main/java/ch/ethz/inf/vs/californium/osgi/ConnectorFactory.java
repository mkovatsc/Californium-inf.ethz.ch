package ch.ethz.inf.vs.californium.osgi;

import java.net.InetSocketAddress;

import ch.ethz.inf.vs.elements.Connector;

public interface ConnectorFactory {

	/**
	 * Creates a new network connector.
	 * 
	 * @param socketAddress the IP address and port to connect to
	 * @return the connector
	 */
	Connector newConnector(InetSocketAddress socketAddress);
}

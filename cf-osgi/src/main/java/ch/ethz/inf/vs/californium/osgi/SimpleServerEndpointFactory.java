package ch.ethz.inf.vs.californium.osgi;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import org.osgi.service.io.ConnectionFactory;

import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.elements.Connector;
import ch.ethz.inf.vs.elements.ConnectorFactory;

/**
 * A basic implementation for creating standard CoAP endpoints.
 * 
 * The factory can also create secure endpoints if it has been configured
 * with a secure {@link ConnectionFactory}.
 * 
 * @author Kai Hudalla
 */
public class SimpleServerEndpointFactory implements EndpointFactory {

	private final Logger log = Logger.getLogger(SimpleServerEndpointFactory.class.getName());
	
	private ConnectorFactory secureConnectorFactory;
	
	/**
	 * Initializes the factory with collaborators.
	 * 
	 * @param secureConnectorFactory the factory to use for creating {@link Connector}s
	 * implementing DTLS for secure Endpoints or <code>null</code> if this factory
	 * does not support the creation of secure Endpoints.
	 */
	public SimpleServerEndpointFactory(ConnectorFactory secureConnectorFactory) {
		this.secureConnectorFactory = secureConnectorFactory;
	}
	
	@Override
	public final Endpoint getEndpoint(NetworkConfig config, InetSocketAddress address) {
		
		CoAPEndpoint endpoint = new CoAPEndpoint(address, config);
		return endpoint;
	}

	@Override
	public final Endpoint getSecureEndpoint(NetworkConfig config, InetSocketAddress address) {

		Endpoint endpoint = null;
		if (secureConnectorFactory != null) {
			endpoint = new CoAPEndpoint(
					secureConnectorFactory.newConnector(address),
					config);
		} else {
			log.fine("A secure ConnectorFactory is required to create secure Endpoints.");
		}
		return endpoint;
	}

}

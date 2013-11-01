package ch.ethz.inf.vs.californium.osgi;

import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.ServerInterface;

/**
 * A factory for {@link ServerInterface} instances.
 * This factory is used by the {@link ManagedServer} in order to create a new server instance
 * when properties are updated via OSGi's Config Admin Service.
 * 
 * @author Kai Hudalla
 */
interface ServerInterfaceFactory {
	
	/**
	 * Creates a new {@link ServerInterface} instance.
	 * Can be overridden e.g. by test classes to use a mock instance instead of a <i>real</i> server.
	 * This default implementation returns a new instance of {@link Server}.
	 * 
	 * @return the new instance
	 */
	ServerInterface newServer();
}
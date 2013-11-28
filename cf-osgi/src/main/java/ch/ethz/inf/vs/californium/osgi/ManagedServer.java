package ch.ethz.inf.vs.californium.osgi;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.ServerInterface;
import ch.ethz.inf.vs.californium.server.resources.Resource;

/**
 * A managed Californium {@code ServerInterface} instance that can be configured using the OSGi
 * <i>Configuration Admin</i> service.
 * 
 * The service understands all network configuration properties defined
 * by {@link NetworkConfigDefaults}.
 * Additionally, it understands the following properties:
 * <ul>
 * <li>ENDPOINT_PORT - Adds an endpoint on the given port. This property
 * can be specified multiple times in order to define multiple endpoints. If this
 * property is not specified at all, a single endpoint on the default CoAP port 5683
 * is created.</li>
 * </ul>
 * 
 * This managed service uses the <i>white board</i> pattern for registering resources,
 * i.e. the service tracks Californium {@code Resource} instances being added to the OSGi service registry
 * and automatically adds them to the managed Californium {@code ServerInterface} instance.
 *  
 * @author Kai Hudalla
 */
public class ManagedServer implements ManagedService, ServiceTrackerCustomizer<Resource, Resource> {

	private final static Logger LOGGER = Logger.getLogger(ManagedServer.class.getCanonicalName());
	
	public final static String ENDPOINT_PORT = "ENDPOINT_PORT";
	private ServerInterface server;
	private boolean running = false;
	private BundleContext context;
	private ServiceTracker<Resource, Resource> resourceTracker;
	private ServerInterfaceFactory serverFactory;
	
	/**
	 * Creates a new instance by invoking
	 * {@link ServiceTracker#ServiceTracker(BundleContext, String, org.osgi.util.tracker.ServiceTrackerCustomizer)}.
	 * Invoking this constructor is equivalent to invoking {@link #ManagedServer(BundleContext, ServerInterfaceFactory)
	 * with <code>null</code> as the server factory.
	 * 
	 * @param bundleContext the bundle context to be used for tracking {@code Resource}s
	 * @throws NullPointerException if the bundle context is <code>null</code>
	 */
	public ManagedServer(BundleContext bundleContext) {
		this(bundleContext, null);
	}

	/**
	 * Creates a new instance by invoking
	 * {@link ServiceTracker#ServiceTracker(BundleContext, String, org.osgi.util.tracker.ServiceTrackerCustomizer)}.
	 * 
	 * @param bundleContext the bundle context to be used for tracking {@code Resource}s
	 * @param serverFactory the factory to use for creating new server instances
	 * @throws NullPointerException if the bundle context is <code>null</code>
	 */
	public ManagedServer(BundleContext bundleContext, ServerInterfaceFactory serverFactory) {
		if (bundleContext == null) {
			throw new NullPointerException("BundleContext must not be null");
		}
		this.context = bundleContext;
		if (serverFactory != null) {
			this.serverFactory = serverFactory;
		} else {
			this.serverFactory= new ServerInterfaceFactory() {
				
				@Override
				public ServerInterface newServer() {
					return new Server();
				}
			};
		}
	}

	/**
	 * Updates the configuration properties of the wrapped Californium server.
	 * 
	 * If the server is running when this method is called by ConfigAdmin, the server
	 * is destroyed, a new instance is created using the given properties and finally
	 * started.
	 *  
	 * @param properties the properties to set on the server
	 */
	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {

		LOGGER.fine("Updating configuration of managed server instance");
		List<Integer> endpointList = new LinkedList<Integer>();
		
		if (isRunning()) {
			stop();
		}

		server = serverFactory.newServer();
		
		NetworkConfig networkConfig = NetworkConfig.createStandardWithoutFile();

		if (properties != null) {
			for (Enumeration<String> allKeys = properties.keys(); allKeys.hasMoreElements(); ) {
				String key = allKeys.nextElement();
				if (ENDPOINT_PORT.equals(key)) {
					String port = (String) properties.get(key);
					try {
						endpointList.add(Integer.parseInt(port));
					} catch (NumberFormatException e) {
						LOGGER.warning(String.format("Property value [%s] for key [%s] cannot be parsed into a port number", port, key));
					}
				} else {
					networkConfig.set(key, properties.get(key));
				}
			}
		}

		for (int port : endpointList) {
			LOGGER.fine(String.format("Adding endpoint on port %d", port));
			server.addEndpoint(new CoAPEndpoint(new InetSocketAddress((InetAddress) null, port)));
		}
		
		server.setExecutor(Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()));
		server.start();
		running = true;

		// start tracking resources registered by arbitrary bundles
		resourceTracker = new ServiceTracker<Resource, Resource>(context, Resource.class.getName(), this);
		resourceTracker.open();
	}
	
	private boolean isRunning() {
		return running;
	}
	
	/**
	 * Stops and destroys the managed server instance.
	 * 
	 * This method should be called by the {@code BundleActivator} that registered
	 * this managed service when the bundle is stopped.
	 */
	protected void stop() {
		if (server != null) {
			LOGGER.fine("Destroying managed server instance");
			if (resourceTracker != null) {
				// stop tracking Resources
				resourceTracker.close();
			}
			
			server.destroy();
			running = false;
		}
	}

	/**
	 * Adds a Californium {@code Resource} to the managed Californium {@code Server}.
	 * 
	 * This method is invoked automatically by the {@code ServiceTracker} whenever
	 * a {@code Resource} is added to the OSGi service registry.
	 * 
	 * @param reference the {@code Resource} service that has been added
	 * @return the unmodified {@code Resource}
	 */
	@Override
	public Resource addingService(ServiceReference<Resource> reference) {
		Resource resource = context.getService(reference);
		LOGGER.fine(String.format("Adding resource [%s]", resource.getName()));
		if (resource != null) {
			server.add(resource);
		}
		return resource;
	}
	
	/**
	 * Removes a Californium {@code Resource} from the managed Californium {@code Server}.
	 * 
	 * This method is invoked automatically by the {@code ServiceTracker} whenever
	 * a {@code Resource} is removed from the OSGi service registry.
	 * 
	 * @param reference the reference to the {@code Resource} service that has been removed
	 * @param service the service object
	 */
	@Override
	public void removedService(ServiceReference<Resource> reference,
			Resource service) {
		LOGGER.fine(String.format("Removing resource [%s]", service.getName()));
		server.remove(service);
		context.ungetService(reference);
	}
	
	/**
	 * Does nothing as the Californium server does not need to be informed about
	 * updated service registration properties of a {@code Resource}.
	 * 
	 * @param reference the updated {@code Resource} service reference
	 * @param service the corresponding {@code Resource} instance
	 */
	@Override
	public void modifiedService(ServiceReference<Resource> reference,
			Resource service) {
		// nothing to do
	}
}

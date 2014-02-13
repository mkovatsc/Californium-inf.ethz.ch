package ch.ethz.inf.vs.californium.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.ServerInterface;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;


public class ManagedServerTest {

	ServerInterfaceFactory serverFactory;
	ServerInterface server;
	ManagedServer managedServer;
	BundleContext bundleContext;
	EndpointFactory endpointFactory;
	EndpointFactory secureEndpointFactory;
	List<Endpoint> endpointList = new LinkedList<Endpoint>();
	Endpoint standardEndpoint;
	Endpoint secureEndpoint;
	
	InetSocketAddress standardAddress = new InetSocketAddress(EndpointManager.DEFAULT_COAP_PORT);
	InetSocketAddress secureAddress = new InetSocketAddress(EndpointManager.DEFAULT_COAP_PORT);
	
	@Before
	public void setUp() {
		
		standardEndpoint = mock(Endpoint.class);
		secureEndpoint = mock(Endpoint.class);
		when(standardEndpoint.getAddress()).thenReturn(standardAddress);
		when(secureEndpoint.getAddress()).thenReturn(secureAddress);
		
		server = mock(ServerInterface.class);
		when(server.getEndpoints()).thenReturn(endpointList);
		
		bundleContext = mock(BundleContext.class);
		serverFactory = new ServerInterfaceFactory() {
			
			@Override
			public ServerInterface newServer(NetworkConfig config) {
				return newServer(config, EndpointManager.DEFAULT_COAP_PORT);
			}

			@Override
			public ServerInterface newServer(NetworkConfig config, int... ports) {
				for (int port : ports) {
					if (port == standardAddress.getPort()) {
						endpointList.add(standardEndpoint);
					} else if (port == secureAddress.getPort()) {
						endpointList.add(secureEndpoint);
					} else {
						endpointList.add(mock(Endpoint.class));
					}
				}
				return server;
			}
		};
		
		secureEndpointFactory = new EndpointFactory() {
			
			@Override
			public Endpoint getSecureEndpoint(NetworkConfig config,
					InetSocketAddress address) {
				return secureEndpoint;
			}
			
			@Override
			public Endpoint getEndpoint(NetworkConfig config, InetSocketAddress address) {
				return standardEndpoint;
			}
		};

		endpointFactory = new EndpointFactory() {
			
			@Override
			public Endpoint getSecureEndpoint(NetworkConfig config,
					InetSocketAddress address) {
				return null;
			}
			
			@Override
			public Endpoint getEndpoint(NetworkConfig config, InetSocketAddress address) {
				return standardEndpoint;
			}
		};
		managedServer = new ManagedServer(bundleContext, serverFactory, endpointFactory);
	}
	
	@Test
	public void testUpdatedDestroysAndCreatesServer() throws Exception {
		managedServer.updated(null);
		verify(server).start();
		reset(server);
		when(server.getEndpoints()).thenReturn(endpointList);
		managedServer.updated(null);
		verify(server).destroy();
		verify(server).start();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAddingService() throws Exception {
		Resource resource = new ResourceBase("test");
		ServiceReference<Resource> ref = mock(ServiceReference.class);
		when(bundleContext.getService(ref)).thenReturn(resource);
		managedServer.updated(null);
		
		Resource addedResource = managedServer.addingService(ref);
		assertEquals(resource, addedResource);
		verify(server).add(resource);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRemovedService() throws Exception {
		Resource resource = new ResourceBase("test");
		ServiceReference<Resource> ref = mock(ServiceReference.class);
		when(bundleContext.getService(ref)).thenReturn(resource);
		managedServer.updated(null);
		
		managedServer.removedService(ref, resource);
		verify(server).remove(resource);
	}

	
	@Test
	public void testSecureEndpointRequiresSecureEndpointFactory() throws Exception {
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(NetworkConfigDefaults.PROPERTY_DEFAULT_COAPS_PORT, Integer.toString(EndpointManager.DEFAULT_COAP_SECURE_PORT));
		managedServer.updated(props);
		assertFalse(server.getEndpoints().isEmpty());
		// verify that the secure CoAP endpoint has not been registered 
		verify(server, times(0)).addEndpoint(secureEndpoint);
	}
	
	@Test
	public void testServiceRegistersEndpoints() throws Exception {

		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(NetworkConfigDefaults.PROPERTY_DEFAULT_COAPS_PORT, Integer.toString(EndpointManager.DEFAULT_COAP_SECURE_PORT));
		
		managedServer = new ManagedServer(bundleContext, serverFactory, secureEndpointFactory);
		managedServer.updated(props);
		
		assertFalse(server.getEndpoints().isEmpty());
		verify(server).addEndpoint(secureEndpoint);
	}
	
	@Test
	public void testEndpointRegistryRetrievesEndpointsFromManagedServer() throws Exception {
		
		managedServer.updated(null);
		managedServer.getEndpoint(standardAddress);
		managedServer.getEndpoint(EndpointManager.DEFAULT_COAP_PORT);
		
		verify(server).getEndpoint(standardAddress);
		verify(server).getEndpoint(EndpointManager.DEFAULT_COAP_PORT);
	}
}

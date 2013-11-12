package ch.ethz.inf.vs.californium.osgi;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.server.ServerInterface;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;


public class ManagedServerTest {

	ServerInterfaceFactory serverFactory;
	ServerInterface server;
	ManagedServer managedServer;
	BundleContext bundleContext;
	
	@Before
	public void setUp() {
		server = mock(ServerInterface.class);
		bundleContext = mock(BundleContext.class);
		serverFactory = new ServerInterfaceFactory() {
			
			@Override
			public ServerInterface newServer() {
				return server;
			}
		};
		managedServer = new ManagedServer(bundleContext, serverFactory);
	}
	
	@Test
	public void testUpdatedDestroysAndCreatesServer() throws Exception {
		managedServer.updated(null);
		verify(server).start();
		reset(server);
		managedServer.updated(null);
		verify(server).destroy();
		verify(server).start();
	}
	
	@Test
	public void testUpdatedRegistersEndpoints() throws Exception {
		String portNo = "666";
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(ManagedServer.ENDPOINT_PORT, portNo);
		
		managedServer.updated(props);
		verify(server).addEndpoint(any(Endpoint.class));
	}

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

	@Test
	public void testRemovedService() throws Exception {
		Resource resource = new ResourceBase("test");
		ServiceReference<Resource> ref = mock(ServiceReference.class);
		when(bundleContext.getService(ref)).thenReturn(resource);
		managedServer.updated(null);
		
		managedServer.removedService(ref, resource);
		verify(server).remove(resource);
	}

}

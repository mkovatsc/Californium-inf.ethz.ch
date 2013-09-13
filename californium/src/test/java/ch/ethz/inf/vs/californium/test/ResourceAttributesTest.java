package ch.ethz.inf.vs.californium.test;

import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.server.resources.DiscoveryResource;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class ResourceAttributesTest {

	private Resource root;
	
	@Before
	public void setup() {
		try {
			System.out.println("\nStart "+getClass().getSimpleName());
			EndpointManager.clear();
			
			root = new ResourceBase("");
			Resource sensors = new ResourceBase("sensors");
			Resource temp = new ResourceBase("temp");
			Resource light = new ResourceBase("light");
			root.add(sensors);
			sensors.add(temp);
			sensors.add(light);
			
			sensors.getAttributes().setTitle("Sensor Index");
			temp.getAttributes().addResourceType("temperature-c");
			temp.getAttributes().addInterfaceDescription("sensor");
			temp.getAttributes().addAttribute("foo");
			temp.getAttributes().addAttribute("bar", "one");
			temp.getAttributes().addAttribute("bar", "two");
			light.getAttributes().addResourceType("light-lux");
			light.getAttributes().addInterfaceDescription("sensor");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Test
	public void testDiscovery() {
		DiscoveryResource discovery = new DiscoveryResource(root);
		String serialized = discovery.discoverTree(root, new LinkedList<String>());
		System.out.println(serialized);
		Assert.assertEquals(serialized,
				"</sensors>;title=\"Sensor Index\"," +
				"</sensors/light>;if=\"sensor\";rt=\"light-lux\"," +
				"</sensors/temp>;if=\"sensor\";foo;rt=\"temperature-c\";bar=\"one two\""
				);
	}
	
	@Test
	public void testDiscoveryFiltering() {
		Request request = Request.newGet();
		request.setURI("/.well-known/core?rt=light-lux");
		
		DiscoveryResource discovery = new DiscoveryResource(root);
		String serialized = discovery.discoverTree(root, request.getOptions().getURIQueries());
		System.out.println(serialized);
		Assert.assertEquals(serialized, 
				"</sensors/light>;if=\"sensor\";rt=\"light-lux\""
				);
	}
	
}

package ch.inf.vs.californium.test;

import java.util.List;

import oracle.jrockit.jfr.Options;

import org.junit.Assert;
import org.junit.Test;

import ch.inf.vs.californium.coap.LinkFormat;
import ch.inf.vs.californium.coap.OptionSet;
import ch.inf.vs.californium.resources.Resource;
import ch.inf.vs.californium.resources.ResourceAttributes;
import ch.inf.vs.californium.resources.ResourceBase;

public class ResourceAttributesTest {

	@Test
	public void testResourceAttributes() {
		
		Resource sensors = new ResourceBase("sensors");
		Resource temp = new ResourceBase("temp");
		Resource light = new ResourceBase("light");
		sensors.add(temp);
		sensors.add(light);
		
		sensors.getAttributes().setTitle("Sensor Index");
		temp.getAttributes().addResourceType("temperature-c");
		temp.getAttributes().addInterfaceDescription("sensor");
		light.getAttributes().addResourceType("light-lux");
		light.getAttributes().addInterfaceDescription("sensor");
		light.getAttributes().addAttribute("foo");
		light.getAttributes().addAttribute("bar", "one");
		light.getAttributes().addAttribute("bar", "two");
		
		// TODO: paths are not correct yet
		
		Assert.assertEquals(LinkFormat.serialize(sensors, new OptionSet().getURIQueries(), true),
				"<sensors>;title=\"Sensor Index\",<light>;if=\"sensor\";foo;rt=\"light-lux\";bar=\"one two\",<temp>;if=\"sensor\";rt=\"temperature-c\"");

	}
	
	// TODO: test filtering
	
}

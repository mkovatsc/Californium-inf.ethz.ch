package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import coap.RemoteResource;
import coap.Resource;

//import coap.Resources;

public class ResourcesTest {
	@Test
	public void TwoResourceTest() {

		// note: order of attributes may change

		String resourceInput1 = "</myUri>,</myUri/something>;rt=\"MyName\";if=\"/someRef/path\";ct=42;sz=10;obs";
		String resourceInput2 = "</sensors>,</sensors/temp>;rt=\"TemperatureC\";ct=41";

		// Build link format string
		String resourceInput = resourceInput1 + "," + resourceInput2;

		// Construct two resources from link format substrings
		// Resource res1 = Resource.fromLinkFormat(resourceInput1);
		// Resource res2 = Resource.fromLinkFormat(resourceInput2);

		// Build resources from assembled link format string
		Resource resource = RemoteResource.newRoot(resourceInput);

		// Check if resources are in hash map
		// assertTrue(resources.hasResource(res1.getResourceName()));
		// assertTrue(resources.hasResource(res2.getResourceName()));

		// Check if link format string equals input
		// String expectedLinkFormat = res1.toLinkFormat() + "," +
		// res2.toLinkFormat();
		// assertEquals(expectedLinkFormat, resources.toLinkFormat());
		assertEquals(resourceInput, resource.toLinkFormat());
	}

}

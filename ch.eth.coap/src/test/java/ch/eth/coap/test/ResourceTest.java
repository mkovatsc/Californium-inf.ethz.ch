package ch.eth.coap.test;

import static org.junit.Assert.*;

import org.junit.Test;

import ch.eth.coap.endpoint.RemoteResource;
import ch.eth.coap.endpoint.Resource;

public class ResourceTest {

	@Test
	public void simpleTest() {
		String input = "</sensors/temp>;ct=41;rt=\"TemperatureC\"";
		Resource root = RemoteResource.newRoot(input);

		Resource res = root.getResource("/sensors/temp");
		assertNotNull(res);

		assertEquals("temp", res.getResourceIdentifier());
		assertEquals(41, res.getContentTypeCode());
		assertEquals("TemperatureC", res.getResourceType());
	}

	@Test
	public void extendedTest() {
		String input = "</myUri/something>;rt=\"MyName\";if=\"/someRef/path\";ct=42;obs;sz=10";
		Resource root = RemoteResource.newRoot(input);

		Resource res = root.getResource("/myUri/something");
		assertNotNull(res);

		assertEquals("something", res.getResourceIdentifier());
		assertEquals("MyName", res.getResourceType());
		assertEquals("/someRef/path", res.getInterfaceDescription());
		assertEquals(42, res.getContentTypeCode());
		assertEquals(10, res.getMaximumSizeEstimate());
	
	}

	@Test
	public void conversionTest() {
		String ref = "</myUri>,</myUri/something>;ct=42;if=\"/someRef/path\";obs;rt=\"MyName\";sz=10";
		Resource res = RemoteResource.newRoot(ref);
		String result = res.toLinkFormat();
		assertEquals(ref, result);
	}
}

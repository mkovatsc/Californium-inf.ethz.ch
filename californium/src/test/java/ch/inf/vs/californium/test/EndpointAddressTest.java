package ch.inf.vs.californium.test;

import java.net.InetAddress;

import junit.framework.Assert;

import org.junit.Test;

import ch.inf.vs.californium.network.EndpointAddress;

/**
 * This test tests the method equals() of the class {@link EndpointAddress}.
 */
public class EndpointAddressTest {

	@Test
	public void testEndpointAddress() throws Exception {
		EndpointAddress e1 = new EndpointAddress(null, 0);
		EndpointAddress e2 = new EndpointAddress(null, 0);
		EndpointAddress e3 = new EndpointAddress(null, 4);
		
		EndpointAddress e4 = new EndpointAddress(InetAddress.getLoopbackAddress(), 77);
		EndpointAddress e5 = new EndpointAddress(InetAddress.getByName("localhost"), 77);
		EndpointAddress e6 = new EndpointAddress(InetAddress.getByName("127.0.0.1"), 77);
		
		/*
		 * Careful:
		 * InetAddress.getByName("localhost") returns 127.0.0.1
		 * InetAddress.getLocalHost() returns 192.168.1.37
		 * ==> they are not the same!
		 */
		
		Assert.assertTrue(e1.equals(e1));
		Assert.assertTrue(e1.equals(e2));
		Assert.assertFalse(e1.equals(e3));
		
		Assert.assertTrue(e4.equals(e5));
		Assert.assertTrue(e4.equals(e6));
		Assert.assertTrue(e5.equals(e6));
	}
	
}

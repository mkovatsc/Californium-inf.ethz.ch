package ch.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.EndpointManager;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.AbstractResource;

public class ObserveTest {

	public static final int SERVER_PORT = 7777;
	public static final String TARGET = "ress";
	public static final String RESPONSE = "hi";
	public static final String URI = "localhost:"+SERVER_PORT+"/"+TARGET;
	
	private Server server;
	private MyResource resource;
	
	@Before
	public void startupServer() {
		System.out.println("\nStart "+getClass().getSimpleName());
		Server.initializeLogger();
		createServer(SERVER_PORT);
	}
	
	@After
	public void shutdownServer() {
		server.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void testObserveLifecycle() throws Exception {
		// setup observe relation
		Request requestA = Request.newGet();
		requestA.setURI(URI);
		requestA.setObserve();
		requestA.send();
		Response resp1 = requestA.waitForResponse(1000);
		assertTrue(resp1.getOptions().hasObserve());
		assertTrue(resource.getObserverCount() == 1);
		assertEquals(resp1.getPayloadString(), resource.currentResponse);
		
		// trigger 3 notification
		for (int i=0;i<3;i++) {
			requestA.setResponse(null);
			resource.changed();
			Response resp2 = requestA.waitForResponse();
			assertTrue(resp2.getOptions().hasObserve());
			assertEquals(resp2.getPayloadString(), resource.currentResponse);
			Thread.sleep(50);
		}
		
		// renew observe relation
		Request requestB = Request.newGet();
		requestB.setURI(URI);
		requestB.setObserve();
		requestB.send();
		Response resp3 = requestB.waitForResponse(100);
		assertTrue(resp3.getOptions().hasObserve());
		assertTrue(resource.getObserverCount() == 1);
		assertEquals(resp3.getPayloadString(), resource.currentResponse);
		
		// trigger 2 notification
		for (int i=0;i<2;i++) {
			requestB.setResponse(null);
			resource.changed();
			Response resp4 = requestB.waitForResponse(100);
			assertTrue(resp4.getOptions().hasObserve());
			assertEquals(resp4.getPayloadString(), resource.currentResponse);
			Thread.sleep(50);
		}
		
		// request A must not receive further notifications (but only request B)
		requestA.setResponse(null);
		resource.changed();
		Response resp5 = requestA.waitForResponse(100);
		assertTrue(resp5 == null); // didn't get another notification
		
		// cancel relation with GET and no observe
		Request requestC = Request.newGet();
		requestC.setURI(URI);
		assertFalse(requestC.getOptions().hasObserve());
		requestC.send(); // without observe option
		Response resp6 = requestC.waitForResponse(100);
		assertFalse(resp6.getOptions().hasObserve());
		assertEquals(resp6.getPayloadString(), resource.currentResponse);
		
		// request B and C must not get any further notifications
		requestB.setResponse(null);
		requestC.setResponse(null);
		resource.changed();
		Response resp7 = requestB.waitForResponse(100);
		Response resp8 = requestC.waitForResponse(100);
		assertTrue(resp7 == null); // didn't get another notification
		assertTrue(resp8 == null); // didn't get another notification
		
		// no observe relations exist anymore
		assertTrue(resource.getObserverCount() == 0);
	}
	
	private void createServer(int port) {
		server = new Server(port);
		resource = new MyResource();
		server.add(resource);
		server.start();
	}
	
	private static class MyResource extends AbstractResource {
		
		private int counter = 0;
		private String currentResponse;
		
		public MyResource() {
			super(TARGET);
			setObservable(true);
		}
		
		@Override
		public void processGET(Exchange exchange) {
			currentResponse = RESPONSE+(counter++);
			exchange.respond(currentResponse);
		}
	}
}

package ch.ethz.inf.vs.californium.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.CoapClient;
import ch.ethz.inf.vs.californium.CoapHandler;
import ch.ethz.inf.vs.californium.CoapObserveRelation;
import ch.ethz.inf.vs.californium.CoapResponse;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class ClientAsynchronousTest {

	public static final String TARGET = "storage";
	public static final String CONTENT_1 = "one";
	public static final String CONTENT_2 = "two";
	public static final String CONTENT_3 = "three";
	public static final String CONTENT_4 = "four";
	public static final String QUERY_UPPER_CASE = "uppercase";
	
	private Server server;
	private int serverPort;
	
	private ResourceBase resource;
	
	private String expected;
	
	private List<String> failed = new CopyOnWriteArrayList<String>();
	private Throwable asyncThrowable = null;
	
	private AtomicInteger notifications = new AtomicInteger();
	
	@Before
	public void startupServer() {
		System.out.println("\nStart "+getClass().getSimpleName());
		NetworkConfig.getStandard()
			.setLong(NetworkConfigDefaults.MAX_TRANSMIT_WAIT, 100);
		createServer();
	}
	
	@After
	public void shutdownServer() {
		server.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void testAsynchronousCall() throws Exception {
		String uri = "coap://localhost:"+serverPort+"/"+TARGET;
		CoapClient client = new CoapClient(uri).useExecutor();
		
		// Check that we get the right content when calling get()
		client.get(new TestHandler("Test 1") {
			@Override public void onLoad(CoapResponse response) {
				assertEquals(CONTENT_1, response.getResponseText());
			}
		});
		Thread.sleep(100);
		
		client.get(new TestHandler("Test 2") {
			@Override public void onLoad(CoapResponse response) {
				assertEquals(CONTENT_1, response.getResponseText());
			}
		});
		Thread.sleep(100);
		
		// Change the content to "two" and check
		client.post(new TestHandler("Test 3") {
			@Override public void onLoad(CoapResponse response) {
				assertEquals(CONTENT_1, response.getResponseText());
			}
		}, CONTENT_2, MediaTypeRegistry.TEXT_PLAIN);
		Thread.sleep(100);
		
		client.get(new TestHandler("Test 4") {
			@Override public void onLoad(CoapResponse response) {
				assertEquals(CONTENT_2, response.getResponseText());
			}
		});
		Thread.sleep(100);
		
		// Observe the resource
		expected = CONTENT_2;
		CoapObserveRelation obs1 = client.observe(new TestHandler("Test Observe") {
			@Override public void onLoad(CoapResponse response) {
				notifications.incrementAndGet();
				String payload = response.getResponseText();
				assertEquals(expected, payload);
				assertEquals(true, response.advanced().getOptions().hasObserve());
			}
		});
		
		Thread.sleep(100);
		resource.changed();
		Thread.sleep(100);
		resource.changed();
		Thread.sleep(100);
		resource.changed();
		
		Thread.sleep(100);
		expected = CONTENT_3;
		client.post(new TestHandler("Test 5") {
			@Override public void onLoad(CoapResponse response) {
				assertEquals(CONTENT_2, response.getResponseText());
			}
		}, CONTENT_3, MediaTypeRegistry.TEXT_PLAIN);
		Thread.sleep(100);
		
		// Try a put and receive a METHOD_NOT_ALLOWED
		client.put(new TestHandler("Test 6") {
			@Override public void onLoad(CoapResponse response) {
				assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
			}
		}, CONTENT_4, MediaTypeRegistry.TEXT_PLAIN);
		
		// Cancel observe relation of obs1 and check that it does no longer receive notifications
		Thread.sleep(100);
		expected = null; // The next notification would now cause a failure
		obs1.reactiveCancel();
		Thread.sleep(100);
		resource.changed();
		
		// Make another post
		Thread.sleep(100);
		client.post(new TestHandler("Test 7") {
			@Override public void onLoad(CoapResponse response) {
				assertEquals(CONTENT_3, response.getResponseText());
			}
		}, CONTENT_4, MediaTypeRegistry.TEXT_PLAIN);
		Thread.sleep(100);
		
		// Try to use the builder and add a query
		new CoapClient.Builder("localhost", serverPort)
			.path(TARGET).query(QUERY_UPPER_CASE).create()
			.get(new TestHandler("Test 8") {
				@Override public void onLoad(CoapResponse response) {
					assertEquals(CONTENT_4.toUpperCase(), response.getResponseText());
				}
			}
		);
		
		// Check that we indeed received 5 notifications
		// 1 from origin GET request, 3 x from changed(), 1 from post()
		Thread.sleep(100);
		Assert.assertEquals(5, notifications.get());
		
		Assert.assertTrue(failed.isEmpty());
		Assert.assertEquals(null, asyncThrowable);
	}
	
	private void assertEquals(Object expected, Object actual) {
		try {
			Assert.assertEquals(expected, actual);
		} catch (Throwable t) {
			t.printStackTrace();
			if (asyncThrowable == null)
				asyncThrowable = t;
		}
	}
	
	private void createServer() {
		// retransmit constantly all 2 seconds
		CoAPEndpoint endpoint = new CoAPEndpoint(7777);
		
		resource = new StorageResource(TARGET, CONTENT_1);
		server = new Server();
		server.add(resource);

		server.addEndpoint(endpoint);
		server.start();
		serverPort = endpoint.getAddress().getPort();
	}
	
	private class StorageResource extends ResourceBase {
		
		private String content;
		
		public StorageResource(String name, String content) {
			super(name);
			this.content = content;
			setObservable(true);
		}
		
		@Override
		public void handleGET(CoapExchange exchange) {
			List<String> queries = exchange.getRequestOptions().getURIQueries();
			String c = content;
			for (String q:queries)
				if (QUERY_UPPER_CASE.equals(q))
					c = content.toUpperCase();
			
			exchange.respond(ResponseCode.CONTENT, c);
		}
		
		@Override
		public void handlePOST(CoapExchange exchange) {
			String old = this.content;
			this.content = exchange.getRequestText();
			exchange.respond(ResponseCode.CHANGED, old);
			changed();
		}
	}
	
	private abstract class TestHandler implements CoapHandler {
		private String name;
		private TestHandler(String name) { this.name = name; }
		@Override public void onError() { failed.add(name); }
	}
}

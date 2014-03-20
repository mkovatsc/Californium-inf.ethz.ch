package ch.ethz.inf.vs.californium.test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.CoapClient;
import ch.ethz.inf.vs.californium.CoapHandler;
import ch.ethz.inf.vs.californium.CoapObserveRelation;
import ch.ethz.inf.vs.californium.CoapResponse;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class ClientSynchronousTest {

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
	
	private AtomicInteger notifications = new AtomicInteger();
	private boolean failed = false;
	
	@Before
	public void startupServer() {
		NetworkConfig.getStandard().setLong(NetworkConfigDefaults.MAX_TRANSMIT_WAIT, 100);
		createServer();
		System.out.println("\nStart "+getClass().getSimpleName() + " on port " + serverPort);
	}
	
	@After
	public void shutdownServer() {
		server.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void testSynchronousCall() throws Exception {
		String uri = "coap://localhost:"+serverPort+"/"+TARGET;
		CoapClient client = new CoapClient(uri).useExecutor();
		
		// Check that we get the right content when calling get()
		String resp1 = client.get().getResponseText();
		Assert.assertEquals(CONTENT_1, resp1);
		
		String resp2 = client.get().getResponseText();
		Assert.assertEquals(CONTENT_1, resp2);
		
		// Change the content to "two" and check
		String resp3 = client.post(CONTENT_2, MediaTypeRegistry.TEXT_PLAIN).getResponseText();
		Assert.assertEquals(CONTENT_1, resp3);
		
		String resp4 = client.get().getResponseText();
		Assert.assertEquals(CONTENT_2, resp4);
		
		// Observe the resource
		expected = CONTENT_2;
		CoapObserveRelation obs1 = client.observeAndWait(
			new CoapHandler() {
				@Override public void onLoad(CoapResponse response) {
					notifications.incrementAndGet();
					String payload = response.getResponseText();
					Assert.assertEquals(expected, payload);
					Assert.assertTrue(response.advanced().getOptions().hasObserve());
				}
				@Override public void onError() {
					failed = true;
					Assert.assertTrue(false);
				}
			});
		Assert.assertFalse(obs1.isCanceled());
		
		Thread.sleep(100);
		resource.changed();
		Thread.sleep(100);
		resource.changed();
		Thread.sleep(100);
		resource.changed();
		
		Thread.sleep(100);
		expected = CONTENT_3;
		String resp5 = client.post(CONTENT_3, MediaTypeRegistry.TEXT_PLAIN).getResponseText();
		Assert.assertEquals(CONTENT_2, resp5);
		
		// Try a put and receive a METHOD_NOT_ALLOWED
		ResponseCode code6 = client.put(CONTENT_4, MediaTypeRegistry.TEXT_PLAIN).getCode();
		Assert.assertEquals(ResponseCode.METHOD_NOT_ALLOWED, code6);
		
		// Cancel observe relation of obs1 and check that it does no longer receive notifications
		Thread.sleep(100);
		expected = null; // The next notification would now cause a failure
		obs1.reactiveCancel();
		Thread.sleep(100);
		resource.changed();
		
		// Make another post
		Thread.sleep(100);
		String resp7 = client.post(CONTENT_4, MediaTypeRegistry.TEXT_PLAIN).getResponseText();
		Assert.assertEquals(CONTENT_3, resp7);
		
		// Try to use the builder and add a query
		String resp8 = new CoapClient.Builder("localhost", serverPort)
			.path(TARGET).query(QUERY_UPPER_CASE).create().get().getResponseText();
		Assert.assertEquals(CONTENT_4.toUpperCase(), resp8);
		
		// Check that we indeed received 5 notifications
		// 1 from origin GET request, 3 x from changed(), 1 from post()
		Thread.sleep(100);
		Assert.assertEquals(5, notifications.get());
		Assert.assertFalse(failed);
	}
	
	private void createServer() {
		// retransmit constantly all 2 seconds
		CoAPEndpoint endpoint = new CoAPEndpoint();
		
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
}

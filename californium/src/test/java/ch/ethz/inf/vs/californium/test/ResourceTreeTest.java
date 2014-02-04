package ch.ethz.inf.vs.californium.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class ResourceTreeTest {

	public static final String RES_A = "A";
	public static final String RES_AA = "AA";
	
	public static final String NAME_1 = "first";
	public static final String NAME_2 = "second";
	public static final String PAYLOAD = "It is freezing";
	
	public static final String CHILD = "child";
	public static final String CHILD_PAYLOAD = "It is too cold";
	
	private Server server;
	private int serverPort;
	
	private ResourceBase resource;
	
	@Before
	public void startupServer() {
		System.out.println("\nStart "+getClass().getSimpleName());
		createServer();
	}
	
	@After
	public void shutdownServer() {
		server.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void testNameChange() throws Exception {
		String base = "coap://localhost:"+serverPort+"/"+RES_A+"/"+RES_AA+"/";
		
		// First check that we reach the resource
		String resp1 = Request.newGet().setURI(base+NAME_1).send().waitForResponse(100).getPayloadString();
		Assert.assertEquals(PAYLOAD, resp1);
		
		// Check that the child of 'first' is also reachable
		String resp2 = Request.newGet().setURI(base+NAME_1+"/"+CHILD).send().waitForResponse(100).getPayloadString();
		Assert.assertEquals(CHILD_PAYLOAD, resp2);
		
		// change the name to 'second'
		resource.setName(NAME_2);
		
		// Check that the resource reacts
		System.out.println("Check that the resource reacts");
		String resp3 = Request.newGet().setURI(base+NAME_2).send().waitForResponse(100).getPayloadString();
		Assert.assertEquals(PAYLOAD, resp3);
		
		// Check that the child of (now) 'second' is also reachable
		System.out.println("Check that the child of (now) 'second' is also reachable");
		String resp4 = Request.newGet().setURI(base+NAME_2+"/"+CHILD).send().waitForResponse(100).getPayloadString();
		Assert.assertEquals(CHILD_PAYLOAD, resp4);
		
		// Check that the resource is not found at the old URI
		System.out.println("Check that the resource is not found at the old URI");
		ResponseCode code1 = Request.newGet().setURI(base+NAME_1).send().waitForResponse(100).getCode();
		Assert.assertEquals(ResponseCode.NOT_FOUND, code1);
		
		// Check that the child of (now) 'second' is not reachable under 'first'
		System.out.println("Check that the child of (now) 'second' is not reachable under 'first'");
		ResponseCode code2 = Request.newGet().setURI(base+NAME_1+"/"+CHILD).send().waitForResponse(100).getCode();
		Assert.assertEquals(ResponseCode.NOT_FOUND, code2);
	}
	
	private void createServer() {
		// retransmit constantly all 2 seconds
		CoAPEndpoint endpoint = new CoAPEndpoint();
		
		resource = new TestResource(NAME_1, PAYLOAD);
		server = new Server();
		server
			.add(new ResourceBase(RES_A)
				.add(new ResourceBase(RES_AA)
					.add(resource
						.add(new TestResource(CHILD, CHILD_PAYLOAD)))));

		server.addEndpoint(endpoint);
		server.start();
		serverPort = endpoint.getAddress().getPort();
	}
	
	private class TestResource extends ResourceBase {
		
		private String payload;
		
		public TestResource(String name, String payload) {
			super(name);
			this.payload = payload;
		}
		
		@Override
		public void handleGET(CoapExchange exchange) {
			exchange.respond(ResponseCode.CONTENT, payload);
		}
	}
}

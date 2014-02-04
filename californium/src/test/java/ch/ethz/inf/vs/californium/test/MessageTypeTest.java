package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This test tests that the message type of responses is correct.
 */
public class MessageTypeTest {

	private static final String SERVER_RESPONSE = "server responds hi";
	private static final String ACC_RESOURCE = "acc-res";
	private static final String NO_ACC_RESOURCE = "no-acc-res";
	
	private Server server;
	private int serverPort;
	
	@Before
	public void setupServer() {
		try {
			System.out.println("\nStart "+getClass().getSimpleName());
			EndpointManager.clear();
			
			CoAPEndpoint endpoint = new CoAPEndpoint();
			
			server = new Server();
			server.addEndpoint(endpoint);
			server.add(new ResourceBase(ACC_RESOURCE) {
				public void handlePOST(CoapExchange exchange) {
					System.out.println("gotit");
					exchange.accept();
					exchange.respond(SERVER_RESPONSE);
				}
			});
			server.add(new ResourceBase(NO_ACC_RESOURCE) {
				public void handlePOST(CoapExchange exchange) {
					exchange.respond(SERVER_RESPONSE);
				}
			});
			server.start();
			serverPort = endpoint.getAddress().getPort();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@After
	public void after() {
		server.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void testNonConfirmable() throws Exception {
		// send request
		Request req2acc = new Request(Code.POST);
		req2acc.setConfirmable(false);
		req2acc.setURI("localhost:"+serverPort+"/"+ACC_RESOURCE);
		req2acc.setPayload("client says hi".getBytes());
		req2acc.send();
		
		// receive response and check
		Response response = req2acc.waitForResponse(100);
		assertNotNull(response);
		assertEquals(response.getPayloadString(), SERVER_RESPONSE);
		assertEquals(response.getType(), Type.NON);
		
		Request req2noacc = new Request(Code.POST);
		req2noacc.setConfirmable(false);
		req2noacc.setURI("coap://localhost:"+serverPort+"/"+NO_ACC_RESOURCE);
		req2noacc.setPayload("client says hi".getBytes());
		req2noacc.send();
		
		// receive response and check
		response = req2noacc.waitForResponse(100);
		assertNotNull(response);
		assertEquals(response.getPayloadString(), SERVER_RESPONSE);
		assertEquals(response.getType(), Type.NON);
	}
	
	@Test
	public void testConfirmable() throws Exception {
		// send request
		Request req2acc = new Request(Code.POST);
		req2acc.setConfirmable(true);
		req2acc.setURI("localhost:"+serverPort+"/"+ACC_RESOURCE);
		req2acc.setPayload("client says hi".getBytes());
		req2acc.send();
		
		// receive response and check
		Response response = req2acc.waitForResponse(100);
		assertNotNull(response);
		assertEquals(response.getPayloadString(), SERVER_RESPONSE);
		assertEquals(response.getType(), Type.CON);
		
		Request req2noacc = new Request(Code.POST);
		req2noacc.setConfirmable(true);
		req2noacc.setURI("coap://localhost:"+serverPort+"/"+NO_ACC_RESOURCE);
		req2noacc.setPayload("client says hi".getBytes());
		req2noacc.send();
		
		// receive response and check
		response = req2noacc.waitForResponse(100);
		assertNotNull(response);
		assertEquals(response.getPayloadString(), SERVER_RESPONSE);
		assertEquals(response.getType(), Type.ACK);
	}
	
}

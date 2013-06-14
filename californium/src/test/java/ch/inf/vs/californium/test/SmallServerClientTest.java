package ch.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.inf.vs.californium.MessageDeliverer;
import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointManager;
import ch.inf.vs.californium.network.Exchange;

/**
 * This is a small test that tests the exchange of one request and one response.
 */
public class SmallServerClientTest {

	private static String SERVER_RESPONSE = "server responds hi";
	
	@Before
	public void initLogger() {
		System.out.println("\nStart "+getClass().getSimpleName());
		EndpointManager.clear();
	}
	
	@After
	public void after() {
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void testNonconfirmable() throws Exception {
		int serverport = 7777;
		createSimpleServer(serverport);
		
		// send request
		Request request = new Request(CoAP.Code.POST);
		request.setConfirmable(false);
		request.setDestination(InetAddress.getLocalHost());
		request.setDestinationPort(serverport);
		request.setPayload("client says hi".getBytes());
		request.send();
		
		// receive response and check
		Response response = request.waitForResponse(1000);
		assertNotNull(response);
		assertEquals(response.getPayloadString(), SERVER_RESPONSE);
	}
	
	
	private void createSimpleServer(int port) {
		Server server = new Server();
		server.addEndpoint(new Endpoint(port));
		server.setMessageDeliverer(new MessageDeliverer() {
			@Override
			public void deliverRequest(Exchange exchange) {
				System.out.println("server has received request");
				exchange.accept();
				try { Thread.sleep(500); } catch (Exception e) {}
				Response response = new Response(ResponseCode.CONTENT);
				response.setConfirmable(false);
				response.setPayload(SERVER_RESPONSE.getBytes());
				exchange.respond(response);
			}
			@Override
			public void deliverResponse(Exchange exchange, Response response) { }
		});
		server.start();
	}
}

package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.MessageDeliverer;
import ch.ethz.inf.vs.californium.server.Server;

/**
 * This is a small test that tests the exchange of one request and one response.
 */
public class SmallServerClientTest {

	private static String SERVER_RESPONSE = "server responds hi";
	
	private int serverPort;
	
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
		createSimpleServer();
		
		// send request
		Request request = new Request(CoAP.Code.POST);
		request.setConfirmable(false);
		request.setDestination(InetAddress.getLocalHost());
		request.setDestinationPort(serverPort);
		request.setPayload("client says hi".getBytes());
		request.send();
		
		// receive response and check
		Response response = request.waitForResponse(1000);
		assertNotNull(response);
		assertEquals(response.getPayloadString(), SERVER_RESPONSE);
	}
	
	
	private void createSimpleServer() {
		CoAPEndpoint endpoint = new CoAPEndpoint();
		Server server = new Server();
		server.addEndpoint(endpoint);
		server.setMessageDeliverer(new MessageDeliverer() {
			@Override
			public void deliverRequest(Exchange exchange) {
				System.out.println("server has received request");
				exchange.sendAccept();
				try { Thread.sleep(500); } catch (Exception e) {}
				Response response = new Response(ResponseCode.CONTENT);
				response.setConfirmable(false);
				response.setPayload(SERVER_RESPONSE.getBytes());
				exchange.sendResponse(response);
			}
			@Override
			public void deliverResponse(Exchange exchange, Response response) { }
		});
		server.start();
		serverPort = endpoint.getAddress().getPort();
	}
}

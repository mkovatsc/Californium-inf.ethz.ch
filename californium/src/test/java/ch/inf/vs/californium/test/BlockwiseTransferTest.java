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
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.StackConfiguration;

/**
 * This test sets the maximum message size and the default block size to 32
 * bytes and sends messages blockwise. All four combinations with short and long
 * requests and responses are tested.
 */
public class BlockwiseTransferTest {

	private static final int SERVER_PORT = 7777;
	private static final String SHORT_REQUEST  = "<Short request>";
	private static final String LONG_REQUEST   = "<Long request xxxxx>".replace("x", "ABCDEFGHIJKLMNOPQRSTUVWXYZ ");
	private static final String SHORT_RESPONSE = "<Short response>";
	private static final String LONG_RESPONSE  = "<Long response xxxxx>".replace("x", "ABCDEFGHIJKLMNOPQRSTUVWXYZ ");
	
	private boolean request_short = true;
	private boolean respond_short = true;
	
	private Server server;
	
	@Before
	public void setupServer() {
		Server.initializeLogger();
		server = createSimpleServer(SERVER_PORT);
	}
	
	@After
	public void shutdownServer() {
		server.destroy();
	}
	
	@Test
	public void test_all() throws Exception {
		test_short_short();
		test_long_short();
		test_short_long();
		test_long_long();
	}
	
	public void test_short_short() throws Exception {
		request_short = true;
		respond_short = true;
		executeRequest();
	}
	
	public void test_long_short() throws Exception {
		request_short = false;
		respond_short = true;
		executeRequest();
	}
	
	public void test_short_long() throws Exception {
		request_short = true;
		respond_short = false;
		executeRequest();
	}
	
	public void test_long_long() throws Exception {
		request_short = false;
		respond_short = false;
		executeRequest();
	}
	
	private void executeRequest() throws Exception {
		Request request = new Request(CoAP.Code.POST);
		request.setDestination(InetAddress.getLocalHost());
		request.setDestinationPort(SERVER_PORT);
		if (request_short)
			request.setPayload(SHORT_REQUEST.getBytes());
		else request.setPayload(LONG_REQUEST.getBytes());
		request.send();
		
		// receive response and check
		Response response = request.waitForResponse(1000);
		assertNotNull(response);
		String payload = response.getPayloadString();
		if (respond_short)
			assertEquals(payload, SHORT_RESPONSE);
		else assertEquals(payload, LONG_RESPONSE);
		System.out.println("  Client correctly received "+payload);
	}
	
	private Server createSimpleServer(int port) {
		Server server = new Server();
		StackConfiguration config = new StackConfiguration();
		config.setDefaultBlockSize(32);
		config.setMaxMessageSize(32);
		server.addEndpoint(new Endpoint(port, config));
		server.setMessageDeliverer(new MessageDeliverer() {
			@Override
			public void deliverRequest(Exchange exchange) {
				String payload = exchange.getRequest().getPayloadString();
				if (request_short)
					assertEquals(payload, SHORT_REQUEST);
				else assertEquals(payload, LONG_REQUEST);
				System.out.println("  Server correctly received "+payload);
					
				Response response = new Response(ResponseCode.CONTENT);
				if (respond_short)
					response.setPayload(SHORT_RESPONSE.getBytes());
				else response.setPayload(LONG_RESPONSE.getBytes());
				exchange.respond(response);
			}
			@Override
			public void deliverResponse(Exchange exchange, Response response) { }
		});
		server.start();
		return server;
	}
	
}

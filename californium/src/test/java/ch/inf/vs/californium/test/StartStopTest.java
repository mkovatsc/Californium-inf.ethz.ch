package ch.inf.vs.californium.test;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.AbstractResource;

/**
 * This test tests whether we are able to properly start, stop and then again
 * start a server. We create two servers each with one resource. Both use an
 * endpoint that both listen on the same port when started. Therefore, they
 * should not be started at the same time. First, we start server 1 and send a
 * request and validate the response come from server 1. Second, we stop server
 * 1, start server 2 and again send a new request and validate that server 2
 * responds. Finally, we stop and destroy both servers.
 */
public class StartStopTest {

	public static final String SERVER_1_RESPONSE = "This is server one";
	public static final String SERVER_2_RESPONSE = "This is server two";
	
	private Server server1, server2;
	
	@Before
	public void setupServers() {
		Server.initializeLogger();
		
		server1 = new Server(7777);
		server1.add(new AbstractResource("ress") {
			@Override public void processGET(Exchange exchange) {
				exchange.respond(SERVER_1_RESPONSE);
			}
		});
		
		server2 = new Server(7777);
		server2.add(new AbstractResource("ress") {
			@Override public void processGET(Exchange exchange) {
				exchange.respond(SERVER_2_RESPONSE);
			}
		});
	}
	
	@After
	public void shutdownServers() {
		if (server1 != null) server1.destroy();
		if (server2 != null) server2.destroy();
	}
	
	@Test
	public void test() throws Exception {
		server1.start();
		sendRequestAndExpect(SERVER_1_RESPONSE);
		
		for (int i=0;i<3;i++) {
			server1.stop();
			server2.start();
			sendRequestAndExpect(SERVER_2_RESPONSE);
			
			server2.stop();
			server1.start();
			sendRequestAndExpect(SERVER_1_RESPONSE);
		}
		server1.stop();
	}
	
	private void sendRequestAndExpect(String expected) throws Exception {
		Request request = Request.newGet();
		request.setURI("localhost:7777/ress");
		String response = request.send().waitForResponse(1000).getPayloadString();
		Assert.assertEquals(expected, response);
	}
	
}

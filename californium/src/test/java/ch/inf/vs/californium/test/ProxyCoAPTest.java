package ch.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.AbstractResource;
import ch.inf.vs.californium.resources.proxy.ProxyCoapClientResource;

public class ProxyCoAPTest {

	private static final int PROXY_PORT = 7777;
	private static final int TARGET_PORT = 8888;
	private static final String PROXY = "proxy";
	private static final String TARGET = "target";
	private static final String TARGET_RESPONSE = "ABC from target 123";

	private Server server_proxy;
	private Server server_target;
	
	@Before
	public void setupServers() {
		Server.initializeLogger();
		server_proxy = new Server(PROXY_PORT);
		server_proxy.add(new ProxyCoapClientResource(PROXY));
		server_proxy.start();
		
		server_target = new Server(TARGET_PORT);
		server_target.add(new AbstractResource(TARGET) {
			public void processGET(Exchange exchange) {
				exchange.respond(TARGET_RESPONSE);
			}
		});
		server_target.start();
	}
	
	@After
	public void shutdownServer() {
		server_proxy.destroy();
		server_target.destroy();
	}
	

	@Test
	public void test() throws Exception {
		Request request = new Request(Code.GET);
		request.setURI("coap://localhost:"+PROXY_PORT + "/" + PROXY);
		request.getOptions().setProxyURI("coap://localhost:"+TARGET_PORT + "/" + TARGET);
		request.send();
		
		Response response = request.waitForResponse(1000);
		assertNotNull(response);
		
		String payload = response.getPayloadString();
		assertEquals(payload, TARGET_RESPONSE);
	}
	
}

package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.Server;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.resources.ResourceBase;
import ch.ethz.inf.vs.californium.resources.proxy.ProxyCoapClientResource;

public class ProxyCoAP2CoAPTest {
	
	private static final String PROXY = "proxy";
	private static final String TARGET = "target";
	private static final String TARGET_RESPONSE = "ABC from target 123";

	private Server server_proxy;
	private Server server_target;
	
	private int targetPort;
	private int proxyPort;
	
	@Before
	public void setupServers() {
		System.out.println("\nStart "+getClass().getSimpleName());
		EndpointManager.clear();
		Endpoint proxyEndpoint = new Endpoint();
		server_proxy = new Server();
		server_proxy.addEndpoint(proxyEndpoint);
		server_proxy.add(new ProxyCoapClientResource(PROXY));
		server_proxy.start();
		
		Endpoint targetEndpoint = new Endpoint();
		server_target = new Server();
		server_target.addEndpoint(targetEndpoint);
		server_target.add(new ResourceBase(TARGET) {
			public void processRequest(Exchange exchange) {
				exchange.respond(TARGET_RESPONSE);
			}
		});
		server_target.start();
		
		proxyPort = proxyEndpoint.getAddress().getPort();
		targetPort = targetEndpoint.getAddress().getPort();
	}
	
	@After
	public void shutdownServer() {
		server_proxy.destroy();
		server_target.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	

	@Test
	public void test() throws Exception {
		Request request = new Request(Code.GET);
		request.setType(Type.NON);
		request.setURI("coap://localhost:"+proxyPort + "/" + PROXY);
		request.getOptions().setProxyURI("coap://localhost:"+targetPort + "/" + TARGET);
		request.send();
		
		Response response = request.waitForResponse(1000);
		assertNotNull(response);
		
		String payload = response.getPayloadString();
		assertEquals(payload, TARGET_RESPONSE);
	}
	
}

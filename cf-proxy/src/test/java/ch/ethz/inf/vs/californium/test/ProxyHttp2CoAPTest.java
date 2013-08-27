package ch.ethz.inf.vs.californium.test;

import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.Server;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.proxy.DirectProxyCoAPResolver;
import ch.ethz.inf.vs.californium.proxy.ProxyHttpServer;
import ch.ethz.inf.vs.californium.resources.ResourceBase;
import ch.ethz.inf.vs.californium.resources.proxy.ProxyCoapClientResource;

/**
 * URLConnection is very buggy.
 */
//@Ignore
public class ProxyHttp2CoAPTest {

	private static final int WAIT_TIME_FOR_USER = 100000;
	
	private static final int HTTP_PORT = 8080;
	private static final String HTTP_URI = "http://localhost:"+HTTP_PORT+"/proxy/";
	private static final String COAP_HOST = "coap://localhost";
	private static final String CoAP_TARGET = "test";
	private static final String RESPONSE = "CoAP response successfully responded";
	
	private Server server;
	private int coapPort;
	private Semaphore semaphore = new Semaphore(0);
	
	@Before
	public void setupServers() {
		try {
			System.out.println("\nStart "+getClass().getSimpleName());
			EndpointManager.clear();
			
			NetworkConfig.getStandard()
				.set(NetworkConfigDefaults.UDP_CONNECTOR_RECEIVER_THREAD_COUNT, 1)
				.set(NetworkConfigDefaults.UDP_CONNECTOR_SENDER_THREAD_COUNT, 1);
			
			Endpoint endpoint = new Endpoint(7777);
			server = new Server();
			server.addEndpoint(endpoint);
			server.add(new ResourceBase(CoAP_TARGET) {
				public void processGET(Exchange exchange) {
					exchange.respond(RESPONSE);
				}
			});
			ProxyCoapClientResource coap2coap = new ProxyCoapClientResource();
			server.add(coap2coap);
			server.start();
			
			ProxyHttpServer proxyHttp = new ProxyHttpServer(HTTP_PORT) {
				@Override public void handleRequest(Request request) {
					super.handleRequest(request);
					stopThisTest();
				} };
			proxyHttp.setProxyCoapResolver(new DirectProxyCoAPResolver(coap2coap));
			
			coapPort = endpoint.getAddress().getPort();
			System.out.println("CoAP server listening on port "+coapPort);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@After
	public void shutdownServer() {
		try {
			server.destroy();
			System.out.println("End "+getClass().getSimpleName());
			Thread.sleep(100);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Test
	public void testHttp2CoAP() throws Exception {
		Thread.sleep(100); // make sure the HTTP server has started
		String url = HTTP_URI + COAP_HOST + ":" + coapPort + "/" + CoAP_TARGET;
		System.out.println(
				"-----------------------------------" + "\n" +
				"This test autoamically resumes when we receive an HTTP request or after " + (WAIT_TIME_FOR_USER / 1000) + " seconds." + "\n" +
				"Insert into your browser: "+url);
		// Wait until the user has entered the url in its browser or some time has elapsed
		new Thread() { public void run() { 
			sleepUninterrupted(WAIT_TIME_FOR_USER);
			System.out.println("Proxy server test stops now (after "+(WAIT_TIME_FOR_USER/1000)+" seconds)");
			semaphore.release();
		} }.start();
		semaphore.acquire();
		// go to next test
	}
	
	/*
	 * Little hack to continue with the next test, after we have received an
	 * HTTP request.
	 */
	private void stopThisTest() {
		// wait until the proxy server certainly has responded to the request
		sleepUninterrupted(500);
		System.out.println("We have received an HTTP request");
		semaphore.release();
	}
	
	private void sleepUninterrupted(long millis) {
		try { Thread.sleep(millis); } catch (InterruptedException e) { e.printStackTrace(); }
	}
}

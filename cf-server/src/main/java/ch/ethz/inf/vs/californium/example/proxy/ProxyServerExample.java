package ch.ethz.inf.vs.californium.example.proxy;

import java.io.IOException;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.Server;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.proxy.DirectProxyCoAPResolver;
import ch.ethz.inf.vs.californium.proxy.ProxyCacheResource;
import ch.ethz.inf.vs.californium.proxy.ProxyHttpServer;
import ch.ethz.inf.vs.californium.resources.ResourceBase;
import ch.ethz.inf.vs.californium.resources.proxy.ForwardingResource;
import ch.ethz.inf.vs.californium.resources.proxy.ProxyCoapClientResource;
import ch.ethz.inf.vs.californium.resources.proxy.ProxyHttpClientResource;

/**
 * Http2CoAP: Insert in browser:
 *     URI: http://localhost:8080/proxy/coap://localhost:5683/targetA
 *     or to send from CoAP2CoAP to third server
 *     URI: http://localhost:8080/proxy/coap://localhost:7777/targetB
 * 
 * CoAP2CoAP: Insert in Copper:
 *     URI: coap://localhost:5683/coap2coap
 *     Proxy: coap://localhost:5683/targetA
 *
 * CoAP2Http: Insert in Copper:
 *     URI: coap://localhost:5683/coap2http
 *     Proxy: http://lantersoft.ch/robots.txt
 */
public class ProxyServerExample {

	// TODO: Move to project cf-proxy
	// It is not there yet because the tests in that project still need to be
	// converted to the new Cf version
	
	private Server targetServerA;
	private Server targetServerB;
	
	public ProxyServerExample() throws IOException {
		ForwardingResource coap2coap = new ProxyCoapClientResource("coap2coap");
		ForwardingResource coap2http = new ProxyHttpClientResource("coap2http");
		
		// Create CoAP Server on 5683 with proxy resources form CoAP to CoAP and HTTP
		targetServerA = new Server(5683);
		targetServerA.add(coap2coap);
		targetServerA.add(coap2http);
		targetServerA.add(new TargetResource("targetA"));
		targetServerA.start();
		
		// Create another CoAP Server on port 7777
		targetServerB = new Server(7777);
		targetServerB.add(new TargetResource("targetB"));
		targetServerB.start();
		
		ProxyHttpServer httpServer = new ProxyHttpServer(8080);
		httpServer.setProxyCoapResolver(new DirectProxyCoAPResolver(coap2coap));
	}
	
	/**
	 * A simple resource that responds to GET requests with a small response
	 * containing the resource's name.
	 */
	private static class TargetResource extends ResourceBase {
		
		public TargetResource(String name) {
			super(name);
		}
		
		@Override
		public void processGET(Exchange exchange) {
			exchange.respond("Response from " + getName());
		}
	}
	
	public static void main(String[] args) throws Exception {
		new ProxyServerExample();
	}
}

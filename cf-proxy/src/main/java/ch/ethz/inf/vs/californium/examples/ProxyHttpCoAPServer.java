package ch.ethz.inf.vs.californium.examples;

import java.io.IOException;

import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.proxy.DirectProxyCoAPResolver;
import ch.ethz.inf.vs.californium.proxy.ProxyHttpServer;
import ch.ethz.inf.vs.californium.resources.proxy.ForwardingResource;
import ch.ethz.inf.vs.californium.resources.proxy.ProxyCoapClientResource;
import ch.ethz.inf.vs.californium.resources.proxy.ProxyHttpClientResource;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * Http2CoAP: Insert in browser:
 *     URI: http://localhost:8080/proxy/coap://localhost:5683/targetA
 * 
 * CoAP2CoAP: Insert in Copper:
 *     URI: coap://localhost:5683/coap2coap
 *     Proxy: coap://localhost:5683/targetA
 *
 * CoAP2Http: Insert in Copper:
 *     URI: coap://localhost:5683/coap2http
 *     Proxy: http://lantersoft.ch/robots.txt
 */
public class ProxyHttpCoAPServer {

	private Server targetServerA;
	private Server targetServerB;
	
	public ProxyHttpCoAPServer() throws IOException {
		ForwardingResource coap2coap = new ProxyCoapClientResource("coap2coap");
		ForwardingResource coap2http = new ProxyHttpClientResource("coap2http");
		
		// Create CoAP Server on 5683 with proxy resources form CoAP to CoAP and HTTP
		targetServerA = new Server(5683);
		targetServerA.add(coap2coap);
		targetServerA.add(coap2http);
		targetServerA.add(new TargetResource("targetA"));
		targetServerA.start();
		
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
		public void handleGET(Exchange exchange) {
			exchange.respond("Response from " + getName());
		}
	}
	
	public static void main(String[] args) throws Exception {
		new ProxyHttpCoAPServer();
	}

}

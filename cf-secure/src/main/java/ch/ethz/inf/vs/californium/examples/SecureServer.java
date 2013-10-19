package ch.ethz.inf.vs.californium.examples;

import java.net.InetSocketAddress;

import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;
import ch.ethz.inf.vs.scandium.DTLSConnector;

public class SecureServer {
	
	public static final int DTLS_PORT = 5685; // TBD

	public static void main(String[] args) {
		System.out.println("Start secure server and bind to port "+DTLS_PORT);
		InetSocketAddress address = new InetSocketAddress(DTLS_PORT);
		
		Server server = new Server();
		server.add(new ResourceBase("hello") {
			
			@Override
			public void handleGET(Exchange exchange) {
				Response response = new Response(ResponseCode.CONTENT);
				response.setPayload("hello security");
				respond(exchange, response);
			}
		});
		server.addEndpoint(new CoAPEndpoint(
				new DTLSConnector(address), NetworkConfig.getStandard()));
		server.start();
		
		System.out.println("Secure CoAP server powered by Scandium (Sc) is listening on port "+DTLS_PORT);
	}

}

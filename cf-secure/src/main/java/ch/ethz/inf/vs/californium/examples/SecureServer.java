package ch.ethz.inf.vs.californium.examples;

import java.net.InetSocketAddress;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.scandium.DTLSConnector;

public class SecureServer {
	
	public static final int DTLS_PORT = NetworkConfig.getStandard().getInt(NetworkConfigDefaults.DEFAULT_COAP_PORT);

	public static void main(String[] args) {
		
		Server server = new Server();
		server.add(new ResourceBase("secure") {	
				@Override
				public void handleGET(CoapExchange exchange) {
					exchange.respond(ResponseCode.CONTENT, "hello security");
				}
			});
        // ETSI Plugtest environment
//		server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress("::1", DTLS_PORT)), NetworkConfig.getStandard()));
//		server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress("127.0.0.1", DTLS_PORT)), NetworkConfig.getStandard()));
//		server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress("2a01:c911:0:2010::10", DTLS_PORT)), NetworkConfig.getStandard()));
//		server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress("10.200.1.2", DTLS_PORT)), NetworkConfig.getStandard()));
		server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress(DTLS_PORT)), NetworkConfig.getStandard()));
		server.start();
		
		System.out.println("Secure CoAP server powered by Scandium (Sc) is listening on port "+DTLS_PORT);
	}

}

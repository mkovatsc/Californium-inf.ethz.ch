package ch.ethz.inf.vs.californium.examples;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;
import ch.ethz.inf.vs.scandium.DTLSConnector;

public class SecureServer {
	
	private static final Logger Log = CalifonriumLogger.getLogger(SecureServer.class);
	
	public static final int DTLS_PORT = 5684;

	public static void main(String[] args) {
		
		System.out.println("Start secure server and bind to port "+DTLS_PORT);
		
		Server server = new Server();
		server.add(new ResourceBase("secure") {	
				@Override
				public void handleGET(CoapExchange exchange) {
					exchange.respond(ResponseCode.CONTENT, "hello security");
				}
			});
        server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress(DTLS_PORT)), NetworkConfig.getStandard()));
		server.start();
		
		System.out.println("Secure CoAP server powered by Scandium (Sc) is listening on port "+DTLS_PORT);
		
		Log.setLevel(Level.INFO);
	}

}

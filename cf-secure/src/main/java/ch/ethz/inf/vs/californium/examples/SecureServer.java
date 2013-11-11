package ch.ethz.inf.vs.californium.examples;

import java.net.InetSocketAddress;

import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.scandium.DTLSConnector;

public class SecureServer {
	
	public static final int DTLS_PORT = 5684; // TBD

	public static void main(String[] args) {
		System.out.println("Start secure server and bind to port "+DTLS_PORT);
		InetSocketAddress address = new InetSocketAddress(DTLS_PORT);
		
		Server server = new Server();
		server.addEndpoint(new CoAPEndpoint(
				new DTLSConnector(address), NetworkConfig.getStandard()));
		server.start();
		
		System.out.println("Secure CoAP server is listening on port "+DTLS_PORT);
		System.out.println("(Note that this feature is not completely implemented yet)");
	}

}

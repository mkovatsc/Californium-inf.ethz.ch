package test;

import ch.inf.vs.californium.MessageDeliverer;
import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.NetworkConfig;
import ch.inf.vs.californium.network.connector.Connector;
import ch.inf.vs.californium.network.connector.DTLSConnector;

public class TestServer {
	
	public static void main(String[] args) throws Exception {
		Server.initializeLogger();
		System.out.println("start server");
		Server server = new Server();
		EndpointAddress address = new EndpointAddress(null, 7777);
		Connector dtlscon = new DTLSConnector(address);
		server.addEndpoint(new Endpoint(dtlscon, address, new NetworkConfig()));
		server.setMessageDeliverer(new MessageDeliverer() {
			@Override
			public void deliverRequest(Exchange exchange) {
				System.out.println("server has received request");
//				exchange.accept();
//				try { Thread.sleep(500); } catch (Exception e) {}
				Response response = new Response(ResponseCode.CONTENT);
				response.setConfirmable(false);
				response.setPayload("ress sais hi");
				exchange.respond(response);
			}
			@Override
			public void deliverResponse(Exchange exchange, Response response) { }
		});
		server.start();
	}
	
}

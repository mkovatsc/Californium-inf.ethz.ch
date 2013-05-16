package ch.inf.vs.californium;

import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.Exchange;

public class TestServer {

	public static void main(String[] args) {
		System.out.println("Start server");
		Server.initializeLogger();
		Server server = new Server();
		server.addEndpoint(new Endpoint(7777));
		server.setMessageDeliverer(new MessageDeliverer() {
			@Override
			public void deliverRequest(Exchange exchange) {
				String payload = exchange.getRequest().getPayloadString();
				System.out.println("  TestServer has received request: "+payload);
				exchange.accept();
				try { Thread.sleep(500); } catch (Exception e) {}
				Response response = new Response(ResponseCode.CONTENT);
				response.setPayload("Content from server is also very very large 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789".getBytes());
				System.out.println("  respond with: "+response.getPayloadString());
				exchange.respond(response);
			}
			
			@Override
			public void deliverResponse(Exchange exchange, Response response) { }
		});
		server.start();
	}
	
}

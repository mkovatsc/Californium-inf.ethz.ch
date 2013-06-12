package ch.inf.vs.californium.debug;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.AbstractResource;
import ch.inf.vs.californium.resources.Resource;

public class TestServer {

	public static void main(String[] args) {
		System.out.println("Start server");
		Server.initializeLogger();
		Server server = new Server();
		Resource eins = new AbstractResource("eins") {};
		Resource zwei = new AbstractResource("zwei") {};
		Resource drei = new AbstractResource("drei") {
			
			public void processPOST(Exchange exchange) {
				Response response = new Response(ResponseCode.CONTENT);
				response.setPayload("Resource drei processed post".getBytes());
				exchange.respond(response);
			}
			
		};
		server.add(eins);
		eins.add(zwei);
		zwei.add(drei);
		
		server.addEndpoint(new Endpoint(7777));
		
		server.start();
	}
	
}

/*
 * server.setMessageDeliverer(new MessageDeliverer() {
			@Override
			public void deliverRequest(Exchange exchange) {
				String payload = exchange.getRequest().getPayloadString();
				System.out.println("  TestServer has received request: "+payload);
				System.out.println("Deliver to "+exchange.getRequest().getOptions().getURIPaths());
				exchange.accept();
				try { Thread.sleep(500); } catch (Exception e) {}
				Response response = new Response(ResponseCode.CONTENT);
				response.setPayload("Content from server is also very very large 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789".getBytes());
				System.out.println("  respond with: "+response.getPayloadString());
				exchange.respond(response);
			}
			
			@Override
			public void deliverResponse(Exchange exchange, Response response) { }
		});*/

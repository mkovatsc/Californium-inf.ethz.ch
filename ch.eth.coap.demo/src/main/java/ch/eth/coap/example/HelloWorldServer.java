package ch.eth.coap.example;

import java.net.SocketException;

import ch.eth.coap.coap.CodeRegistry;
import ch.eth.coap.coap.GETRequest;

import ch.eth.coap.endpoint.LocalEndpoint;
import ch.eth.coap.endpoint.LocalResource;

public class HelloWorldServer extends LocalEndpoint {
	
	/*
	 * Definition of the Hello-World Resource
	 * 
	 */
	class HelloWorldResource extends LocalResource {

		public HelloWorldResource() {

			// set resource identifier
			super("helloWorld"); 
			
			// set display name
			setResourceTitle("Hello-World Resource");
		}

		@Override
		public void performGET(GETRequest request) {

			// respond to the request
			request.respond(CodeRegistry.RESP_CONTENT, "Hello World!");
		}
	}
	
	/*
	 * Constructor for a new Hello-World server. Here, the resources
	 * of the server are initialized.
	 * 
	 */
	public HelloWorldServer() throws SocketException {
		
		// provide an instance of a Hello-World resource
		addResource(new HelloWorldResource());
	}

	/*
	 * Application entry point.
	 * 
	 */
	public static void main(String[] args) {
		
		try {
			
			// create server
			HelloWorldServer server = new HelloWorldServer();
			
			System.out.println("Server listening on port " + server.port());
			
		} catch (SocketException e) {
			
			System.err.println("Failed to initialize server: " + e.getMessage());
		}
	}
}

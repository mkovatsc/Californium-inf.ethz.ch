package ch.ethz.inf.vs.californium.examples.resources;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;

/*
 * This class implements a 'hello world' resource for demonstration purposes.
 * 
 * Defines a resource that returns "Hello World!" on a GET request.
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class HelloWorldResource extends LocalResource {

	public HelloWorldResource() {
		super("helloWorld");
		setResourceTitle("GET a friendly greeting!");
		setResourceType("HelloWorldDisplayer");
	}

	@Override
	public void performGET(GETRequest request) {

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);

		// set payload
		response.setPayload("Hello World! Some umlauts: äöü");
		// complete the request
		request.respond(response);
	}
}

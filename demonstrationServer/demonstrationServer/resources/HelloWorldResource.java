package demonstrationServer.resources;

import coap.CodeRegistry;
import coap.GETRequest;
import coap.ReadOnlyResource;
import coap.Response;

/*
 * This class implements a 'hello world' resource for demonstration purposes.
 * 
 * Defines a resource that returns "Hello World!" on a GET request.
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class HelloWorldResource extends ReadOnlyResource {

	public HelloWorldResource() {
		super("helloWorld");
		setResourceType("GET a friendly greeting!");
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

package ch.eth.coap.demo.resources;

import ch.eth.coap.coap.CodeRegistry;
import ch.eth.coap.coap.POSTRequest;
import ch.eth.coap.endpoint.LocalResource;

/*
 * This class implements a 'toUpper' resource for demonstration purposes.
 * 
 * Defines a resource that returns a POSTed string in all uppercase letters
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class ToUpperResource extends LocalResource {

	public ToUpperResource() {
		super("toUpper");
		setResourceTitle("POST text here to convert it to uppercase");
		setResourceType("UppercaseConverter");
	}

	@Override
	public void performPOST(POSTRequest request) {

		// retrieve text to convert from payload
		String text = request.getPayloadString();

		// complete the request
		request.respond(CodeRegistry.V3_RESP_OK, text.toUpperCase());
	}
}

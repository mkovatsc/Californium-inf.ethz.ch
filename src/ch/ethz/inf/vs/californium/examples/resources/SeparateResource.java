package ch.ethz.inf.vs.californium.examples.resources;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;


/*
 * This class implements a 'separate' resource for demonstration purposes.
 * 
 * Defines a resource that returns a response in a separate CoAP Message
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class SeparateResource extends LocalResource {

	public SeparateResource() {
		super("separate");
		setResourceTitle("GET a response in a separate CoAP Message");
		setResourceType("SepararateResponseTester");
	}

	@Override
	public void performGET(GETRequest request) {

		// we know this stuff may take longer...
		// promise the client that this request will be acted upon
		// by sending an Acknowledgement
		request.accept();

		// do the time-consuming computation
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);

		// set payload
		response.setPayload("This message was sent by a separate response.\n"
				+ "Your client will need to acknowledge it, otherwise it will be retransmitted.");

		// complete the request
		request.respond(response);
	}
}

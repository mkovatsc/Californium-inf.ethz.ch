package ch.ethz.inf.vs.californium.examples.resources;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource responds with a kind "hello world" to GET requests.
 * 
 * @author Martin Lanter
 */
public class HelloWorldResource extends ResourceBase {

	public HelloWorldResource(String name) {
		super(name);
	}
	
	@Override
	public void handleGET(Exchange exchange) {
		Response response = new Response(ResponseCode.CONTENT);
		response.setPayload("hello world");
		respond(exchange, response);
	}

}

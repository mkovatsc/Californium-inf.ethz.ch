package ch.ethz.inf.vs.californium.benchmark;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource responds with a kind "hello world" to GET requests.
 * 
 * @author Martin Lanter
 */
public class BenchmarkResource extends ResourceBase {

	public BenchmarkResource(String name) {
		super(name);
	}
	
	@Override
	public void handleRequest(Exchange exchange) {
		Response response = new Response(ResponseCode.CONTENT);
		response.setPayload("hello world");
		exchange.sendResponse(response);
	}

}

package ch.ethz.inf.vs.californium.examples.resources;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource responds with the data from a request in its payload. This
 * resource responds to GET, POST, PUT and DELETE requests.
 */
public class MirrorResource extends ResourceBase {

	public MirrorResource(String name) {
		super(name);
	}
	
	@Override
	public Resource getChild(String name) {
		return this;
	}
	
	/**
	 * This method uses the internal {@link Exchange} class for advanced handling.
	 */
	@Override
	public void handleRequest(Exchange exchange) {
		Request request = exchange.getRequest();
		StringBuilder buffer = new StringBuilder();
		buffer.append("resource ").append(getURI()).append(" received request")
			.append("\n").append("Code: ").append(request.getCode())
			.append("\n").append("Source: ").append(request.getSource()).append(":").append(request.getSourcePort())
			.append("\n").append("Type: ").append(request.getType())
			.append("\n").append("MID: ").append(request.getMID())
			.append("\n").append("Token: ").append(request.getTokenString())
			.append("\n").append(request.getOptions());
		Response response = new Response(ResponseCode.CONTENT);
		response.setPayload(buffer.toString());
		response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
		exchange.sendResponse(response);
	}
}

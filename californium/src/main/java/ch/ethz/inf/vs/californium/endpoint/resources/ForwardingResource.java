/**
 * 
 */

package ch.ethz.inf.vs.californium.endpoint.resources;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

/**
 * @author Francesco Corazza
 * 
 */
public abstract class ForwardingResource extends LocalResource {

	public ForwardingResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	public ForwardingResource(String resourceIdentifier, boolean hidden) {
		super(resourceIdentifier, hidden);
	}

	@Override
	public void performDELETE(DELETERequest request) {
		Response response = forwardRequest(request);
		request.respond(response);
	}

	@Override
	public void performGET(GETRequest request) {
		Response response = forwardRequest(request);
		request.respond(response);
	}

	@Override
	public void performPOST(POSTRequest request) {
		Response response = forwardRequest(request);
		request.respond(response);
	}

	@Override
	public void performPUT(PUTRequest request) {
		Response response = forwardRequest(request);
		request.respond(response);
	}

	protected abstract Response forwardRequest(Request incomingRequest);
}

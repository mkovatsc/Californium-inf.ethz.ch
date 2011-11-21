package ch.eth.coap.endpoint;

import java.util.HashMap;
import java.util.Map;

import ch.eth.coap.coap.CodeRegistry;
import ch.eth.coap.coap.DELETERequest;
import ch.eth.coap.coap.GETRequest;
import ch.eth.coap.coap.POSTRequest;
import ch.eth.coap.coap.PUTRequest;
import ch.eth.coap.coap.Request;


/*
 * This class describes the functionality of a local CoAP resource.
 * 
 * A client of the Californium framework will override this class 
 * in order to provide custom resources.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class LocalResource extends Resource {

	// Constructors ////////////////////////////////////////////////////////////

	public LocalResource(String resourceIdentifier, boolean hidden) {
		super(resourceIdentifier, hidden);
	}

	public LocalResource(String resourceIdentifier) {
		super(resourceIdentifier, false);
	}

	// Observing ///////////////////////////////////////////////////////////////

	public void addObserveRequest(GETRequest request) {

		if (request != null) {

			// lazy creation
			if (observeRequests == null) {
				observeRequests = new HashMap<String, GETRequest>();
			}

			observeRequests.put(request.endpointID(), request);

			System.out
					.printf("Observation relationship between %s and %s established.\n",
							request.endpointID(), getResourceIdentifier());

		}
	}

	public void removeObserveRequest(String endpointID) {

		if (observeRequests != null) {
			if (observeRequests.remove(endpointID) != null) {
				System.out
						.printf("Observation relationship between %s and %s terminated.\n",
								endpointID, getResourceIdentifier());
			}
		}
	}

	public boolean isObserved(String endpointID) {
		return observeRequests != null
				&& observeRequests.containsKey(endpointID);
	}

	protected void processObserveRequests() {
		if (observeRequests != null) {
			for (GETRequest request : observeRequests.values()) {
				performGET(request);
			}
		}
	}

	protected void changed() {
		processObserveRequests();
	}

	// REST Operations /////////////////////////////////////////////////////////

	@Override
	public void performGET(GETRequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}

	@Override
	public void performPUT(PUTRequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}

	@Override
	public void performPOST(POSTRequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}

	@Override
	public void performDELETE(DELETERequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}

	@Override
	public void createNew(Request request, String newIdentifier) {
		request.respond(CodeRegistry.RESP_FORBIDDEN);
	}

	private Map<String, GETRequest> observeRequests;

}

package coap;

/*
 * This class describes the functionality of a CoAP read-only resource.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class ReadOnlyResource extends LocalResource {

	// Constructors ////////////////////////////////////////////////////////////

	public ReadOnlyResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	// REST Operations /////////////////////////////////////////////////////////

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
	public void createNew(PUTRequest request, String newIdentifier) {
		request.respond(CodeRegistry.RESP_FORBIDDEN);
	}

}

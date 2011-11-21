package ch.eth.coap.endpoint;

import ch.eth.coap.coap.DELETERequest;
import ch.eth.coap.coap.GETRequest;
import ch.eth.coap.coap.POSTRequest;
import ch.eth.coap.coap.PUTRequest;
import ch.eth.coap.coap.Request;

public class RemoteResource extends Resource {

	public static RemoteResource newRoot(String linkFormat) {
		RemoteResource resource = new RemoteResource();
		resource.setResourceIdentifier("");
		resource.setResourceType("root");
		resource.setResourceTitle("Root");
		resource.addLinkFormat(linkFormat);
		return resource;
	}

	@Override
	public void createNew(Request request, String newIdentifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public void performDELETE(DELETERequest request) {
		// TODO Auto-generated method stub

	}

	@Override
	public void performGET(GETRequest request) {
		// TODO Auto-generated method stub

	}

	@Override
	public void performPOST(POSTRequest request) {
		// TODO Auto-generated method stub

	}

	@Override
	public void performPUT(PUTRequest request) {
		// TODO Auto-generated method stub

	}

}

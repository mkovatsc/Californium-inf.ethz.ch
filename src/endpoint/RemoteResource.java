package endpoint;

import coap.DELETERequest;
import coap.GETRequest;
import coap.POSTRequest;
import coap.PUTRequest;
import coap.Request;

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
	public void createSubResource(Request request, String newIdentifier) {
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

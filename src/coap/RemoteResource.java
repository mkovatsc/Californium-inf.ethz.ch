package coap;

public class RemoteResource extends Resource {

	public static RemoteResource newRoot(String linkFormat) {
		RemoteResource resource = new RemoteResource();
		resource.setResourceIdentifier("");
		resource.setResourceType("root");
		resource.addLinkFormat(linkFormat);
		return resource;
	}

	@Override
	public void createNew(PUTRequest request, String newIdentifier) {
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

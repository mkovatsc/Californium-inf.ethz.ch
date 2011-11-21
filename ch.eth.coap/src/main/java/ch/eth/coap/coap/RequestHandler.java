package ch.eth.coap.coap;

public interface RequestHandler {

	public void performGET(GETRequest request);

	public void performPOST(POSTRequest request);

	public void performPUT(PUTRequest request);

	public void performDELETE(DELETERequest request);

}

package ch.ethz.inf.vs.californium.coap;

public interface RequestHandler {

	public void performGET(GETRequest request);

	public void performPOST(POSTRequest request);

	public void performPUT(PUTRequest request);

	public void performDELETE(DELETERequest request);

}

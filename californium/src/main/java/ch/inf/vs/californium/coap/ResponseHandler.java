package ch.inf.vs.californium.coap;

public interface ResponseHandler {

	public void responded(Response response);
	
	public void acknowledged();
	
	public void rejected();
	
	public void timeouted();
	
	public void canceled();
	
}

package ch.ethz.inf.vs.californium;

public interface CoapHandler {

	public void responded(CoapResponse response);
	
	public void failed();

}

package ch.ethz.inf.vs.californium.coap;

public class UnsupportedRequest extends Request {

	public UnsupportedRequest(int code) {
		super(code);
	}
	
	@Override
	public void send() {
		LOG.severe("Cannot send UnsupportedRequest");
	}
}

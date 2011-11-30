package ch.ethz.inf.vs.californium.coap;

public class POSTRequest extends Request {

	public POSTRequest() {
		super(CodeRegistry.METHOD_POST, true);
	}

	@Override
	public void dispatch(RequestHandler handler) {
		handler.performPOST(this);
	}
}

package ch.ethz.inf.vs.californium.coap;

public class DELETERequest extends Request {

	public DELETERequest() {
		super(CodeRegistry.METHOD_DELETE, true);
	}

	@Override
	public void dispatch(RequestHandler handler) {
		handler.performDELETE(this);
	}
}

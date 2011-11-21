package ch.eth.coap.coap;

public class GETRequest extends Request {

	public GETRequest() {
		super(CodeRegistry.METHOD_GET, true);
	}

	@Override
	public void dispatch(RequestHandler handler) {
		handler.performGET(this);
	}
}

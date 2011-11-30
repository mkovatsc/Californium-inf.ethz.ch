package ch.ethz.inf.vs.californium.coap;

public class Response extends Message {

	public Response() {
		this(CodeRegistry.RESP_VALID);
	}

	public Response(int code) {
		setCode(code);
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public Request getRequest() {
		return request;
	}

	public int getRTT() {
		if (request != null) {
			return (int) (getTimestamp() - request.getTimestamp());
		} else {
			return -1;
		}
	}

	public void handle() {
		if (request != null) {
			request.handleResponse(this);
		}
	}

	@Override
	protected void payloadAppended(byte[] block) {
		if (request != null) {
			request.responsePayloadAppended(this, block);
		}
	}

	@Override
	protected void completed() {
		if (request != null) {
			request.responseCompleted(this);
		}
	}

	@Override
	public void handleBy(MessageHandler handler) {
		handler.handleResponse(this);
	}

	public boolean isPiggyBacked() {
		return isAcknowledgement() && getCode() != CodeRegistry.EMPTY_MESSAGE;
	}

	public boolean isEmptyACK() {
		return isAcknowledgement() && getCode() == CodeRegistry.EMPTY_MESSAGE;
	}

	private Request request;
}

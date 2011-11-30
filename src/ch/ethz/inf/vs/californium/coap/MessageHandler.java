package ch.ethz.inf.vs.californium.coap;

public interface MessageHandler {

	public void handleRequest(Request request);

	public void handleResponse(Response response);
}

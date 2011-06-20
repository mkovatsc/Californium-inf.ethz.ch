package coap;

public interface MessageHandler {

	public void handleRequest(Request request);

	public void handleResponse(Response response);
}

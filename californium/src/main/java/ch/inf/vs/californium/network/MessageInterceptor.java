package ch.inf.vs.californium.network;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

public interface MessageInterceptor {

	public void sendRequest(Request request);
	
	public void sendResponse(Response response);
	
	public void sendEmptyMessage(EmptyMessage message);
	
	public void receiveRequest(Request request);
	
	public void receiveResponse(Response response);
	
	public void receiveEmptyMessage(EmptyMessage message);
	
}

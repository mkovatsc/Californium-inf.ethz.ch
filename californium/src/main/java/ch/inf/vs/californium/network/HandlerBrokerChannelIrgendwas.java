package ch.inf.vs.californium.network;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

public interface HandlerBrokerChannelIrgendwas {

	public void sendRequest(Exchange exchange, Request request);
	
	public void sendResponse(Exchange exchange, Response response);
	
	public void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);
	
	public void receiveData(RawData raw);
	
}

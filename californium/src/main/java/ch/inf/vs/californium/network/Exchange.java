package ch.inf.vs.californium.network;

import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

public class Exchange {

	private Request request;
	private Response response;
	
	public Exchange() {
		
	}
	
	public boolean hasRequest() {
		return request != null;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public boolean hasResponse() {
		return response != null;
	}
	
	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}
	
}

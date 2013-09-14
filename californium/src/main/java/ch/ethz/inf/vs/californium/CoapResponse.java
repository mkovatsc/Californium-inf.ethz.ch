package ch.ethz.inf.vs.californium;

import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.Response;

public class CoapResponse {

	private Response response;
	
	protected CoapResponse(Response response) {
		this.response = response;
	}
	
	public boolean isSuccess() {
		return CoAP.ResponseCode.isSuccess(response.getCode());
	}
	
	public String getPayloadString() {
		return response.getPayloadString();
	}
	
	public byte[] getPayload() {
		return response.getPayload();
	}
	
	public Response getResponse() {
		return response;
	}
}

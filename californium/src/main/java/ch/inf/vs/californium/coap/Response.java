package ch.inf.vs.californium.coap;

import java.util.Arrays;

import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.CoAP.Type;

public class Response extends Message {

	private final CoAP.ResponseCode code;
	
	public Response(ResponseCode code) {
		super(Type.NCON);
		this.code = code;
	}

	public CoAP.ResponseCode getCode() {
		return code;
	}
	
	@Override
	public String toString() {
		return getType()+"-"+code+"-Response: MID="+getMid()+", Token="+Arrays.toString(getToken())+", "+getOptions()+", Payload=\""+getPayloadString()+"\"";
	}
	
	public static Response createResponse(Request request, ResponseCode code) {
		Response response = new Response(code);
		response.setDestination(request.getSource());
		response.setDestinationPort(request.getSourcePort());
		response.setToken(request.getToken());
		return response;
	}
}

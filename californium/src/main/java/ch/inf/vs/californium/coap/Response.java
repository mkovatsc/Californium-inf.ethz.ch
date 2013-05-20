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
		String payload;
		if (getPayloadSize() <= 24)
			payload = "\""+getPayloadString()+"\"";
		else payload = "\""+getPayloadString().substring(0,20)+".. "+getPayloadSize()+" bytes\"";
		return getType()+"-"+code+"-Response: MID="+getMid()+", Token="+Arrays.toString(getToken())+", "+getOptions()+", Payload="+payload+", debugID="+debugID;
	}
	
	public static Response createPiggybackedResponse(Request request, ResponseCode code) {
		Response response = new Response(code);
		response.setMid(request.getMid());
		response.setType(Type.ACK);
		response.setDestination(request.getSource());
		response.setDestinationPort(request.getSourcePort());
		response.setToken(request.getToken());
		return response;
	}
	
	public static Response createSeparateResponse(Request request, ResponseCode code) {
		Response response = new Response(code);
		response.setDestination(request.getSource());
		response.setDestinationPort(request.getSourcePort());
		response.setToken(request.getToken());
		return response;
	}
}

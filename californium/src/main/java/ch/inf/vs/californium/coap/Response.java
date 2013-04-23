package ch.inf.vs.californium.coap;

import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.CoAP.Type;

public class Response extends Message {

	private final CoAP.ResponseCode code;
	
	public Response(ResponseCode code) {
		super(Type.CON);
		this.code = code;
	}

	public CoAP.ResponseCode getCode() {
		return code;
	}
	
}

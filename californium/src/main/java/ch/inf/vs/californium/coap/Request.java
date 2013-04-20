package ch.inf.vs.californium.coap;

import ch.inf.vs.californium.coap.CoAP.Code;


public class Request extends Message {

	private final CoAP.Code code;
	
	public Request(Code code) {
		this.code = code;
	}
	
	public Code getCode() {
		return code;
	}
}

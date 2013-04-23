package ch.inf.vs.californium.coap;

import java.util.Arrays;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.Type;


public class Request extends Message {

	private final CoAP.Code code;
	
	public Request(Code code) {
		super(Type.NCON);
		this.code = code;
	}
	
	public Code getCode() {
		return code;
	}
	
	@Override
	public String toString() {
		return getType()+"-"+code+"-Request: Token="+Arrays.toString(getToken())+", "+getOptions();
	}
}

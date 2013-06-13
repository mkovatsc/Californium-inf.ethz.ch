package ch.inf.vs.californium.coap;

import java.util.Arrays;

import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.CoAP.Type;

/**
 * Response represents a CoAP response to a CoAP request. A response is either a
 * piggy-backed response with type ACK or a separate response with type CON or
 * NCON. A response has a response code ({@link CoAP.ResponseCode}).
 */
public class Response extends Message {

	/** The response code. */
	private final CoAP.ResponseCode code;
	
	/**
	 * Instantiates a new response with the specified response code.
	 *
	 * @param code the response code
	 */
	public Response(ResponseCode code) {
		super(Type.NON);
		this.code = code;
	}

	/**
	 * Gets the response code.
	 *
	 * @return the code
	 */
	public CoAP.ResponseCode getCode() {
		return code;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String payload;
		if (getPayloadSize() <= 24)
			payload = "\""+getPayloadString()+"\"";
		else payload = "\""+getPayloadString().substring(0,20)+".. "+getPayloadSize()+" bytes\"";
		String mid = getMid()==NONE?"none":String.valueOf(getMid());
		return getType()+"-"+code+"-Response: MID="+mid+", Token="+Arrays.toString(getToken())+", "+getOptions()+", Payload="+payload;
	}
	
	/**
	 * Creates a piggy-backed response with the specified response code to the
	 * specified request. The destination address of the response is the source
	 * address of the request. The response has the same MID and token as the
	 * request.
	 * 
	 * @param request the request
	 * @param code the code
	 * @return the response
	 */
	public static Response createPiggybackedResponse(Request request, ResponseCode code) {
		Response response = new Response(code);
		response.setMid(request.getMid());
		response.setType(Type.ACK);
		response.setDestination(request.getSource());
		response.setDestinationPort(request.getSourcePort());
		response.setToken(request.getToken());
		return response;
	}
	
	/**
	 * Creates a separate response with the specified response code to the
	 * specified request. The destination address of the response is the source
	 * address of the request. The response has the same token as the request
	 * but needs another MID from the CoAP network stack.
	 *
	 * @param request the request
	 * @param code the code
	 * @return the response
	 */
	public static Response createSeparateResponse(Request request, ResponseCode code) {
		Response response = new Response(code);
		response.setDestination(request.getSource());
		response.setDestinationPort(request.getSourcePort());
		response.setToken(request.getToken());
		return response;
	}
}

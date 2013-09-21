package ch.ethz.inf.vs.californium;

import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.OptionSet;
import ch.ethz.inf.vs.californium.coap.Response;

/**
 * CoapResponse provides a simple API for CoAP responses. Use a
 * {@link CoapClient} to send requests to a CoAP server and receive such a
 * response.
 * <p>
 * CoapResponse wraps an instance of type {@link Response} that is used
 * internally in Californium. To access this object directly for more detailed
 * information, call {@link #getDetailed()}.
 */
public class CoapResponse {

	/** The insternal response. */
	private Response response;
	
	/**
	 * Instantiates a new coap response.
	 *
	 * @param response the response
	 */
	protected CoapResponse(Response response) {
		this.response = response;
	}

	/**
	 * Gets the response code code.
	 *
	 * @return the response code
	 */
	public ResponseCode getCode() {
		return response.getCode();
	}
	
	/**
	 * Checks if the response code is a successful code.
	 *
	 * @return true, if is success
	 */
	public boolean isSuccess() {
		return CoAP.ResponseCode.isSuccess(response.getCode());
	}
	
	/**
	 * Gets the payload of this response as string.
	 *
	 * @return the response text
	 */
	public String getResponseText() {
		return response.getPayloadString();
	}
	
	/**
	 * Gets the payload of this response as byte array.
	 *
	 * @return the payload
	 */
	public byte[] getPayload() {
		return response.getPayload();
	}
	
	/**
	 * Gets the internal representation of the response.
	 *
	 * @return the internal response object
	 */
	public Response getDetailed() {
		return response;
	}
	
	/**
	 * Gets the set of options of this response.
	 *
	 * @return the options
	 */
	public OptionSet getOptions() {
		return response.getOptions();
	}
}

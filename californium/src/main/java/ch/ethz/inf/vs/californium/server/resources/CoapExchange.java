package ch.ethz.inf.vs.californium.server.resources;

import java.net.InetAddress;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.OptionSet;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;

/**
 * The Class CoapExchange represents an exchange of a CoAP request and response
 * and provides a user-friendly API to subclasses of {@link ResourceBase} for
 * responding to requests.
 */
public class CoapExchange {
	
	/** The exchange. */
	private Exchange exchange;
	
	/** The destination resource. */
	private ResourceBase resource;
	
	private String locationPath;
	
	/**
	 * Constructs a new CoAP Exchange object representing the specified exchange
	 * and Resource.
	 * 
	 * @param exchange the exchange
	 * @param resource the resource
	 */
	protected CoapExchange(Exchange exchange, ResourceBase resource) {
		if (exchange == null) throw new NullPointerException();
		if (resource == null) throw new NullPointerException();
		this.exchange = exchange;
		this.resource = resource;
	}
	
	/**
	 * Gets the source address of the request.
	 *
	 * @return the source address
	 */
	public InetAddress getSourceAddress() {
		return exchange.getRequest().getSource();
	}
	
	/**
	 * Gets the source port of the request.
	 *
	 * @return the source port
	 */
	public int getSourcePort() {
		return exchange.getRequest().getSourcePort();
	}
	
	/**
	 * Gets the request code: <tt>GET</tt>, <tt>POST</tt>, <tt>PUT</tt> or
	 * <tt>DELETE</tt>.
	 * 
	 * @return the request code
	 */
	public Code getRequestCode() {
		return exchange.getRequest().getCode();
	}
	
	/**
	 * Gets the request's options.
	 *
	 * @return the request options
	 */
	public OptionSet getRequestOptions() {
		return exchange.getRequest().getOptions();
	}
	
	/**
	 * Gets the request payload as byte array.
	 *
	 * @return the request payload
	 */
	public byte[] getRequestPayload() {
		return exchange.getRequest().getPayload();
	}
	
	/**
	 * Gets the request payload as string.
	 *
	 * @return the request payload string
	 */
	public String getRequestText() {
		return exchange.getRequest().getPayloadString();
	}
	
	/**
	 * Accept the exchange, i.e. send an acknowledgment to the client that the
	 * exchange has arrived and a separate message is being computed and sent
	 * soon. Call this method on an exchange if the computation of a response
	 * might take some time and might trigger a timeout at the client.
	 */
	public void accept() {
		exchange.accept();
	}
	
	/**
	 * Reject the exchange if it is impossible to be processed, e.g. if it
	 * carries an unknown critical option. In most cases, it is better to
	 * respond with an error response code to bad requests though.
	 */
	public void reject() {
		exchange.reject();
	}
	
	public void setLocationPath(String path) {
		locationPath = path;
	}
	
	/**
	 * Respond the specified response code and no payload.
	 *
	 * @param code the code
	 */
	public void respond(ResponseCode code) {
		respond(new Response(code));
	}
	
	/**
	 * Respond with response code 2.05 (Content) and the specified payload.
	 * 
	 * @param payload the payload as string
	 */
	public void respond(String payload) {
		respond(ResponseCode.CONTENT, payload);
	}
	
	/**
	 * Respond with the specified response code and the specified payload.
	 *
	 * @param code the response code
	 * @param payload the payload
	 */
	public void respond(ResponseCode code, String payload) {
		Response response = new Response(code);
		response.setPayload(payload);
		response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
		respond(response);
	}
	
	/**
	 * Respond with the specified response code and the specified payload.
	 *
	 * @param code the response code
	 * @param payload the payload
	 */
	public void respond(ResponseCode code, byte[] payload) {
		Response response = new Response(code);
		response.setPayload(payload);
		respond(response);
	}

	/**
	 * Respond with the specified response code and the specified payload.
	 *
	 * @param code the response code
	 * @param payload the payload
	 * @param contentFormat the Content-Format of the payload
	 */
	public void respond(ResponseCode code, byte[] payload, int contentFormat) {
		Response response = new Response(code);
		response.setPayload(payload);
		response.getOptions().setContentFormat(contentFormat);
		respond(response);
	}
	
	/**
	 * Respond with the specified response code and the specified payload.
	 *
	 * @param code the response code
	 * @param payload the payload
	 * @param contentFormat the Content-Format of the payload
	 */
	public void respond(ResponseCode code, String payload, int contentFormat) {
		Response response = new Response(code);
		response.setPayload(payload);
		response.getOptions().setContentFormat(contentFormat);
		respond(response);
	}
	
	/**
	 * Provides access to the internal Exchange object.
	 * 
	 * @return the Exchange object
	 */
	public Exchange advanced() {
		return exchange;
	}
	
	/**
	 * Respond with the specified response.
	 *
	 * @param response the response
	 */
	private void respond(Response response) {
		if (response == null) throw new NullPointerException();
		if (locationPath != null)
			response.getOptions().setLocationPath(locationPath);
		resource.respond(exchange, response);
	}
}

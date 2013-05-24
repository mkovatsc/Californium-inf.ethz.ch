package ch.inf.vs.californium.coap;

import java.util.Arrays;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointManager;


/**
 * Request represents a CoAP request. A request has either the {@link Type} CON
 * or NCON and one of the {@link CoAP.Code} GET, POST, PUT or DELETE.
 */
public class Request extends Message {

	/** The request code. */
	private final CoAP.Code code;
	
	/** The current response for the request. */
	private Response response;
	
	/** The lock object used to wait for a response. */
	private Object lock;
	
	/**
	 * Instantiates a new request.
	 *
	 * @param code the request code
	 */
	public Request(Code code) {
		super(Type.NCON);
		this.code = code;
	}
	
	/**
	 * Gets the request code.
	 *
	 * @return the code
	 */
	public Code getCode() {
		return code;
	}
	
	/**
	 * Sends the request over the default endpoint to its destination and
	 * expects a response back.
	 */
	public void send() {
		validateBeforeSending();
		EndpointManager.getEndpointManager().getDefaultEndpoint().sendRequest(this);
	}
	
	/**
	 * Sends the request over the specified endpoint to its destination and
	 * expects a response back.
	 * 
	 * @param endpoint the endpoint
	 */
	public void send(Endpoint endpoint) {
		validateBeforeSending();
		endpoint.sendRequest(this);
	}
	
	/**
	 * Validate before sending that there is a destination set.
	 */
	private void validateBeforeSending() {
		if (getDestination() == null)
			throw new NullPointerException("Destination is null");
		if (getDestinationPort() == 0)
			throw new NullPointerException("Destination port is 0");
	}

	/**
	 * Gets the current response.
	 *
	 * @return the response
	 */
	public Response getResponse() {
		return response;
	}

	/**
	 * Sets the response.
	 *
	 * @param response the new response
	 */
	public void setResponse(Response response) {
		this.response = response;
		
		if (lock != null)
			synchronized (lock) {
				lock.notifyAll();
			}
		// else: we know that nobody is waiting on the lock
	}
	
	/**
	 * Wait for the response. This function blocks until there is a response or
	 * the request has been canceled.
	 * 
	 * @return the response
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	public Response waitForResponse() throws InterruptedException {
		return waitForResponse(0);
	}
	
	/**
	 * Wait for the response. This function block until there is a response, the
	 * request has been canceled or the specified timeout has expired. A timeout
	 * of 0 is interpreted as infinity.
	 * 
	 * @param timeout
	 *            the maximum time to wait in milliseconds.
	 * @return the response
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	public Response waitForResponse(long timeout) throws InterruptedException {
		// Lazy initialization of a lock
		if (lock == null) {
			synchronized (this) {
				if (lock == null)
					lock = new Object();
			}
		}
		// wait for response
		synchronized (lock) {
			while (response == null /* TODO: and not canceled*/) {
				lock.wait(timeout);
				if (timeout > 0) // TODO: Only when time has elapsed
					return response;
			}
		}
		return response;
	}
	
	/**
	 * Cancels the request.
	 */
	public void cancel() {
		if (lock != null) {
			synchronized (lock) {
				lock.notifyAll();
			}
		}
		// TODO: cancel exchange
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
		return getType()+"-"+code+"-Request: MID="+getMid()+", Token="+Arrays.toString(getToken())+", "+getOptions()+", Payload="+payload+", debugID="+debugID;
	}
}

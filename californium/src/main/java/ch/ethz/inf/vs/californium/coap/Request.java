package ch.ethz.inf.vs.californium.coap;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;


/**
 * Request represents a CoAP request. A request has either the {@link Type} CON
 * or NCON and one of the {@link CoAP.Code} GET, POST, PUT or DELETE.
 */
public class Request extends Message {
	
	// TODO: Add method to reset a request so that it can be sent again. Reset MID, maybe token, bytes.

	private final static Logger LOGGER = CalifonriumLogger.getLogger(Request.class);
	
	/** The request code. */
	private final CoAP.Code code;
	
	/** The current response for the request. */
	private Response response;
	
	private String scheme;
	
	/** The lock object used to wait for a response. */
	private Object lock;
	
	/**
	 * Instantiates a new request.
	 *
	 * @param code the request code
	 */
	public Request(Code code) {
		this.code = code;
	}
	
	public Request(Code code, Type type) {
		this.code = code;
		super.setType(type);
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
	 * TODO (scheme is important)
	 * must have form: [scheme]://[host]:[port]{/resource}*?{&query}*
	 */
	public void setURI(String uri) {
		try {
			if (!uri.startsWith("coap://") && !uri.startsWith("coaps://"))
				uri = "coap://" + uri;
			setURI(new URI(uri));
		} catch (URISyntaxException e) {
			LOGGER.log(Level.WARNING, "Failed to set uri "+uri ,e);
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * TODO
	 */
	public void setURI(URI uri) {
		/*
		 * Implementation from old Cf
		 */
		String host = uri.getHost();
		// set Uri-Host option if not IP literal
		if (host != null && !host.toLowerCase().matches("(\\[[0-9a-f:]+\\]|[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})")) {
			getOptions().setURIHost(host);
		}

		try {
			setDestination(InetAddress.getByName(host));
		} catch (UnknownHostException e) {
    		LOGGER.log(Level.WARNING, "Unknown host as destination", e);
    	}
		
		/*
		 * The Uri-Port is only for special cases where it differs from the UDP port.
		 * (Tell me when that happens...)
		 */
		// set uri-port option
		int port = uri.getPort();
		if (port >= 0) {
			getOptions().setURIPort(port);
			setDestinationPort(port);
		} else if (getDestinationPort() == 0) {
			// FIXME: should this depend on scheme(coaps with different default port)? 
			setDestinationPort(EndpointManager.DEFAULT_PORT);
		}

		// set Uri-Path options
		String path = uri.getPath();
		if (path != null && path.length() > 1) {
			getOptions().setURIPath(path);
		}

		// set Uri-Query options
		String query = uri.getQuery();
		if (query != null) {
			getOptions().setURIQuery(query);
		}
		
		String scheme = uri.getScheme();
		if (scheme != null) {
			// decide according to URI scheme whether DTLS is enabled for the client
			this.scheme = scheme;
		}
	}
	
	/**
	 * Sends the request over the default endpoint to its destination and
	 * expects a response back.
	 */
	public Request send() {
		validateBeforeSending();
		// TODO: if secure, send over DTLS
		EndpointManager.getEndpointManager().getDefaultEndpoint().sendRequest(this);
		return this;
	}
	
	/**
	 * Sends the request over the specified endpoint to its destination and
	 * expects a response back.
	 * 
	 * @param endpoint the endpoint
	 */
	public Request send(Endpoint endpoint) {
		validateBeforeSending();
		endpoint.sendRequest(this);
		return this;
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
	 * Sets CoAP's observe option. If the target resource of this request
	 * responds with a success code and also sets the observe option, it will
	 * send more responses in the future whenever the resource's state changes.
	 */
	public void setObserve() {
		getOptions().setObserve(0);
	}
	
	/**
	 * Gets the current response. TODO: This method is currently here for backward
	 * compatibility with the proxy. After the final implementation of how
	 * resources send responses, we should be able to change this.
	 *
	 * @return the response
	 */
	public Response getResponse() {
		return response;
	}

	/**
	 * Sets the response. TODO: This method is currently here for backward
	 * compatibility with the proxy. After the final implementation of how
	 * resources send responses, we should be able to change this.
	 * 
	 * @param response
	 *            the new response
	 */
	public void setResponse(Response response) {
		this.response = response;
		
		if (lock != null)
			synchronized (lock) {
				lock.notifyAll();
			}
		// else: we know that nobody is waiting on the lock
		
		for (MessageObserver handler:getMessageObservers())
			handler.responded(response);
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
	
	// TODO: this method also removes the current response
	/**
	 * Wait for the response. This function blocks until there is a response,
	 * the request has been canceled or the specified timeout has expired. A
	 * timeout of 0 is interpreted as infinity. If a response is already here,
	 * this method return it immediately.
	 * <p>
	 * The calling thread returns if either a response arrives, the request gets
	 * rejected by the server, the request gets canceled or, in case of a
	 * confirmable request, timeouts. If no response has arrived the return
	 * value is null.
	 * 
	 * @param timeout
	 *            the maximum time to wait in milliseconds.
	 * @return the response (null if timeout occured)
	 * @throws InterruptedException
	 *             the interrupted exception
	 * @see #waitForNextResponse(long)
	 */
	public Response waitForResponse(long timeout) throws InterruptedException {
		long before = System.currentTimeMillis();
		long expired = timeout>0 ? (before + timeout) : 0;
		// Lazy initialization of a lock
		if (lock == null) {
			synchronized (this) {
				if (lock == null)
					lock = new Object();
			}
		}
		// wait for response
		synchronized (lock) {
			while (response == null 
					&& !isCanceled() && !isTimeouted() && !isRejected()) {
				lock.wait(timeout);
				long now = System.currentTimeMillis();
				if (timeout > 0 && expired <= now) {
					Response r = response;
					response = null;
					return r;
				}
			}
			Response r = response;
			response = null;
			return r;
		}
	}
	
//	/**
//	 * Remove the current response and wait for the next response. This method
//	 * blocks until there is a new response, the request has been canceled or
//	 * the specified timeout has expired. A timeout of 0 is interpreted as
//	 * infinity.
//	 * <p>
//	 * The calling thread returns if either a new response arrives, the request
//	 * gets rejected by the server, the request gets canceled or, in case of a
//	 * confirmable request, timeouts. If no response has arrived the return
//	 * value is null.
//	 * 
//	 * @param timeout
//	 *            the maximum time to wait in milliseconds.
//	 * @return the response (null if timeout occured)
//	 * @throws InterruptedException
//	 *             the interrupted exception
//	 * @see #waitForResponse(long)
//	 */
//	public Response waitForNextResponse(long timeout) throws InterruptedException {
//		long before = System.currentTimeMillis();
//		long expired = timeout>0 ? (before + timeout) : 0;
//		// Lazy initialization of a lock
//		if (lock == null) {
//			synchronized (this) {
//				if (lock == null)
//					lock = new Object();
//			}
//		}
//		// wait for response
//		synchronized (lock) {
//			response = null;
//			while (response == null 
//					&& !isCanceled() && !isTimeouted() && !isRejected()) {
//				lock.wait(timeout);
//				long now = System.currentTimeMillis();
//				if (timeout > 0 && expired <= now) {
//					return response;
//				}
//			}
//			return response;
//		}
//	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Furthermore, if the request is canceled, it will wake up all threads that
	 * are currently waiting for a response.
	 */
	@Override
	public void setTimeouted(boolean timeouted) {
		super.setTimeouted(timeouted);
		if (timeouted && lock != null) {
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Furthermore, if the request is canceled, it will wake up all threads that
	 * are currently waiting for a response.
	 */
	@Override
	public void setCanceled(boolean canceled) {
		super.setCanceled(canceled);
		if (canceled && lock != null) {
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String payload = getPayloadString();
		if (payload == null) payload = "null";
		else if (payload.length() <= 24)
			payload = "\""+payload+"\"";
		else payload = "\"" + payload.substring(0,20) + ".. " + payload.length() + " bytes\"";
		return getType()+"-"+code+"-Request: MID="+getMID()+", Token=["+getTokenString()+"], "+getOptions()+", Payload="+payload;
	}
	
	public static Request newGet() { return new Request(Code.GET); }
	public static Request newPost() { return new Request(Code.POST); }
	public static Request newPut() { return new Request(Code.PUT); }
	public static Request newDelete() { return new Request(Code.DELETE); }

}

package ch.inf.vs.californium.coap;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointManager;
import ch.inf.vs.californium.resources.CalifonriumLogger;


/**
 * Request represents a CoAP request. A request has either the {@link Type} CON
 * or NCON and one of the {@link CoAP.Code} GET, POST, PUT or DELETE.
 */
public class Request extends Message {

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
		super(Type.NON);
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
	 * TODO: comment
	 */
	public void setObserve() {
		getOptions().setObserve(0);
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
		
		for (ResponseHandler handler:getResponseHandlers())
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
	
	/**
	 * Wait for the response. This function block until there is a response, the
	 * request has been canceled or the specified timeout has expired. A timeout
	 * of 0 is interpreted as infinity.
	 * 
	 * @param timeout
	 *            the maximum time to wait in milliseconds.
	 * @return the response (null if timeout occured)
	 * @throws InterruptedException
	 *             the interrupted exception
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
			while (response == null && !isCanceled() && !isRejected()) {
				lock.wait(timeout);
				long now = System.currentTimeMillis();
				if (timeout > 0 && expired <= now) {
					LOGGER.info(" ==Request timeouted, timeout: "+timeout+", expired="+expired+", now="+now+", before="+before);
					return response;
				}
			}
		}
		return response;
	}
	
	/**
	 * Cancels the request.
	 */
	// TODO: comment
	@Override
	public void cancel() {
		super.cancel();
		if (lock != null) {
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
		String payload;
		if (getPayloadSize() <= 24)
			payload = "\""+getPayloadString()+"\"";
		else payload = "\""+getPayloadString().substring(0,20)+".. "+getPayloadSize()+" bytes\"";
		return getType()+"-"+code+"-Request: MID="+getMid()+", Token="+Arrays.toString(getToken())+", "+getOptions()+", Payload="+payload;
	}
	
	public static Request newGet() { return new Request(Code.GET); }
	public static Request newPost() { return new Request(Code.POST); }
	public static Request newPut() { return new Request(Code.PUT); }
	public static Request newDelete() { return new Request(Code.DELETE); }

}

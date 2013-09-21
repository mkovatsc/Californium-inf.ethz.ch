package ch.ethz.inf.vs.californium;

/**
 * A CoapHandler can be used to asynchronously react to responses from a CoAP
 * client. When a response or in case of a CoAP observe relation a notification
 * arrives, the method {@link #responded(CoapResponse)} is invoked. If a request
 * timeouts or the server rejects it, the method {@link #failed()} is invoked.
 */
public interface CoapHandler {

	/**
	 * Invoked when a CoAP response or notification has arrived.
	 *
	 * @param response the response
	 */
	public void responded(CoapResponse response);
	
	/**
	 * Invoked when a request timeouts or has been rejected by the server.
	 */
	public void failed();

}

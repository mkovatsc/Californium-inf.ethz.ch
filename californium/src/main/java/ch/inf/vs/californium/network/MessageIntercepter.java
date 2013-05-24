package ch.inf.vs.californium.network;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.serializer.Serializer;

/**
 * MessageInterceptors registers at an endpoint. When messages arrive from the
 * connector, the corresponding receive-method is called. When a message is
 * about to be sent over a connector, the corresponding send-method is called.
 * The intercepter can be sought of being placed inside an {@link Endpoint} just
 * between the message {@link Serializer} and the {@link Matcher}.
 */
public interface MessageIntercepter {

	/**
	 * Override this method to be notified when a request is about to be sent.
	 *
	 * @param request the request
	 */
	public void sendRequest(Request request);
	
	/**
	 * Override this method to be notified when a response is about to be sent.
	 *
	 * @param response the response
	 */
	public void sendResponse(Response response);
	
	/**
	 * Ovveride this method to be notified when an empty message is about to be
	 * sent.
	 * 
	 * @param message the empty message
	 */
	public void sendEmptyMessage(EmptyMessage message);
	
	/**
	 * Override this method to be notified when request has been received.
	 *
	 * @param request the request
	 */
	public void receiveRequest(Request request);
	
	/**
	 * Override this method to be notified when response has been received.
	 *
	 * @param response the response
	 */
	public void receiveResponse(Response response);
	
	/**
	 * Override this method to be notified when an empty message has been
	 * received.
	 * 
	 * @param message the message
	 */
	public void receiveEmptyMessage(EmptyMessage message);
	
}

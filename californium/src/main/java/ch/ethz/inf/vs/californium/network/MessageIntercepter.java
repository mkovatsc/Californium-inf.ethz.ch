package ch.ethz.inf.vs.californium.network;

import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.layer.CoapStack;
import ch.ethz.inf.vs.californium.network.serializer.DataParser;
import ch.ethz.inf.vs.californium.network.serializer.Serializer;
import ch.ethz.inf.vs.elements.Connector;

/**
 * MessageIntercepters registers at an endpoint. When messages arrive from the
 * connector, the corresponding receive-method is called. When a message is
 * about to be sent over a connector, the corresponding send-method is called.
 * The intercepter can be sought of being placed inside an {@link CoAPEndpoint} just
 * between the message {@link Serializer} and the {@link Matcher}.
 * <p>
 * A <code>MessageInterceptor</code> can cancel a message to stop it. If it is
 * an outgoing message that traversed down through the {@link CoapStack} to the
 * <code>Matcher</code> and is now intercepted and canceled, will not reach the
 * {@link Connector}. If it is an incoming message coming from the
 * <code>Connector</code> to the {@link DataParser} and is now intercepted and
 * canceled, will not reach the <code>Matcher</code>.
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
	 * Override this method to be notified when an empty message is about to be
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

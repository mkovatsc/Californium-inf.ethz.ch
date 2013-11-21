
package ch.ethz.inf.vs.californium.coap;

/**
 * The observer interface for receiving events on a message.
 * <p>
 * The following methods are called
 * <ul>
 * <li> {@link #responded(Response)} when a response arrives</li>
 * <li> {@link #acknowledged()} when the message has been acknowledged</li>
 * <li> {@link #rejected()} when the message has been rejected</li>
 * <li> {@link #timeouted()} when the client stops retransmitting the message and
 * still has not received anything from the remote endpoint</li>
 * <li> {@link #canceled()} when the message has been canceled</li>
 * </ul>
 * <p>
 * The class that is interested in processing a message event either implements
 * this interface (and all the methods it contains) or extends the abstract
 * {@link MessageObserverAdapter} class (overriding only the methods of
 * interest).
 * <p>
 * The observer object created from that class is then registered with a message
 * using the message's {@link Message#addMessageObserver(MessageObserver)}
 * method.
 * <p>
 * Note: This class is unrelated to CoAP's observe relationship between an
 * endpoint and a resource. However, when a request establishes a CoAP observe
 * relationship to a resource which sends notifications, the method
 * {@link #responded(Response)} can be used to react to each such notification.
 */
public interface MessageObserver {

	public void retransmitting();
	
	/**
	 * Invoked when a response arrives.
	 * 
	 * @param response the response that arrives
	 */
	public void responded(Response response);

	/**
	 * Invoked when the message has been acknowledged by the remote endpoint.
	 */
	public void acknowledged();

	/**
	 * Invoked when the message has been rejected by the remote endpoint.
	 */
	public void rejected();

	/**
	 * Invoked when the client stops retransmitting the message and still has
	 * not received anything from the remote endpoint. By default this is the
	 * case after 5 unsuccessful transmission attempts.
	 */
	public void timeouted();

	/**
	 * Invoked when the message has been canceled. For instance, a user might
	 * cancel a request or a CoAP resource that is being observer might cancel a
	 * response to send another one instead.
	 */
	public void canceled();

}

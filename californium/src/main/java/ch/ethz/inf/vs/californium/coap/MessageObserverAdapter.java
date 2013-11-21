package ch.ethz.inf.vs.californium.coap;

/**
 * An abstract adapter class for reacting to message events. The methods in this
 * class are empty. This class exists as convenience for creating message
 * observer objects.
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
 * Extend this class to create a message observer and override the methods for
 * the events of interest. (If you implement the <code>MessageObserver</code>
 * interface, you have to define all of the methods in it. This abstract class
 * defines empty methods for them all, so you only have to define methods for
 * events you care about.)
 * <p>
 * Create a message observer using the extended class and then register it with
 * a message using the message's
 * <code>addMessageObserver(MessageObserver observer)</code> method.
 */
public abstract class MessageObserverAdapter implements MessageObserver {

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.coap.MessageObserver#retransmitting()
	 */
	@Override
	public void retransmitting() {
		// empty implementation
		
	}
	
	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.coap.MessageObserver#responded(ch.inf.vs.californium.coap.Response)
	 */
	@Override
	public void responded(Response response) {
		// empty implementation
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.coap.MessageObserver#acknowledged()
	 */
	@Override
	public void acknowledged() {
		// empty implementation
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.coap.MessageObserver#rejected()
	 */
	@Override
	public void rejected() {
		// empty implementation
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.coap.MessageObserver#timeouted()
	 */
	@Override
	public void timeouted() {
		// empty implementation
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.coap.MessageObserver#canceled()
	 */
	@Override
	public void canceled() {
		// empty implementation
	}
}

package ch.ethz.inf.vs.californium;

import ch.ethz.inf.vs.californium.coap.Request;

/**
 * A CoapObserveRelation represents a CoAP observe relation between a CoAP
 * client and a resource on a server. CoapObserveRelation provides a simple API
 * to check whether a relation has successfully established and to cancel or
 * refresh the relation.
 */
public class CoapObserveRelation {

	/** The request. */
	private Request request;
	
	/** Indicates whether the relation has been canceled. */
	private boolean canceled;
	
	/** The current notification. */
	private CoapResponse current;
	
	/**
	 * Constructs a new CoapObserveRelation with the specified request.
	 *
	 * @param request the request
	 */
	protected CoapObserveRelation(Request request) {
		this.request = request;
		this.canceled = false;
	}
	
	/**
	 * Refresh the relation to the resource.
	 */
	public void refresh() {
		// TODO: refresh this observe relation in case the server has forgotten
		// about it (send another GET request).
	}
	
	/**
	 * Cancel the observe relation.
	 */
	public void cancel() {
		request.cancel();
		setCanceled(true);
	}
	
	/**
	 * Checks if the relation has been canceled.
	 *
	 * @return true, if the relation has been canceled
	 */
	public boolean isCanceled() {
		return canceled;
	}
	
	/**
	 * Gets the current notification or null if none has arrived yet.
	 *
	 * @return the current notification
	 */
	public CoapResponse getCurrent() {
		return current;
	}
	
	/**
	 * Marks this relation as canceled.
	 *
	 * @param canceled true if this relation has been canceled
	 */
	protected void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}
	
	/**
	 * Sets the current notification.
	 *
	 * @param current the new current
	 */
	protected void setCurrent(CoapResponse current) {
		this.current = current;
	}
	
}

package ch.ethz.inf.vs.californium.network;

/**
 * An EndpointObserver registers on an endpoint to be notified when the endpoint
 * starts, stops or destroys itself.
 */
public interface EndpointObserver {

	/**
	 * This method is called when the endpoint starts.
	 *
	 * @param endpoint the endpoint
	 */
	public void started(Endpoint endpoint);
	
	/**
	 * This method is called when the endpoint stops.
	 *
	 * @param endpoint the endpoint
	 */
	public void stopped(Endpoint endpoint);
	
	/**
	 * This method is called when the endpoint is being destroyed.
	 *
	 * @param endpoint the endpoint
	 */
	public void destroyed(Endpoint endpoint);
	
}

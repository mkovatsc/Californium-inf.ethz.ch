package ch.ethz.inf.vs.californium.network;

/**
 * The exchange observer can be added to an {@link Exchange} and will be invoked
 * when it has completed, i.e. when the last response has been sent and
 * acknowledged or after the exchange lifecycle time.
 */
public interface ExchangeObserver {

	/**
	 * Invoked when the exchange has completed.
	 * 
	 * @param exchange
	 */
	public void completed(Exchange exchange);
	
}

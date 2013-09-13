package ch.ethz.inf.vs.californium.network.dedupl;

import java.util.concurrent.ScheduledExecutorService;

import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Exchange.KeyMID;

public interface Deduplicator {

	public void start();
	
	public void stop();
	
	public void setExecutor(ScheduledExecutorService executor);
	
	/**
	 * Checks if the specified key is already associated with a previous
	 * exchange and otherwise associates the key with the exchange specified. This method can also be though of as 'put if absent'.
	 * This is equivalent to
     * <pre>
     *   if (!duplicator.containsKey(key))
     *       return duplicator.put(key, value);
     *   else
     *       return duplicator.get(key);
     * </pre>
     * except that the action is performed atomically.
	 * 
	 * @param key the key
	 * @param exchange the exchange
	 * @return the previous exchange associated with the specified key, or
     *         <tt>null</tt> if there was no mapping for the key.
	 */
	public Exchange findPrevious(KeyMID key, Exchange exchange);
	
	public Exchange find(KeyMID key);
	
	public void clear();
}

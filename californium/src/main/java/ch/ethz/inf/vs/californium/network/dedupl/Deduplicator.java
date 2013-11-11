package ch.ethz.inf.vs.californium.network.dedupl;

import java.util.concurrent.ScheduledExecutorService;

import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Exchange.KeyMID;

/**
 * The deduplicator has to detect duplicates. Notice that CONs and NONs can be
 * duplicates.
 */
public interface Deduplicator {

	/**
	 * Starts the deduplicator
	 */
	public void start();
	
	/**
	 * Stops the deduplicator. The deduplicator should NOT clear its state.
	 */
	public void stop();
	
	/**
	 * Set the specified executor. This method might call stop(), replace the
	 * executor and then start() again.
	 * @param executor the executor
	 */
	public void setExecutor(ScheduledExecutorService executor);
	
	/**
	 * Checks if the specified key is already associated with a previous
	 * exchange and otherwise associates the key with the exchange specified. 
	 * This method can also be though of as 'put if absent'. This is equivalent 
	 * to
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
	
	/**
	 * Clears the state of this deduplicator.
	 */
	public void clear();
}

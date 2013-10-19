package ch.ethz.inf.vs.californium.network.dedupl;

import java.util.concurrent.ScheduledExecutorService;

import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Exchange.KeyMID;

/**
 * This is a dummy implementation that does no deduplication. If a matcher
 * does not want to deduplicate incoming messages, it should use this
 * deduplicator instead of 'null'.
 */
public class NoDeduplicator implements Deduplicator {

	@Override
	public void start() { }

	@Override
	public void stop() { }

	@Override
	public void setExecutor(ScheduledExecutorService executor) { }

	@Override
	public Exchange findPrevious(KeyMID key, Exchange exchange) {
		return null;
	}

	@Override
	public Exchange find(KeyMID key) {
		return null;
	}

	@Override
	public void clear() { }

}

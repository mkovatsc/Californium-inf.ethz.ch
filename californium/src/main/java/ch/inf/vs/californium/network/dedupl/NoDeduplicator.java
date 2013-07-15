package ch.inf.vs.californium.network.dedupl;

import java.util.concurrent.ScheduledExecutorService;

import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.Exchange.KeyMID;

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

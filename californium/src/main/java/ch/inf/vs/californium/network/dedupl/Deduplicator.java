package ch.inf.vs.californium.network.dedupl;

import java.util.concurrent.ScheduledExecutorService;

import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.Exchange.KeyMID;

public interface Deduplicator {

	public void start();
	
	public void stop();
	
	public void setExecutor(ScheduledExecutorService executor);
	
	public Exchange findPrevious(KeyMID key, Exchange exchange);
	
	public Exchange find(KeyMID key);
	
	public void clear();
}

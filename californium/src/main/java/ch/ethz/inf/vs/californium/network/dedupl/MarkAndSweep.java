package ch.ethz.inf.vs.californium.network.dedupl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.Server;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Exchange.KeyMID;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;

public class MarkAndSweep implements Deduplicator {

	private final static Logger LOGGER = CalifonriumLogger.getLogger(MarkAndSweep.class);
	
	private ConcurrentHashMap<KeyMID, Exchange> incommingMessages;
	
	private NetworkConfig config;
	private MarkAndSweepAlgorithm algorithm;
	
	private ScheduledExecutorService executor;
	
	private boolean started = false;
	
	public MarkAndSweep(NetworkConfig config) {
		this.config = config;
		incommingMessages = new ConcurrentHashMap<KeyMID, Exchange>();
		algorithm = new MarkAndSweepAlgorithm();
	}
	
	public void start() {
		started = true;
		algorithm.schedule();
	}
	
	public void stop() {
		started = false;
		algorithm.cancel();
	}
	
	public void setExecutor(ScheduledExecutorService executor) {
		stop();
		this.executor = executor;
		if (started)
			start();
	}
	
	public Exchange findPrevious(KeyMID key, Exchange exchange) {
		Exchange previous = incommingMessages.putIfAbsent(key, exchange);
		return previous;
	}
	
	public Exchange find(KeyMID key) {
		return incommingMessages.get(key);
	}
	
	public void clear() {
		incommingMessages.clear();
	}
	
	private class MarkAndSweepAlgorithm implements Runnable {

		private ScheduledFuture<?> future;
		
		@Override
		public void run() {
			try {
//				LOGGER.info("Start Mark-And-Sweep with "+incommingMessages.size()+" entries");
				markAndSweep();
//				long usedKB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024;
//				if (Server.LOG_ENABLED)
//					LOGGER.info("After Mark-And-Sweep: "
//						+ exchangesByMID.size()+" " + exchangesByToken.size()+ " "
//						+ ongoingExchanges.size()+ " " + incommingMessages.size());
//				System.gc();
				
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Exception in Mark-and-Sweep algorithm", t);
			
			} finally {
				try {
					schedule();
				} catch (Throwable t) {
					LOGGER.log(Level.WARNING, "Exception while scheduling Mark-and-Sweep algorithm", t);
				}
			}
		}
		
		private void markAndSweep() {
			int lifecycle = config.getInt(NetworkConfigDefaults.EXCHANGE_LIFECYCLE);
			long oldestAllowed = System.currentTimeMillis() - lifecycle;
			for (Map.Entry<?,Exchange> entry:incommingMessages.entrySet()) {
				Exchange exchange = entry.getValue();
				if (exchange.getTimestamp() < oldestAllowed) {
					
					// TODO: and not observe!!! Should we take ts of last message?
					
//					if (Server.LOG_ENABLED)
//						LOGGER.info("Mark-And-Sweep removes "+entry.getKey());
					
					incommingMessages.remove(entry.getKey());
				}
			}
		}
		
		private void schedule() {
			long period = config.getLong(NetworkConfigDefaults.MARK_AND_SWEEP_INTERVAL);
			if (Server.LOG_ENABLED)
				LOGGER.fine("MAS schedules in "+period+" ms");
			future = executor.schedule(this, period, TimeUnit.MILLISECONDS);
//			future = executor.scheduleWithFixedDelay(this, period, period, TimeUnit.MILLISECONDS);
		}
		
		private void cancel() {
			if (future != null)
				future.cancel(true);
		}
		
	}
	
}

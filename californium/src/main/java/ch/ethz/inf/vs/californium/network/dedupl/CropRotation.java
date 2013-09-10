package ch.ethz.inf.vs.californium.network.dedupl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Exchange.KeyMID;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;

public class CropRotation implements Deduplicator {

	private final static Logger LOGGER = CalifonriumLogger.getLogger(CropRotation.class);
	
	private ScheduledExecutorService executor;
	
	private ExchangeMap[] maps;
	private int first;
	private int second;
	
	private boolean started;

	private long period;
	private Rotation rotation;
	
	public CropRotation(NetworkConfig config) {
		this.rotation = new Rotation();
		maps = new ExchangeMap[3];
		maps[0] = new ExchangeMap();
		maps[1] = new ExchangeMap();
		maps[2] = new ExchangeMap();
		first = 0;
		second = 1;
		period = config.getInt(NetworkConfigDefaults.CROP_ROTATION_PERIOD);
	}
	
	@Override
	public synchronized void start() {
		started = true;
		rotation.schedule();
	}

	@Override
	public synchronized void stop() {
		started = false;
		rotation.cancel();
		clear();
	}

	@Override
	public synchronized void setExecutor(ScheduledExecutorService executor) {
		started = false;
		rotation.cancel();
		this.executor = executor;
		if (started)
			start();
	}

	@Override
	public Exchange findPrevious(KeyMID key, Exchange exchange) {
		int f = first;
		int s = second;
		Exchange prev = maps[f].putIfAbsent(key, exchange);
		if (prev != null || f==s) 
			return prev;
		prev = maps[s].putIfAbsent(key, exchange);
		return prev;
	}

	@Override
	public Exchange find(KeyMID key) {
		int f = first;
		int s = second;
		Exchange prev = maps[f].get(key);
		if (prev != null || f==s)
			return prev;
		prev = maps[s].get(key);
		return prev;
	}

	@Override
	public void clear() {
		maps[0].clear();
		maps[1].clear();
		maps[2].clear();
	}
	
	private class Rotation implements Runnable {
		
		private ScheduledFuture<?> future;
		
		public void run() {
			try {
				rotation();
				System.gc();
				
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Exception in Crop-Rotation algorithm", t);
			
			} finally {
				try {
					schedule();
				} catch (Throwable t) {
					LOGGER.log(Level.WARNING, "Exception while scheduling Crop-Rotation algorithm", t);
				}
			}
		}
		
		private void rotation() {
			int third = first;
			first = second;
			second = (second+1)%3;
			maps[third].clear();
		}
		
		private void schedule() {
			LOGGER.fine("CR schedules in "+period+" ms");
			future = executor.schedule(this, period, TimeUnit.MILLISECONDS);
		}
		
		private void cancel() {
			if (future != null)
				future.cancel(true);
		}
	}
	
	private static class ExchangeMap extends ConcurrentHashMap<KeyMID, Exchange> {}
}

package ch.ethz.inf.vs.californium.observe;

import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;

public class ObserveNotificationOrderer {

	private AtomicInteger number;
	
	/** The timestamp of the last response */
	private long timestamp;
	
	/** The current response */
	private Response response;
	
	public ObserveNotificationOrderer() {
		this.number = new AtomicInteger();
	}
	
	public synchronized void orderResponse(Response response) {
		if (this.response != null)
			this.response.cancel();
		this.response = response;
		response.getOptions().setObserve(getNextObserveNumber());
	}
	
	/**
	 * Return a new observe option number. This method is thread-safe as it
	 * increases the option number atomically.
	 * 
	 * @return a new observe option number
	 */
	public int getNextObserveNumber() {
		int next = number.incrementAndGet();
		while (next >= 1<<24) {
			number.compareAndSet(next, 0);
			next = number.incrementAndGet();
		}
		// assert 0 <= next && next < 1<<24;
		return next;
	}
	
	public int getCurrent() {
		return number.get();
	}
	
	public boolean compareAndSet(int expect, int update) {
		return number.compareAndSet(expect, update);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public synchronized boolean isNew(Response response) {
		// Multiple responses with different notification numbers might
		// arrive and be processed by different threads. We have to
		// ensure that only the most fresh one is being delivered.
		// We use the notation from the observe draft-08.
		long T1 = getTimestamp();
		long T2 = System.currentTimeMillis();
		int V1 = getCurrent();
		int V2 = response.getOptions().getObserve();
		int notifMaxAge = NetworkConfig.getStandard()
				.getInt(NetworkConfigDefaults.NOTIFICATION_MAX_AGE);
		if (V1 < V2 && V2 - V1 < 1<<23
				|| V1 > V2 && V1 - V2 > 1<<23
				|| T2 > T1 + notifMaxAge) {

			setTimestamp(T2);
			number.set(V2);
			// TODO: remove comments
//			System.out.println("T1="+T1+", T2="+T2+", V1="+V1+", V2="+V2+" => new");
			return true;
		} else {
//			System.out.println("T1="+T1+", T2="+T2+", V1="+V1+", V2="+V2+" => old");
			return false;
		}
	}
}

package ch.ethz.inf.vs.californium.observe;

import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.inf.vs.californium.coap.Response;

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
}

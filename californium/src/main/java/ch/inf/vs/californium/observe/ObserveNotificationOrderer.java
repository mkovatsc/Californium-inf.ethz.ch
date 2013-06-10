package ch.inf.vs.californium.observe;

import java.util.concurrent.atomic.AtomicInteger;

public class ObserveNotificationOrderer {

	private AtomicInteger number;
	
	private long timestamp;
	
	public ObserveNotificationOrderer() {
		this.number = new AtomicInteger();
	}
	
	public int getNextObserveNumber() {
		int next = number.incrementAndGet();
		while (next >= 1<<24) {
			number.compareAndSet(next, 0);
			next = number.incrementAndGet();
		}
		assert(0 <= next && next < 1<<24);
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

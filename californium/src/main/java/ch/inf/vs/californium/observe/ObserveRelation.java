package ch.inf.vs.californium.observe;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import ch.inf.vs.californium.DefaultMessageDeliverer;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.Resource;

public class ObserveRelation {

	// TODO: do we need synchronization?
	
	private final static Logger LOGGER = Logger.getLogger(ObserveRelation.class.getName());
	
	private final Exchange exchange;
	
	private final Set<Resource> resources;
	
	private AtomicInteger number = new AtomicInteger();
	
	public ObserveRelation(Exchange exchange) {
		if (exchange == null)
			throw new NullPointerException();
		this.exchange = exchange;
		this.resources = Collections.newSetFromMap(
				new ConcurrentHashMap<Resource, Boolean>());
	}
	
	public void notifyObservers(Resource resource) {
		resource.processRequest(exchange);
	}
	
	public boolean addResource(Resource resource) {
		return resources.add(resource);
	}
	
	public boolean removeResource(Resource resource) {
		return resources.remove(resource);
	}
	
	public void clear() {
		for (Resource resource:resources)
			resource.removeObserveRelation(this);
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
	
	@Override // TODO
	public int hashCode() {
		return exchange.getRequest().getSourcePort();
	}
	
	@Override // TODO
	public boolean equals(Object o) {
		if (! (o instanceof ObserveRelation))
			return false;
		ObserveRelation relation = (ObserveRelation) o;
		return exchange.getRequest().getSourcePort() ==
				relation.exchange.getRequest().getSourcePort();
	}
}

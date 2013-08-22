package ch.ethz.inf.vs.californium.observe;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ObserveRelationContainer implements Iterable<ObserveRelation> {
	
	private Set<ObserveRelation> observeRelations;
	
	public ObserveRelationContainer() {
		this.observeRelations = Collections.newSetFromMap(
				new ConcurrentHashMap<ObserveRelation,Boolean>());
	}
	
	public boolean add(ObserveRelation relation) {
		if (relation == null)
			throw new NullPointerException();
		return observeRelations.add(relation);
	}
	
	public boolean remove(ObserveRelation relation) {
		if (relation == null)
			throw new NullPointerException();
		return observeRelations.remove(relation);
	}
	
	public int getSize() {
		return observeRelations.size();
	}

	@Override
	public Iterator<ObserveRelation> iterator() {
		return observeRelations.iterator();
	}
	
	public Set<ObserveRelation> asSet() {
		Set<ObserveRelation> set = Collections.newSetFromMap(
				new ConcurrentHashMap<ObserveRelation,Boolean>());
		for (ObserveRelation relation:observeRelations)
			set.add(relation);
		return set;
	}
}

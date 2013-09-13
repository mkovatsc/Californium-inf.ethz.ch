package ch.ethz.inf.vs.californium.observe;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ch.ethz.inf.vs.californium.network.EndpointAddress;
import ch.ethz.inf.vs.californium.server.resources.Resource;

public class ObservingEndpoint {

	private final EndpointAddress address;

	private final ConcurrentHashMap<ResourcePath, ObserveRelation> relations;
	
	public ObservingEndpoint(EndpointAddress address) {
		this.address = address;
		this.relations = new ConcurrentHashMap<ResourcePath, ObserveRelation>();
	}
	
	public ObserveRelation findObserveRelation(List<String> path, Resource resource) {
		ResourcePath resourcePath = new ResourcePath(path);
		ObserveRelation relation = relations.get(resourcePath);
		if (relation == null) {
			relation = createObserveRelation(resource, resourcePath);
		}
		return relation;
	}
	
	public ObserveRelation getObserveRelation(List<String> path) {
		ResourcePath resourcePath = new ResourcePath(path);
		return relations.get(resourcePath);
	}
	
	public void removeObserveRelation(ObserveRelation relation) {
		ResourcePath resourcePath = new ResourcePath(relation.getPath());
		relations.remove(resourcePath);
	}
	
	public void cancelAll() {
		for (ObserveRelation relation:relations.values())
			relation.cancel();
	}
	
	private ObserveRelation createObserveRelation(Resource resource, ResourcePath path) {
		ObserveRelation relation = new ObserveRelation(this, resource, path.path);
		ObserveRelation previous = relations.putIfAbsent(path, relation);
		if (previous != null)
			return previous; // and forget relation
		else
			return relation;
	}
	
	
	// Me might make this an official class and also let it make a deep copy for safety
	private static class ResourcePath {
		
		private final List<String> path;
		
		private ResourcePath(List<String> path) {
			if (path == null)
				throw new NullPointerException();
			this.path = path;
		}
		
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof ResourcePath))
				return false;
			ResourcePath rp = (ResourcePath) o;
			return path.equals(rp.path);
		}
		
		@Override
		public int hashCode() {
			return path.hashCode();
		}
		
	}

	public EndpointAddress getAddress() {
		return address;
	}
}

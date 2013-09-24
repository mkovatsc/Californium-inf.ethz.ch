package ch.ethz.inf.vs.californium.observe;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ObservingEndpoint {
	
	private final InetSocketAddress address;

	private final List<ObserveRelation> relations;
	
	public ObservingEndpoint(InetSocketAddress address) {
		this.address = address;
		this.relations = new CopyOnWriteArrayList<ObserveRelation>();
	}
	
	public void addObserveRelation(ObserveRelation relation) {
		relations.add(relation);
	}
	
	public void removeObserveRelation(ObserveRelation relation) {
		relations.remove(relation);
	}
	
	public void cancelAll() {
		for (ObserveRelation relation:relations)
			relation.cancel();
	}

	public InetSocketAddress getAddress() {
		return address;
	}
	
	/*
	 * This class is obsolete now since observe-09 where a client can have
	 * multiple observe relations with the same resource. Furthermore, the
	 * methods above have become much simpler since there is close to no
	 * bookkeeping required.
	 */
//	private static class ResourcePath {
//		
//		private final List<String> path;
//		
//		private ResourcePath(List<String> path) {
//			if (path == null)
//				throw new NullPointerException();
//			this.path = path;
//		}
//		
//		@Override
//		public boolean equals(Object o) {
//			if (! (o instanceof ResourcePath))
//				return false;
//			ResourcePath rp = (ResourcePath) o;
//			return path.equals(rp.path);
//		}
//		
//		@Override
//		public int hashCode() {
//			return path.hashCode();
//		}
//	}
}

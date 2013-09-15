package ch.ethz.inf.vs.californium.server.resources;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.observe.ObserveRelation;

/**
 * 
 */
public interface Resource extends RequestProcessor {

	public void processRequest(Exchange exchange);
	
	public String getName();
	public void setName(String name);
	
	public String getPath();
	public void setPath(String path);
	
	public String getURI();
	
	public boolean isVisible();
	public boolean isCachable();
	public boolean isObservable();
	
	public ResourceAttributes getAttributes();
	
	public void add(Resource child);
	public boolean remove(Resource child);
	public Collection<Resource> getChildren();
	public Resource getChild(String name);
	public Resource getParent();
	public void setParent(Resource parent);
	
	public void addObserver(ResourceObserver observer);
	public void removeObserver(ResourceObserver observer);
	
	public void addObserveRelation(ObserveRelation relation);
	public void removeObserveRelation(ObserveRelation relation);
	
	public Executor getExecutor();
	
	public List<Endpoint> getEndpoints();

	// TODO: Under construction. Use ResourceBase to enjoy coding sugar
	public static class ResourceTreeBuilder {
		
		private Resource root;
		
		public ResourceTreeBuilder(Resource root) {
			this.root = root;
		}
		
//		public ResourceTreeBuilder add(Resource resource) {
//			root.add(resource);
//			return this;
//		}
//		
//		public ResourceTreeBuilder add(ResourceTreeBuilder builder) {
//			root.add(builder.create());
//			return this;
//		}
		
		public Resource create() {
//			b.add(A
//					.add(AA
//							.add(AAA)
//					.add(AB
//							.add(ABA)
//					))
			return root;
		}
	}
}

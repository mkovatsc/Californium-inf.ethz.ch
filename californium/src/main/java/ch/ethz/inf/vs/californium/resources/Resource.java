package ch.ethz.inf.vs.californium.resources;

import java.util.Collection;
import java.util.concurrent.Executor;

import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.observe.ObserveRelation;

public interface Resource {

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
	
	public void setExecutor(Executor executor);
	public Executor getExecutor();
}

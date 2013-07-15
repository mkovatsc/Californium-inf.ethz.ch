package ch.inf.vs.californium.resources;

import java.util.Collection;

import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.observe.ObserveRelation;

public interface Resource {

	public void processRequest(Exchange exchange);
	
	public void add(Resource child);
	public boolean remove(Resource child);
	public Collection<Resource> getChildren();
	public Resource getChild(String name);
	public Resource getParent();
	public void setParent(Resource parent);
	
	public void addObserver(ResourceObserver observer);
	public void removeObserver(ResourceObserver observer);
	
	public String getName();
	public void setName(String name);
	
	public String getPath();
	public void setPath(String path);
	
	public ResourceAttributes getAttributes();
	
	public boolean isVisible();
	
	public boolean isCachable();
	
	public boolean isObservable();
	
	public void addObserveRelation(ObserveRelation relation);
	public void removeObserveRelation(ObserveRelation relation);
	
}

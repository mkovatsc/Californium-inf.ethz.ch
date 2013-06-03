package ch.inf.vs.californium.resources;

import ch.inf.vs.californium.network.Exchange;

public interface Resource {

	public void processGET(Exchange exchange);
	
	public void processPOST(Exchange exchange);
	
	public void processPUT(Exchange exchange);
	
	public void processDELETE(Exchange exchange);
	
	public void add(Resource child);
	
	public boolean remove(Resource child);
	
//	public Resource remove(String name);
	
	public Resource getChild(String name);
	
	public Resource getParent();
	
	public void setParent(Resource parent);
	
	public void addObserver(ResourceObserver observer);
	
	public void removeObserver(ResourceObserver observer);
	
	public String getName();
	public void setName(String name);
	
	public String getPath();
	public void setPath(String path);
	
	public ResourceInfo getInfo();
	
	public boolean isHidden();
	
	public boolean isCachable();
	
	public boolean isAcceptRequestForChild();
	
}

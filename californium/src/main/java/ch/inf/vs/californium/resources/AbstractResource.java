package ch.inf.vs.californium.resources;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;

public abstract class AbstractResource implements Resource {

	private final ResourceInfo info;
	
	private String name;
	private String path;
	private boolean hidden;
	
	private Resource parent;
	private Map<String, Resource> children;
	
	private List<ResourceObserver> observers;
	
	private boolean acceptRequestsForChild;
	
	public AbstractResource(String name) {
		this(name, false);
	}
	
	public AbstractResource(String name, boolean hidden) {
		this.name = name;
		this.hidden = hidden;
		this.info = new ResourceInfo();
		this.children = new ConcurrentHashMap<>();
		this.observers = new CopyOnWriteArrayList<>();
	}
	
	@Override
	public void processGET(Exchange exchange) {
		exchange.respond(new Response(ResponseCode.METHOD_NOT_ALLOWED));
	}

	@Override
	public void processPOST(Exchange exchange) {
		exchange.respond(new Response(ResponseCode.METHOD_NOT_ALLOWED));

	}

	@Override
	public void processPUT(Exchange exchange) {
		exchange.respond(new Response(ResponseCode.METHOD_NOT_ALLOWED));

	}

	@Override
	public void processDELETE(Exchange exchange) {
		exchange.respond(new Response(ResponseCode.METHOD_NOT_ALLOWED));
	}

	@Override
	public synchronized void add(Resource child) {
		if (child.getName() == null)
			throw new NullPointerException("Child must have a name");
		if (child.getParent() != null)
			child.getParent().remove(child);
		children.put(child.getName(), child);
		child.setParent(this);
	}

	@Override
	public synchronized boolean remove(Resource child) {
		 Resource removed = remove(child.getName());
		 if (removed == child) {
			 child.setParent(null);
			 return true;
		 }
		 return false;
	}
	
	public synchronized Resource remove(String name) {
		return children.remove(name);
	}
	
	@Override
	public Resource getParent() {
		return parent;
	}
	
	public void setParent(Resource parent) {
		this.parent = parent;
	}
	
	@Override
	public Resource getChild(String name) {
		return children.get(name);
	}

	@Override
	public synchronized void addObserver(ResourceObserver observer) {
		observers.add(observer);
	}

	@Override
	public synchronized void removeObserver(ResourceObserver observer) {
		observers.remove(observer);
	}

	@Override
	public ResourceInfo getInfo() {
		return info;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isCachable() {
		return true;
	}

	public String getPath() {
		return path;
	}

	public synchronized void setPath(String path) {
		if (path == null)
			throw new NullPointerException();
		String old = this.path;
		this.path = path;
		for (ResourceObserver obs:observers)
			obs.pathChanged(this, old);
		adjustChildrenPath();
	}

	public synchronized void setName(String name) {
		if (name == null)
			throw new NullPointerException();
		String old = this.name;
		for (ResourceObserver obs:observers)
			obs.nameChanged(this, old);
		this.name = name;
		adjustChildrenPath();
	}
	
	private void adjustChildrenPath() {
		String childpath = path + name;
		for (Resource child:children.values())
			child.setPath(childpath);
	}
	
	@Override
	public boolean isHidden() {
		return hidden;
	}
	
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	@Override
	public boolean isAcceptRequestForChild() {
		return acceptRequestsForChild;
	}
	
	public void setDoesAcceptRequestForChild(boolean b) {
		acceptRequestsForChild = b;
	}
}

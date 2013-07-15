package ch.inf.vs.californium.resources;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.observe.ObserveRelation;
import ch.inf.vs.californium.observe.ObserveRelationContainer;

public  class ResourceBase implements Resource {

	/** The logger. */
	private final static Logger LOGGER = Logger.getLogger(ResourceBase.class.getName());
	
	private final ResourceAttributes attributes;
	
	private String name;
	private String path;
	private boolean hidden;
	private boolean observable;
	
	private Resource parent;
	
	// We need a ConcurrentHashMap to have stronger guarantees in a
	// multi-threaded environment (e.g. for discovery to work properly). 
	private ConcurrentHashMap<String, Resource> children;
	
	private List<ResourceObserver> observers;
	
	private ObserveRelationContainer observeRelations;
	
	public ResourceBase(String name) {
		this(name, false);
	}
	
	public ResourceBase(String name, boolean hidden) {
		this.name = name;
		this.path = name;
		this.hidden = hidden;
		this.attributes = new ResourceAttributes();
		this.children = new ConcurrentHashMap<>();
		this.observers = new CopyOnWriteArrayList<>();
		this.observeRelations = new ObserveRelationContainer();
	}
	
	@Override
	public void processRequest(Exchange exchange) {
		Code code = exchange.getRequest().getCode();
		switch (code) {
			case GET:	processGET(exchange); break;
			case POST:	processPOST(exchange); break;
			case PUT:	processPUT(exchange); break;
			case DELETE: processDELETE(exchange); break;
		}
	}

	public void processGET(Exchange exchange) {
		exchange.respond(new Response(ResponseCode.METHOD_NOT_ALLOWED));
	}

	public void processPOST(Exchange exchange) {
		exchange.respond(new Response(ResponseCode.METHOD_NOT_ALLOWED));
	}

	public void processPUT(Exchange exchange) {
		exchange.respond(new Response(ResponseCode.METHOD_NOT_ALLOWED));

	}

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
	public ResourceAttributes getAttributes() {
		return attributes;
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
	
//	@Override
//	public boolean isAcceptRequestForChild() {
//		return acceptRequestsForChild;
//	}
//	
//	public void setDoesAcceptRequestForChild(boolean b) {
//		acceptRequestsForChild = b;
//	}
	
	@Override
	public boolean isObservable() {
		return observable;
	}

	public void setObservable(boolean observable) {
		this.observable = observable;
	}

	@Override
	public void addObserveRelation(ObserveRelation relation) {
		observeRelations.add(relation);
	}

	@Override
	public void removeObserveRelation(ObserveRelation relation) {
		observeRelations.remove(relation);
	}
	
	public int getObserverCount() {
		return observeRelations.getSize();
	}
	
	public void changed() {
		for (ObserveRelation relation:observeRelations) {
			relation.notifyObservers();
		}
	}

	@Override // should be used for read-only
	public Collection<Resource> getChildren() {
//		return new ArrayList<>(children.values());
		return children.values();
	}
}

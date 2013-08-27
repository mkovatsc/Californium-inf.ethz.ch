package ch.ethz.inf.vs.californium.resources;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.observe.ObserveRelation;
import ch.ethz.inf.vs.californium.observe.ObserveRelationContainer;

public  class ResourceBase implements Resource {

	/** The logger. */
//	private final static Logger LOGGER = Logger.getLogger(ResourceBase.class.getName());
	
	private final ResourceAttributes attributes;
	
	private String name;
	private String path;
	private boolean visible;
	private boolean observable;
	
	// We need a ConcurrentHashMap to have stronger guarantees in a
	// multi-threaded environment (e.g. for discovery to work properly). 
	private ConcurrentHashMap<String, Resource> children;
	private Resource parent;
	
	private List<ResourceObserver> observers;
	
	private ObserveRelationContainer observeRelations;
	
	private Executor executor;
	
	public ResourceBase(String name) {
		this(name, true);
	}
	
	public ResourceBase(String name, boolean visible) {
		this.name = name;
		this.path = "";
		this.visible = visible;
		this.attributes = new ResourceAttributes();
		this.children = new ConcurrentHashMap<String, Resource>();
		this.observers = new CopyOnWriteArrayList<ResourceObserver>();
		this.observeRelations = new ObserveRelationContainer();
	}
	
	@Override
	public void processRequest(final Exchange exchange) {
		Executor executor = getExecutor();
		if (executor != null) {
			executor.execute(new Runnable() {
				public void run() {
					processRequestImpl(exchange);
				} });
		} else {
			processRequestImpl(exchange);
		}
	}
	
	protected void processRequestImpl(Exchange exchange) {
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
	
	public void respond(Exchange exchange, Response response) {
		exchange.respond(response);
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
	
	public synchronized ResourceBase add(ResourceBase child) {
		add( (Resource) child);
		return this;
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
	
	public synchronized void delete() {
		Resource parent = getParent();
		if (parent != null) {
			parent.remove(this);
		}
		
		if (isObservable()) {
			clearAndNotifyObserveRelations();
		}
	}
	
	public void clearAndNotifyObserveRelations() {
		/*
		 * draft-ietf-core-observe-08, chapter 3.2 Notification states:
		 * In the event that the resource changes in a way that would cause
		 * a normal GET request at that time to return a non-2.xx response
		 * (for example, when the resource is deleted), the server sends a
		 * notification with a matching response code and removes the client
		 * from the list of observers.
		 */
		for (ObserveRelation relation:observeRelations) {
			relation.cancel();
			relation.getExchange().respond(ResponseCode.NOT_FOUND);
		}
	}
	
	public void clearObserveRelations() {
		for (ObserveRelation relation:observeRelations) {
			relation.cancel();
		}
	}
	
	@Override
	public Resource getParent() {
		return parent;
	}
	
	public void setParent(Resource parent) {
		this.parent = parent;
		if (parent != null)
			this.path = parent.getPath()  + parent.getName() + "/";
		adjustChildrenPath();
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

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getURI() {
		return getPath() + getName();
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
		this.name = name;
		for (ResourceObserver obs:observers)
			obs.nameChanged(this, old);
		adjustChildrenPath();
	}
	
	private void adjustChildrenPath() {
		String childpath = path + name + /*since 23.7.2013*/ "/";
		for (Resource child:children.values())
			child.setPath(childpath);
	}
	
	@Override
	public boolean isVisible() {
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
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
		return children.values();
	}
	
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}
	
	public Executor getExecutor() {
		if (executor!=null) return executor;
		else return parent != null ? parent.getExecutor() : null;
	}
}

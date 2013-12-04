package ch.ethz.inf.vs.californium.server.resources;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CoapClient;
import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.observe.ObserveNotificationOrderer;
import ch.ethz.inf.vs.californium.observe.ObserveRelation;
import ch.ethz.inf.vs.californium.observe.ObserveRelationContainer;

/**
 * ResourceBase is a basic implementation of a resource. Extend this class to
 * write your own resources. Instances of type or subtype of ResourceBase can be
 * built up to a tree very easily, see {@link #add(ResourceBase)}.
 * <p>
 * ResourceBase uses four distinct methods to handle requests:
 * <tt>handleGET()</tt>, <tt>handlePOST()</tt>, <tt>handlePUT()</tt> and
 * <tt>handleDELETE()</tt>. Each method has a default implementation that
 * responds with a 4.05 (Method Not Allowed). Each method exists twice but with
 * a different parameter: <tt>handleGET(Exchange)</tt> and
 * <tt>handleGET(CoAPExchange)</tt> for instance. The class {@link Exchange} is
 * used internally in Californium to keep the state of an exchange of CoAP
 * messages. Only override this version of the method if you need to access
 * detailed information of an exchange. Most developer should rather override
 * the latter version. CoAPExchange provides a save and user-friendlier API that
 * can be used to respond to a request.
 * <p>
 * The following example override the four handle-method.
 * <pre>
 * public class CoAPResourceExample extends ResourceBase {
 * 
 *   public CoAPResourceExample(String name) {
 *     super(name);
 *   }
 * 
 *   public void handleGET(CoapExchange exchange) {
 *     exchange.respond("hello world");
 *   }
 * 
 *   public void handlePOST(CoapExchange exchange) {
 *     exchange.accept();
 * 
 *     List<String> queries = exchange.getRequestOptions().getURIQueries();
 *     // ...
 *     exchange.respond(ResponseCode.CREATED);
 *   }
 * 
 *   public void handlePUT(CoapExchange exchange) {
 *     // ...
 *     exchange.respond(ResponseCode.CHANGED);
 *     changed(); // notify all observers
 *   }
 * 
 *   public void handleDELETE(CoapExchange exchange) {
 *     delete();
 *     exchange.respond(ResponseCode.DELETED);
 *   }
 * }
 * </pre>
 * <p>
 * Each resource is allowed to define its own executor. When a request arrives,
 * the request will be handled by the resource's executor. If a resource does
 * not define its own executor, the executor of its parent or transitively an
 * ancestor will be used. If no ancestor up to the root defines its own
 * executor, the thread that delivers the request will invoke the handling
 * method.
 * <p>
 * ResourceBase supports CoAP's observe mechanism. Enable a ResourceBase to be
 * observable by a CoAP client by marking it as observable with
 * {@link #setObservable(boolean)}. Notify all CoAP observers by calling
 * {@link #changed()}. The method changed() reprocesses the requests from the
 * observing clients that have originally established the observe relation. If
 * the resource or one of its ancestors define an executor, the reprocessing is
 * done on the executor. A CoAP observe relation between this resource and a
 * CoAP client is represented by an instance of {@link ObserveRelation}.
 * <p>
 * In contrast the class {@link ResourceObserver} has nothing to do with CoAP's
 * observe mechanism but is an implementation of the general observe-pattern. A
 * ResourceObserver is invoked whenever the name or path of a resource changes,
 * when a child resource is added or removed or when a CoAP observe relation is
 * added or canceled.
 * // TODO: make example with createClient().get() 
 */
public  class ResourceBase implements Resource {

	/** The logger. */
	protected final static Logger LOGGER = Logger.getLogger(ResourceBase.class.getCanonicalName());
	
	/** The attributes of this resource. */
	private final ResourceAttributes attributes;
	
	/** The resource name. */
	private String name;
	
	/** The resource path. */
	private String path;
	
	/** Indicates whether this resource is visible to clients. */
	private boolean visible;
	
	/** Indicates whether this resource is observable by clients. */
	private boolean observable;
	
	// We need a ConcurrentHashMap to have stronger guarantees in a
	// multi-threaded environment (e.g. for discovery to work properly). 
	/** The child resources */
	private ConcurrentHashMap<String, Resource> children;
	
	/** The parent of this resource. */
	private Resource parent;
	
	/** The list of observers (not CoAP observer). */
	private List<ResourceObserver> observers;

	/** The the list of CoAP observe relations. */
	private ObserveRelationContainer observeRelations;
	
	/** The notification orderer. */
	private ObserveNotificationOrderer notificationOrderer;
	
	/**
	 * Constructs a new resource with the specified name.
	 *
	 * @param name the name
	 */
	public ResourceBase(String name) {
		this(name, true);
	}
	
	/**
	 * Constructs a new resource with the specified name and makes it visible to
	 * clients if the flag is true.
	 * 
	 * @param name the name
	 * @param visible if the resource is visible
	 */
	public ResourceBase(String name, boolean visible) {
		this.name = name;
		this.path = "";
		this.visible = visible;
		this.attributes = new ResourceAttributes();
		this.children = new ConcurrentHashMap<String, Resource>();
		this.observers = new CopyOnWriteArrayList<ResourceObserver>();
		this.observeRelations = new ObserveRelationContainer();
		this.notificationOrderer = new ObserveNotificationOrderer();
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#handleRequest(ch.ethz.inf.vs.californium.network.Exchange)
	 */
	@Override
	public void handleRequest(final Exchange exchange) {
		Code code = exchange.getRequest().getCode();
		switch (code) {
			case GET:	handleGET(exchange); break;
			case POST:	handlePOST(exchange); break;
			case PUT:	handlePUT(exchange); break;
			case DELETE: handleDELETE(exchange); break;
		}
	}
	
	/**
	 * Handles the GET request in the given exchange. By default it responds
	 * with a 4.05 (Method Not Allowed). Override this method if the GET request
	 * handling of your resource implementation requires the internal state of
	 * the exchange. Most developer should be better off with overriding this'
	 * method's sibling {@link #handleGET(CoapExchange)} that uses a parameter
	 * with a simpler and less error-prone API.
	 * 
	 * @param exchange the exchange with the GET request
	 */
	public void handleGET(Exchange exchange) {
		handleGET(new CoapExchange(exchange, this));
	}

	/**
	 * Handles the POST request in the given exchange. By default it responds
	 * with a 4.05 (Method Not Allowed). Override this method if the POST
	 * request handling of your resource implementation requires the internal
	 * state of the exchange. Most developer should be better off with
	 * overriding this' method's sibling {@link #handlePOST(CoapExchange)} that
	 * uses a parameter with a simpler and less error-prone API.
	 * 
	 * @param exchange the exchange with the POST request
	 */
	public void handlePOST(Exchange exchange) {
		handlePOST(new CoapExchange(exchange, this));
	}

	/**
	 * Handles the PUT request in the given exchange. By default it responds
	 * with a 4.05 (Method Not Allowed). Override this method if the PUT request
	 * handling of your resource implementation requires the internal state of
	 * the exchange. Most developer should be better off with overriding this'
	 * method's sibling {@link #handlePUT(CoapExchange)} that uses a parameter
	 * with a simpler and less error-prone API.
	 * 
	 * @param exchange the exchange with the PUT request
	 */
	public void handlePUT(Exchange exchange) {
		handlePUT(new CoapExchange(exchange, this));
	}

	/**
	 * Handles the DELETE request in the given exchange. Override this method if
	 * the DELETE request handling of your resource implementation requires the
	 * internal state of the exchange. Most developer should be better off with
	 * overriding this' method's sibling {@link #handleDELETE(CoapExchange)} that
	 * uses a parameter with a simpler and less error-prone API.
	 *
	 * @param exchange the exchange with the DELETE request
	 */
	public void handleDELETE(Exchange exchange) {
		handleDELETE(new CoapExchange(exchange, this));
	}
	
	/**
	 * Handles the GET request in the given CoAPExchange. By default it
	 * responds with a 4.05 (Method Not Allowed). Override this method to
	 * respond differently to GET requests.
	 * 
	 * @param exchange the exchange
	 */
	public void handleGET(CoapExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
	}
	
	/**
	 * Hanldes the POST request in the given CoAPExchange. By default it
	 * responds with a 4.05 (Method Not Allowed). Override this method to
	 * respond differently to POST requests.
	 *
	 * @param exchange the exchange
	 */
	public void handlePOST(CoapExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
	}
	
	/**
	 * Hanldes the PUT request in the given CoAPExchange. By default it
	 * responds with a 4.05 (Method Not Allowed). Override this method to
	 * respond differently to PUT requests.
	 *
	 * @param exchange the exchange
	 */
	public void handlePUT(CoapExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
	}
	
	/**
	 * Handles the DELETE request in the given CoAPExchange. By default it
	 * responds with a 4.05 (Method Not Allowed). Override this method to
	 * respond differently to DELETE requests.
	 *
	 * @param exchange the exchange
	 */
	public void handleDELETE(CoapExchange exchange) {
		exchange.respond(ResponseCode.METHOD_NOT_ALLOWED);
	}
	
	/**
	 * Send the specified response back to the client of the specified exchange.
	 * This method handles the resource's observe relations to clients, i.e.
	 * establishing or canceling them.
	 * 
	 * @param exchange the exchange
	 * @param response the response
	 */
	protected void respond(Exchange exchange, Response response) {
		if (exchange == null) throw new NullPointerException();
		if (response == null) throw new NullPointerException();
		checkObserveRelation(exchange, response);
		exchange.respond(response);
	}
	
	/**
	 * Check the observe relation status of the specified exchange according to
	 * the specified response. If this resource allows to be observed by clients
	 * and the request is a GET request with an observe option and the response
	 * has a successful response code, this method adds the observer relations
	 * to this resource. In any other case, no observe relation can be
	 * established and if there was one previously, it is canceled.
	 * 
	 * @param exchange the exchange
	 * @param response the response
	 */
	private void checkObserveRelation(Exchange exchange, Response response) {
		/*
		 * If the request for the specified exchange tries to establish an observer
		 * relation, then the ServerMessageDeliverer must have created such a relation
		 * and added to the exchange. Otherwise, there is no such relation.
		 * Remember that different paths might lead to this resource.
		 */
		
		ObserveRelation relation = exchange.getRelation();
		if (relation == null) return; // because request did not try to establish a relation
		
		if (CoAP.ResponseCode.isSuccess(response.getCode())) {
			response.getOptions().setObserve(notificationOrderer.getCurrent());
			
			if (!relation.isEstablished()) {
				LOGGER.info("Successfully established observe relation between "+relation.getSource()+" and resource "+getURI());
				relation.setEstablished(true);
				addObserveRelation(relation);
			} else {
				// Cancel previous response in case it has been lost and is
				// about to be retransmitted.
//				Response prev = exchange.getResponse(); // We no longer do this since the ObserveLayer takes care of that
//				if (prev != null) prev.cancel();
			}
		
		} else {
			// The request would like to establish an observe relation but the response
			// was not successful.
			LOGGER.info("Response code "+response.getCode()+"prevented observe relation between "+relation.getSource()+" and resource "+getURI());
			relation.cancel();
		}
	}
	
	/**
	 * Creates a {@link CoapClient} that uses the same executor as this resource
	 * and one of the endpoints that this resource belongs to. If no executor is
	 * defined by this resource or any parent, the client will not have an
	 * executor (it still works). If this resource is not yet added to a server
	 * or the server has no endpoints, the client has no specific endpoint and
	 * will use Californium's default endpoint.
	 * 
	 * @return the CoAP client
	 */
	public CoapClient createClient() {
		CoapClient client = new CoapClient();
		client.setExecutor(getExecutor());
		List<Endpoint> endpoints = getEndpoints();
		if (!endpoints.isEmpty())
			client.setEndpoint(endpoints.get(0));
		return client;
	}
	
	
	/**
	 * Creates a {@link CoapClient} that uses the same executor as this resource
	 * and one of the endpoints that this resource belongs to. If no executor is
	 * defined by this resource or any parent, the client will not have an
	 * executor (it still works). If this resource is not yet added to a server
	 * or the server has no endpoints, the client has no specific endpoint and
	 * will use Californium's default endpoint.
	 * 
	 * @param uri the uri
	 * @return the CoAP client
	 */
	public CoapClient createClient(URI uri) {
		return createClient().setUri(uri.toString());
	}
	
	/**
	 * Creates a {@link CoapClient} that uses the same executor as this resource
	 * and one of the endpoints that this resource belongs to. If no executor is
	 * defined by this resource or any parent, the client will not have an
	 * executor (it still works). If this resource is not yet added to a server
	 * or the server has no endpoints, the client has no specific endpoint and
	 * will use Californium's default endpoint.
	 *
	 * @param uri the uri
	 * @return the CoAP client
	 */
	public CoapClient createClient(String uri) {
		return createClient().setUri(uri);
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#add(ch.ethz.inf.vs.californium.server.resources.Resource)
	 */
	@Override
	public synchronized void add(Resource child) {
		if (child.getName() == null)
			throw new NullPointerException("Child must have a name");
		if (child.getParent() != null)
			child.getParent().remove(child);
		children.put(child.getName(), child);
		child.setParent(this);
		for (ResourceObserver obs:observers)
			obs.addedChild(child);
	}
	
	/**
	 * Adds the specified resource as child. This method is syntactic sugar to
	 * have a fluent-interface when adding resources to a tree. For instance,
	 * consider the following example:
	 * 
	 * <pre>
	 * server.add(
	 *   new ResourceBase("foo")
	 *     .add(new ResourceBase("a")
	 *       .add(new ResourceBase("a1"))
	 *       .add(new ResourceBase("a2"))
	 *       .add(new ResourceBase("a3"))
	 *       .add(new ResourceBase("a4"))
	 *     )
	 *     .add(new ResourceBase("b")
	 *       .add(new ResourceBase("b1")
	 *     )
	 *   )
	 * );
	 * </pre>
	 * 
	 * @param child the child
	 * @return this
	 */
	public synchronized ResourceBase add(ResourceBase child) {
		add( (Resource) child);
		return this;
	}
	
	/**
	 * Adds the specified resource as child. This method is syntactic sugar to
	 * have a fluent-interface when adding resources to a tree. For instance,
	 * consider the following example:
	 * 
	 * <pre>
	 * server.add(
	 *   new ResourceBase("foo").add(
	 *     new ResourceBase("a").add(
	 *       new ResourceBase("a1"),
	 *       new ResourceBase("a2"),
	 *       new ResourceBase("a3"),
	 *       new ResourceBase("a4")
	 *     ),
	 *     new ResourceBase("b").add(
	 *       new ResourceBase("b1")
	 *     )
	 *   )
	 * );
	 * </pre>
	 * 
	 * @param child the child
	 * @return this
	 */
	public synchronized ResourceBase add(ResourceBase... children) {
		for (ResourceBase child:children)
			add(child);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#remove(ch.ethz.inf.vs.californium.server.resources.Resource)
	 */
	@Override
	public synchronized boolean remove(Resource child) {
		Resource removed = remove(child.getName());
		if (removed == child) {
			child.setParent(null);
			child.setPath(null);
			for (ResourceObserver obs : observers)
				obs.removedChild(child);
			return true;
		}
		return false;
	}
	
	/**
	 * Removes the child with the specified name and returns it. If no child
	 * with the specified name is found, the return value is null.
	 * 
	 * @param name the name
	 * @return the removed resource or null
	 */
	public synchronized Resource remove(String name) {
		return children.remove(name);
	}
	
	/**
	 * Delete this resource from its parents and notify all observing CoAP
	 * clients that this resource is no longer accessible.
	 */
	public synchronized void delete() {
		Resource parent = getParent();
		if (parent != null) {
			parent.remove(this);
		}
		
		if (isObservable()) {
			clearAndNotifyObserveRelations();
		}
	}
	
	/**
	 * Remove all observe relations to CoAP clients and notify them that the
	 * observe relation has been canceled.
	 */
	public void clearAndNotifyObserveRelations() {
		/*
		 * draft-ietf-core-observe-08, chapter 3.2 Notification states:
		 * In the event that the resource changes in a way that would cause
		 * a normal GET request at that time to return a non-2.xx response
		 * (for example, when the resource is deleted), the server sends a
		 * notification with a matching response code and removes the client
		 * from the list of observers.
		 * This method is called, when the resource is deleted.
		 */
		for (ObserveRelation relation:observeRelations) {
			relation.cancel();
			relation.getExchange().respond(ResponseCode.NOT_FOUND);
		}
	}
	
	/**
	 * Cancel all observe relations to CoAP clients.
	 */
	public void clearObserveRelations() {
		for (ObserveRelation relation:observeRelations) {
			relation.cancel();
		}
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#getParent()
	 */
	@Override
	public Resource getParent() {
		return parent;
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#setParent(ch.ethz.inf.vs.californium.server.resources.Resource)
	 */
	public void setParent(Resource parent) {
		this.parent = parent;
		if (parent != null)
			this.path = parent.getPath()  + parent.getName() + "/";
		adjustChildrenPath();
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#getChild(java.lang.String)
	 */
	@Override
	public Resource getChild(String name) {
		return children.get(name);
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#addObserver(ch.ethz.inf.vs.californium.server.resources.ResourceObserver)
	 */
	@Override
	public synchronized void addObserver(ResourceObserver observer) {
		observers.add(observer);
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#removeObserver(ch.ethz.inf.vs.californium.server.resources.ResourceObserver)
	 */
	@Override
	public synchronized void removeObserver(ResourceObserver observer) {
		observers.remove(observer);
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#getAttributes()
	 */
	@Override
	public ResourceAttributes getAttributes() {
		return attributes;
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#isCachable()
	 */
	@Override
	public boolean isCachable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#getPath()
	 */
	@Override
	public String getPath() {
		return path;
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#getURI()
	 */
	@Override
	public String getURI() {
		return getPath() + getName();
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#setPath(java.lang.String)
	 */
	public synchronized void setPath(String path) {
		String old = this.path;
		this.path = path;
		for (ResourceObserver obs:observers)
			obs.changedPath(old);
		adjustChildrenPath();
	}

	// If the parent already has a child with that name, the behavior is undefined
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#setName(java.lang.String)
	 */
	public synchronized void setName(String name) {
		if (name == null)
			throw new NullPointerException();
		String old = this.name;
		Resource parent = getParent();
		synchronized (parent) {
			parent.remove(this);
			this.name = name;
			parent.add(this);
		}
		for (ResourceObserver obs:observers)
			obs.changedName(old);
		adjustChildrenPath();
	}
	
	/**
	 * Adjust the path of all children. This method is invoked when the URI of
	 * this resource has changed, e.g., if its name or the name of an ancestor
	 * has changed.
	 */
	private void adjustChildrenPath() {
		String childpath = path + name + /*since 23.7.2013*/ "/";
		for (Resource child:children.values())
			child.setPath(childpath);
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#isVisible()
	 */
	@Override
	public boolean isVisible() {
		return visible;
	}
	
	/**
	 * Marks this resource as visible to CoAP clients.
	 *
	 * @param visible true if visible
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#isObservable()
	 */
	@Override
	public boolean isObservable() {
		return observable;
	}

	/**
	 * Marks this resource as observable by CoAP clients.
	 *
	 * @param observable true if observable
	 */
	public void setObservable(boolean observable) {
		this.observable = observable;
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#addObserveRelation(ch.ethz.inf.vs.californium.observe.ObserveRelation)
	 */
	@Override
	public void addObserveRelation(ObserveRelation relation) {
		observeRelations.add(relation);
		for (ResourceObserver obs:observers)
			obs.addedObserveRelation(relation);
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#removeObserveRelation(ch.ethz.inf.vs.californium.observe.ObserveRelation)
	 */
	@Override
	public void removeObserveRelation(ObserveRelation relation) {
		observeRelations.remove(relation);
		for (ResourceObserver obs:observers)
			obs.removedObserveRelation(relation);
	}
	
	/**
	 * Returns the number of observe realtions that this resource has to CoAP
	 * clients.
	 * 
	 * @return the observer count
	 */
	public int getObserverCount() {
		return observeRelations.getSize();
	}
	
	/**
	 * Notifies all CoAP clients that have established an observe relation with
	 * this resource that the state has changed by reprocessing their original
	 * request that has established the relation. The notification is done by
	 * the executor of this resource or on the executor of its parent or
	 * transitively ancestor. If no ancestor defines its own executor, the
	 * thread that has called this method performs the notification.
	 */
	public void changed() {
		Executor executor = getExecutor();
		if (executor != null) {
			executor.execute(new Runnable() {
				public void run() {
					notifyObserverRelations();
				}
			});
		} else {
			notifyObserverRelations();
		}
	}
	
	/**
	 * Notifies all CoAP clients that have established an observe relation with
	 * this resource that the state has changed by reprocessing their original
	 * request that has established the relation.
	 */
	protected void notifyObserverRelations() {
		notificationOrderer.getNextObserveNumber();
		for (ObserveRelation relation:observeRelations) {
			relation.notifyObservers();
		}
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#getChildren()
	 */
	@Override // should be used for read-only
	public Collection<Resource> getChildren() {
		return children.values();
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#getExecutor()
	 */
	public Executor getExecutor() {
		return parent != null ? parent.getExecutor() : null;
	}
	
	/**
	 * Execute an arbitrary task on the executor of this resource or the first
	 * parent that defines its own executor. If no parent defines an executor,
	 * the thread that calls this method executes the specified task.
	 * 
	 * @param task the task
	 */
	public void execute(Runnable task) {
		Executor executor = getExecutor();
		if (executor != null)
			executor.execute(task);
		else task.run();
	}
	
	/**
	 * Execute an arbitrary task on the executor of this resource or the first
	 * parent that defines its own executor and wait until it the task is
	 * completed. If no parent defines an executor, the thread that calls this
	 * method executes the specified task.
	 * 
	 * @param task the task
	 * @throws InterruptedException the interrupted exception
	 */
	public void executeAndWait(final Runnable task) throws InterruptedException {
		final Semaphore semaphore = new Semaphore(0);
		execute(new Runnable() {
			public void run() {
				task.run();
				semaphore.release();
			}
		});
		semaphore.acquire();
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.Resource#getEndpoints()
	 */
	public List<Endpoint> getEndpoints() {
		if (parent == null)
			return Collections.emptyList();
		else return parent.getEndpoints();
	}
}

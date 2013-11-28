package ch.ethz.inf.vs.californium;

import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.MessageObserverAdapter;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.observe.ObserveNotificationOrderer;

/**
 * The Class CoapClient.
 */
public class CoapClient {

	/** The logger. */
	private static final Logger LOGGER = Logger.getLogger(CoapClient.class.getCanonicalName());
	
	/** The timeout. */
	private long timeout = NetworkConfig.getStandard()
		.getLong(NetworkConfigDefaults.COAP_CLIENT_DEFAULT_TIMEOUT);
	
	/** The destination URI */
	private String uri;
	
	/** The executor. */
	private Executor executor;
	
	/** The endpoint. */
	private Endpoint endpoint;
	
	/**
	 * Constructs a new CoapClient that has no destination URI yet.
	 */
	public CoapClient() {
		this("");
	}
	
	/**
	 * Constructs a new CoapClient that sends requests to the specified URI.
	 *
	 * @param uri the uri
	 */
	public CoapClient(String uri) {
		this.uri = uri;
	}
	
	/**
	 * Constructs a new CoapClient that sends request to the specified URI.
	 * 
	 * @param uri the uri
	 */
	public CoapClient(URI uri) {
		this(uri.toString());
	}
	
	/**
	 * Constructs a new CoapClient with the specified scheme, host, port and 
	 * path as URI.
	 *
	 * @param scheme the scheme
	 * @param host the host
	 * @param port the port
	 * @param path the path
	 */
	public CoapClient(String scheme, String host, int port, String... path) {
		StringBuilder builder = new StringBuilder()
			.append(scheme).append("://").append(host).append(":").append(port);
		for (String element:path)
			builder.append("/").append(element);
		this.uri = builder.toString();
	}
	
	// Synchronous GET
	
	/**
	 * Sends a GET request and waits for the response.
	 *
	 * @return the coap response
	 */
	public CoapResponse get() {
		return synchronous(Request.newGet().setURI(uri));
	}
	
	/**
	 * Sends a GET request with the specified accept option and waits for the
	 * response.
	 * 
	 * @param accept the accept option
	 * @return the coap response
	 */
	public CoapResponse get(int accept) {
		return synchronous(accept(Request.newGet().setURI(uri), accept));
	}
	
	// Asynchronous GET
	
	/**
	 * Sends a GET request and invokes the specified handler when a response
	 * arrives.
	 *
	 * @param handler the handler
	 */
	public void get(CoapHandler handler) {
		asynchronous(Request.newGet().setURI(uri), handler);
	}
	
	/**
	 * Sends  aGET request with the specified accept option and invokes the
	 * handler when a response arrives.
	 *
	 * @param accept the accept option
	 * @param handler the handler
	 */
	public void get(int accept, CoapHandler handler) {
		asynchronous(accept(Request.newGet().setURI(uri), accept), handler);
	}
	
	// Synchronous POST
	
	/**
	 * Sends a POST request with the specified payload and waits for the 
	 * response.
	 *
	 * @param payload the payload
	 * @return the coap response
	 */
	public CoapResponse post(String payload) {
		return synchronous(Request.newPost().setURI(uri).setPayload(payload));
	}
	
	/**
	 * Sends a POST request with the specified payload and waits for the 
	 * response.
	 *
	 * @param payload the payload
	 * @return the coap response
	 */
	public CoapResponse post(byte[] payload) {
		return synchronous(Request.newPost().setURI(uri).setPayload(payload));
	}
	
	/**
	 * Sends a POST request with the specified payload and the specified content
	 * format option and waits for the response.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @return the coap response
	 */
	public CoapResponse post(String payload, int format) {
		return synchronous(format(Request.newPost().setURI(uri).setPayload(payload), format));
	}
	
	/**
	 * Sends a POST request with the specified payload and the specified content
	 * format option and waits for the response.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @return the coap response
	 */
	public CoapResponse post(byte[] payload, int format) {
		return synchronous(format(Request.newPost().setURI(uri).setPayload(payload), format));
	}
	
	/**
	 * Sends a POST request with the specified payload, the specified content
	 * format and the specified accept option and waits for the response.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @param accept the accept option
	 * @return the coap response
	 */
	public CoapResponse post(String payload, int format, int accept) {
		return synchronous(accept(format(Request.newPost().setURI(uri).setPayload(payload), format), accept));
	}
	
	/**
	 * Sends a POST request with the specified payload, the specified content
	 * format and the specified accept option and waits for the response.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @param accept the accept option
	 * @return the coap response
	 */
	public CoapResponse post(byte[] payload, int format, int accept) {
		return synchronous(accept(format(Request.newPost().setURI(uri).setPayload(payload), format), accept));
	}
	
	// Asynchronous POST
	
	/**
	 * Sends a POSt request with the specified payload and invokes the specified
	 * handler when a response arrives.
	 *
	 * @param payload the payload
	 * @param handler the handler
	 */
	public void post(String payload, CoapHandler handler) {
		asynchronous(Request.newPost().setURI(uri).setPayload(payload), handler);
	}
	
	/**
	 * Sends a POST request with the specified payload and invokes the specified
	 * handler when a response arrives.
	 *
	 * @param payload the payload
	 * @param handler the handler
	 */
	public void post(byte[] payload, CoapHandler handler) {
		asynchronous(Request.newPost().setURI(uri).setPayload(payload), handler);
	}
	
	/**
	 * Sends a POST request with the specified payload and the specified content
	 * format and invokes the specified handler when a response arrives.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @param handler the handler
	 */
	public void post(String payload, int format, CoapHandler handler) {
		asynchronous(format(Request.newPost().setURI(uri).setPayload(payload), format), handler);
	}
	
	/**
	 * Sends a POST request with the specified payload and the specified content
	 * format and invokes the specified handler when a response arrives.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @param handler the handler
	 */
	public void post(byte[] payload, int format, CoapHandler handler) {
		asynchronous(format(Request.newPost().setURI(uri).setPayload(payload), format), handler);
	}
	
	/**
	 * Sends a POST request with the specified payload, the specified content
	 * format and accept and invokes the specified handler when a response
	 * arrives.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @param accept the accept option
	 * @param handler the handler
	 */
	public void post(String payload, int format, int accept, CoapHandler handler) {
		asynchronous(accept(format(Request.newPost().setURI(uri).setPayload(payload), format), accept), handler);
	}
	
	/**
	 * Sends a POST request with the specified payload, the specified content
	 * format and accept and invokes the specified handler when a response
	 * arrives.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @param accept the accept option
	 * @param handler the handler
	 */
	public void post(byte[] payload, int format, int accept, CoapHandler handler) {
		asynchronous(accept(format(Request.newPost().setURI(uri).setPayload(payload), format), accept), handler);
	}
	
	// Synchronous PUT
	
	/**
	 * Sends a PUT request with the specified payload and waits for the 
	 * response.
	 *
	 * @param payload the payload
	 * @return the coap response
	 */
	public CoapResponse put(String payload) {
		return synchronous(Request.newPut().setURI(uri).setPayload(payload));
	}
	
	/**
	 * Sends a PUT request with the specified payload and waits for the
	 * response.
	 *
	 * @param payload the payload
	 * @return the coap response
	 */
	public CoapResponse put(byte[] payload) {
		return synchronous(Request.newPut().setURI(uri).setPayload(payload));
	}
	
	/**
	 * Sends a PUT request with the specified content format and waits for
	 * the response.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @return the coap response
	 */
	public CoapResponse put(String payload, int format) {
		return synchronous(format(Request.newPut().setURI(uri).setPayload(payload), format));
	}
	
	/**
	 * Sends a PUT request with the specified content format and waits for
	 * the response.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @return the coap response
	 */
	public CoapResponse put(byte[] payload, int format) {
		return synchronous(format(Request.newPut().setURI(uri).setPayload(payload), format));
	}
	
	// Asynchronous PUT
	
	/**
	 * Sends a PUT request with the specified payload and invokes the specified
	 * handler when a response arrives.
	 *
	 * @param payload the payload
	 * @param handler the handler
	 */
	public void put(String payload, CoapHandler handler) {
		asynchronous(Request.newPut().setURI(uri).setPayload(payload), handler);
	}
	
	/**
	 * Sends a PUT request with the specified payload and invokes the specified
	 * handler when a response arrives.
	 *
	 * @param payload the payload
	 * @param handler the handler
	 */
	public void put(byte[] payload, CoapHandler handler) {
		asynchronous(Request.newPut().setURI(uri).setPayload(payload), handler);
	}
	
	/**
	 * Sends a PUT request with the specified payload and the specified content
	 * format and invokes the specified handler when a response arrives.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @param handler the handler
	 */
	public void put(String payload, int format, CoapHandler handler) {
		asynchronous(format(Request.newPut().setURI(uri).setPayload(payload), format), handler);
	}
	
	/**
	 * Sends a PUT request with the specified payload and the specified content
	 * format and invokes the specified handler when a response arrives.
	 *
	 * @param payload the payload
	 * @param format the content format
	 * @param handler the handler
	 */
	public void put(byte[] payload, int format, CoapHandler handler) {
		asynchronous(format(Request.newPut().setURI(uri).setPayload(payload), format), handler);
	}
	
	// Synchronous DELETE
	
	/**
	 * Sends a DELETE request and waits for the response.
	 *
	 * @return the coap response
	 */
	public CoapResponse delete() {
		return synchronous(Request.newDelete().setURI(uri));
	}
	
	/**
	 * Sends a DELETE request and invokes the specified handler when a response
	 * arrives.
	 *
	 * @param handler the handler
	 */
	public void delete(CoapHandler handler) {
		asynchronous(Request.newDelete().setURI(uri), handler);
	}
	
	// Synchronous observer
	
	/**
	 * Sends an observe request and waits until it has been established 
	 * whereupon the specified handler is invoked when a notification arrives.
	 *
	 * @param handler the handler
	 * @return the coap observe relation
	 */
	public CoapObserveRelation observeAndWait(CoapHandler handler) {
		Request request = Request.newGet().setURI(uri).setObserve();
		return observeAndWait(request, handler);
	}
	
	/**
	 * Sends an observe request with the specified accept option and waits until
	 * it has been established whereupon the specified handler is invoked when a
	 * notification arrives.
	 *
	 * @param handler the handler
	 * @param accept the accept option
	 * @return the coap observe relation
	 */
	public CoapObserveRelation observeAndWait(CoapHandler handler, int accept) {
		Request request = Request.newGet().setURI(uri).setObserve();
		request.getOptions().setAccept(accept);
		return observeAndWait(request, handler);
	}
	
	/**
	 * Sends the specified observe request and waits for the response whereupon
	 * the specified handler is invoked when a notification arrives.
	 *
	 * @param request the request
	 * @param handler the handler
	 * @return the coap observe relation
	 */
	private CoapObserveRelation observeAndWait(Request request, CoapHandler handler) {
		CoapObserveRelation relation = new CoapObserveRelation(request);
		request.addMessageObserver(new ObserveMessageObserveImpl(handler, relation));
		CoapResponse response = synchronous(request);
		if (response == null || !response.getDetailed().getOptions().hasObserve())
			relation.setCanceled(true);
		return relation;
	}
	
	// Asynchronous observe
	
	/**
	 * Sends an observe request and invokes the specified handler each time
	 * a notification arrives.
	 *
	 * @param handler the handler
	 * @return the coap observe relation
	 */
	public CoapObserveRelation observe(CoapHandler handler) {
		Request request = Request.newGet().setURI(uri).setObserve();
		return observe(request, handler);
	}
	
	/**
	 * Sends an observe request with the specified accept option and invokes the
	 * specified handler each time a notification arrives.
	 *
	 * @param handler the handler
	 * @param accept the accept option
	 * @return the coap observe relation
	 */
	public CoapObserveRelation observe(CoapHandler handler, int accept) {
		Request request = Request.newGet().setURI(uri).setObserve();
		return observe(accept(request, accept), handler);
	}
	
	/**
	 * Sends the specified observe request and invokes the specified handler
	 * each time a notification arrives.
	 *
	 * @param request the request
	 * @param handler the handler
	 * @return the coap observe relation
	 */
	private CoapObserveRelation observe(Request request, CoapHandler handler) {
		CoapObserveRelation relation = new CoapObserveRelation(request);
		request.addMessageObserver(new ObserveMessageObserveImpl(handler, relation));
		send(request);
		return relation;
	}
	
	// Implementation
	
	/**
	 * Asynchronously sends the specified request and invokes the specified
	 * handler when a response arrives.
	 *
	 * @param request the request
	 * @param handler the handler
	 */
	private void asynchronous(Request request, CoapHandler handler) {
		request.addMessageObserver(new MessageObserverImpl(handler));
		send(request);
	}
	
	/**
	 * Synchronously sends the specified request.
	 *
	 * @param request the request
	 * @return the coap response
	 */
	private CoapResponse synchronous(Request request) {
		try {
			Response response = send(request).waitForResponse(getTimeout());
			if (response == null) return null;
			else return new CoapResponse(response);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Sets the specified content format to the specified request.
	 *
	 * @param request the request
	 * @param contentFormat the content format
	 * @return the request
	 */
	private Request format(Request request, int contentFormat) {
		request.getOptions().setContentFormat(contentFormat);
		return request;
	}
	
	/**
	 * Sets the specified accept option to the specified request.
	 *
	 * @param request the request
	 * @param accept the accept option
	 * @return the request
	 */
	private Request accept(Request request, int accept) {
		request.getOptions().setAccept(accept);
		return request;
	}
	
	/**
	 * Sends the specified request over the endpoint of the client if one is
	 * defined or over the default endpoint otherwise.
	 *
	 * @param request the request
	 * @return the request
	 */
	protected Request send(Request request) {
		if (endpoint != null)
			endpoint.sendRequest(request);
		else request.send();
		return request;
	}
	
	/**
	 * Gets the timeout.
	 *
	 * @return the timeout
	 */
	public long getTimeout() {
		return timeout;
	}
	
	/**
	 * Sets the timeout how long synchronous method calls will wait until they
	 * give up and return anyways. The value 0 is equal to infinity.
	 *
	 * @param timeout the timeout
	 * @return the coap client
	 */
	public CoapClient setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Gets the destination URI of this client.
	 *
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Sets the destination URI of this client.
	 *
	 * @param uri the uri
	 * @return the coap client
	 */
	public CoapClient setUri(String uri) {
		this.uri = uri;
		return this;
	}
	
	/**
	 * Sets a single-threaded executor to this client. All handlers will be
	 * invoked by this executor.
	 *
	 * @return the coap client
	 */
	public CoapClient useExecutor() {
		this.executor = Executors.newSingleThreadExecutor();
		return this;
	}

	/**
	 * Gets the executor of this client.
	 *
	 * @return the executor
	 */
	public Executor getExecutor() {
		if (executor == null)
			synchronized(this) {
			if (executor == null)
				executor = Executors.newSingleThreadExecutor();
		}
		return executor;
	}

	/**
	 * Sets the executor to this client. All handlers will be invoked by this
	 * executor.
	 *
	 * @param executor the executor
	 * @return the coap client
	 */
	public CoapClient setExecutor(Executor executor) {
		this.executor = executor;
		return this;
	}

	/**
	 * Gets the endpoint this client uses.
	 *
	 * @return the endpoint
	 */
	public Endpoint getEndpoint() {
		return endpoint;
	}

	/**
	 * Sets the endpoint this client is supposed to use.
	 *
	 * @param endpoint the endpoint
	 * @return the coap client
	 */
	public CoapClient setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
		return this;
	}
	
	/**
	 * The MessageObserverImpl is called when a response arrives. It wraps the
	 * response into a CoapResponse and lets the executor invoke the handler's
	 * method.
	 */
	private class MessageObserverImpl extends MessageObserverAdapter {

		/** The handler. */
		protected CoapHandler handler;
		
		/**
		 * Constructs a new message observer that calls the specified handler
		 *
		 * @param handler the handler
		 */
		private MessageObserverImpl(CoapHandler handler) {
			this.handler = handler;
		}
		
		/* (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.coap.MessageObserverAdapter#responded(ch.ethz.inf.vs.californium.coap.Response)
		 */
		@Override public void responded(final Response response) {
			succeeded(response != null ? new CoapResponse(response) : null);
		}
		
		/* (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.coap.MessageObserverAdapter#rejected()
		 */
		@Override public void rejected()  { failed(); }
		
		/* (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.coap.MessageObserverAdapter#timeouted()
		 */
		@Override public void timeouted() { failed(); }
		
		/**
		 * Invoked when a response arrives (even if the response code is not
		 * successful, the response still has been successfully transmitted).
		 *
		 * @param response the response
		 */
		protected void succeeded(final CoapResponse response) {
			Executor exe = getExecutor();
			if (exe == null) handler.responded(response);
			else exe.execute(new Runnable() {				
				public void run() {
					try {
						deliver(response);
					} catch (Throwable t) {
						LOGGER.log(Level.WARNING, "Exception while handling response", t);
					}}});
		}
		
		/**
		 * Invokes the handler's method with the specified response. This method
		 * must be invoked by the client's executor if it defines one.
		 *
		 * @param response the response
		 */
		protected void deliver(CoapResponse response) {
			
		}
		
		/**
		 * Invokes the handler's method failed() on the executor.
		 */
		protected void failed() {
			Executor exe = getExecutor();
			if (exe == null) handler.failed();
			else exe.execute(new Runnable() { 
				public void run() { 
					try {
						handler.failed(); 
					} catch (Throwable t) {
						LOGGER.log(Level.WARNING, "Exception while handling failure", t);
					}}});
		}
	}
	
	/**
	 * The ObserveMessageObserveImpl is called whenever a notification of an
	 * observed resource arrives. It wraps the response into a CoapResponse and
	 * lets the executor invoke the handler's method.
	 */
	private class ObserveMessageObserveImpl extends MessageObserverImpl {
		
		/** The observer relation relation. */
		private final CoapObserveRelation relation;
		
		/** The orderer. */
		private final ObserveNotificationOrderer orderer;
		
		/**
		 * Constructs a new message observer with the specified handler and the
		 * specified relation.
		 *
		 * @param handler the handler
		 * @param relation the relation
		 */
		public ObserveMessageObserveImpl(CoapHandler handler, CoapObserveRelation relation) {
			super(handler);
			this.relation = relation;
			this.orderer = new ObserveNotificationOrderer();
		}
		
		/**
		 * Checks if the specified response truly is a new notification and if,
		 * invokes the handler's method or drops the notification otherwise.
		 */
		@Override protected void deliver(CoapResponse response) {
			synchronized (orderer) {
				if (orderer.isNew(response.getDetailed())) {
					relation.setCurrent(response);
					handler.responded(response);
				} else {
					System.out.println("drop: "+response.getDetailed());
					// drop this notification
					return;
				}
			}
		}
		
		/**
		 * Marks the relation as canceled and invokes the the handler's failed()
		 * method.
		 */
		@Override protected void failed() {
			relation.setCanceled(true);
			super.failed();
		}
	}
	
	/**
	 * The Builder can be used to build a CoapClient if the URI's pieces are
	 * available in separate strings. This is in particular useful to add 
	 * mutliple queries to the URI.
	 */
	public static class Builder {
		
		/** The scheme, host and port. */
		String scheme, host, port;
		
		/** The path and the query. */
		String[] path, query;
		
		/**
		 * Instantiates a new builder.
		 *
		 * @param host the host
		 * @param port the port
		 */
		public Builder(String host, int port) {
			this.host = host;
			this.port = Integer.toString(port);
		}
		
		/**
		 * Sets the specified scheme.
		 *
		 * @param scheme the scheme
		 * @return the builder
		 */
		public Builder scheme(String scheme) { this.scheme = scheme; return this; }
		
		/**
		 * Sets the specified host.
		 *
		 * @param host the host
		 * @return the builder
		 */
		public Builder host(String host) { this.host = host; return this; }
		
		/**
		 * Sets the specified port.
		 *
		 * @param port the port
		 * @return the builder
		 */
		public Builder port(String port) { this.port = port; return this; }
		
		/**
		 * Sets the specified port.
		 *
		 * @param port the port
		 * @return the builder
		 */
		public Builder port(int port) { this.port = Integer.toString(port); return this; }
		
		/**
		 * Sets the specified resource path.
		 *
		 * @param path the path
		 * @return the builder
		 */
		public Builder path(String... path) { this.path = path; return this; }
		
		/**
		 * Sets the specified query.
		 *
		 * @param query the query
		 * @return the builder
		 */
		public Builder query(String... query) { this.query = query; return this; }
		
		/**
		 * Creates the CoapClient
		 *
		 * @return the client
		 */
		public CoapClient create() {
			StringBuilder builder = new StringBuilder();
			if (scheme != null)	
				builder.append(scheme).append("://");
			builder.append(host).append(":").append(port);
			for (String element:path)
				builder.append("/").append(element);
			if (query.length > 0)
				builder.append("?");
			for (int i=0;i<query.length;i++) {
				builder.append(query[i]);
				if (i < query.length-1)
					builder.append("&");
			}
			return new CoapClient(builder.toString());
		}
	}
}

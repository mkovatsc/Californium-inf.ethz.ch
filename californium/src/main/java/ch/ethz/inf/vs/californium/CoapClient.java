package ch.ethz.inf.vs.californium;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.MessageObserverAdapter;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;

public class CoapClient {

	private static final Logger LOGGER = CalifonriumLogger.getLogger(CoapClient.class);
	
	private long timeout = NetworkConfig.getStandard()
		.getLong(NetworkConfigDefaults.COAP_CLIENT_DEFAULT_TIMEOUT);
	
	private String uri;
	
	private Executor executor;
	
	private Endpoint endpoint;
	
	public CoapClient() {
		this("");
	}
	
	public CoapClient(String uri) {
		this.uri = uri;
	}
	
	public CoapClient(String scheme, String host, int port, String... path) {
		StringBuilder builder = new StringBuilder()
			.append(scheme).append("://").append(host).append(":").append(port);
		for (String element:path)
			builder.append("/").append(element);
		this.uri = builder.toString();
	}
 	
	public CoapResponse get() {
		return synchronous(Request.newGet().setURI(uri));
	}
	
	public void get(CoapHandler handler) {
		asynchronous(Request.newGet().setURI(uri), handler);
	}
	
	public CoapResponse post(String payload) {
		return synchronous(Request.newPost().setURI(uri).setPayload(payload));
	}
	
	public CoapResponse post(byte[] payload) {
		return synchronous(Request.newPost().setURI(uri).setPayload(payload));
	}
	
	public void post(String payload, CoapHandler handler) {
		asynchronous(Request.newPost().setURI(uri).setPayload(payload), handler);
	}
	
	public void post(byte[] payload, CoapHandler handler) {
		asynchronous(Request.newPost().setURI(uri).setPayload(payload), handler);
	}
	
	public CoapResponse put(String payload) {
		return synchronous(Request.newPut().setURI(uri).setPayload(payload));
	}
	
	public CoapResponse put(byte[] payload) {
		return synchronous(Request.newPut().setURI(uri).setPayload(payload));
	}
	
	public void put(String payload, CoapHandler handler) {
		asynchronous(Request.newPut().setURI(uri).setPayload(payload), handler);
	}
	
	public void put(byte[] payload, CoapHandler handler) {
		asynchronous(Request.newPut().setURI(uri).setPayload(payload), handler);
	}
	
	public CoapResponse delete() {
		return synchronous(Request.newDelete().setURI(uri));
	}
	
	public void delete(CoapHandler handler) {
		asynchronous(Request.newDelete().setURI(uri), handler);
	}
	
	public CoapObserveRelation observe(CoapHandler handler) {
		Request request = Request.newGet().setURI(uri).setObserve();
		CoapObserveRelation relation = new CoapObserveRelation(request);
		request.addMessageObserver(new ObserveMessageObserveImpl(handler, relation));
		send(request);
		return relation;
	}
	
	public CoapObserveRelation observeAndWait(CoapHandler handler) {
		Request request = Request.newGet().setURI(uri).setObserve();
		CoapObserveRelation relation = new CoapObserveRelation(request);
		request.addMessageObserver(new ObserveMessageObserveImpl(handler, relation));
		CoapResponse response = synchronous(request);
		if (response == null || !response.getResponse().getOptions().hasObserve())
			relation.setCanceled(true);
		return relation;
	}
	
	private void asynchronous(Request request, CoapHandler handler) {
		request.addMessageObserver(new MessageObserverImpl(handler));
		send(request);
	}
	
	private CoapResponse synchronous(Request request) {
		try {
			Response response = send(request).waitForResponse(getTimeout());
			if (response == null) return null;
			else return new CoapResponse(response);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Request send(Request request) {
		if (endpoint != null)
			endpoint.sendRequest(request);
		else request.send();
		return request;
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public CoapClient setTimeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	public String getUri() {
		return uri;
	}

	public CoapClient setUri(String uri) {
		this.uri = uri;
		return this;
	}
	
	public CoapClient useExecutor() {
		this.executor = Executors.newSingleThreadExecutor();
		return this;
	}

	public Executor getExecutor() {
		if (executor == null)
			synchronized(this) {
			if (executor == null)
				executor = Executors.newSingleThreadExecutor();
		}
		return executor;
	}

	public CoapClient setExecutor(Executor executor) {
		this.executor = executor;
		return this;
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public CoapClient setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
		return this;
	}
	
	private class MessageObserverImpl extends MessageObserverAdapter {

		private CoapHandler handler;
		
		private MessageObserverImpl(CoapHandler handler) {
			this.handler = handler;
		}
		
		@Override public void responded(final Response response) {
			succeeded(response != null ? new CoapResponse(response) : null);
		}
		
		@Override public void rejected()  { failed(); }
		@Override public void timeouted() { failed(); }
//		@Override public void canceled()  { failed(); }
		
		protected void succeeded(final CoapResponse response) {
			Executor exe = getExecutor();
			if (exe == null) handler.responded(response);
			else exe.execute(new Runnable() {				
				public void run() {
					try {
						handler.responded(response);
					} catch (Throwable t) {
						LOGGER.log(Level.WARNING, "Exception while handling response", t);
					}}});
		}
		
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
	
	private class ObserveMessageObserveImpl extends MessageObserverImpl {
		
		private CoapObserveRelation relation;
		
		public ObserveMessageObserveImpl(CoapHandler handler, CoapObserveRelation relation) {
			super(handler);
			this.relation = relation;
		}
		
		@Override protected void succeeded(CoapResponse response) {
			relation.setCurrent(response);
			super.succeeded(response); // TODO: ordering
		}
		
		@Override protected void failed() {
			relation.setCanceled(true);
			super.failed();
		}
		
	}
	
	public static class Builder {
		
		String scheme, host, port;
		String[] path, query;
		
		public Builder(String host, int port) {
			this.host = host;
			this.port = Integer.toString(port);
		}
		public Builder scheme(String scheme) { this.scheme = scheme; return this; }
		public Builder host(String host) { this.host = host; return this; }
		public Builder port(String port) { this.port = port; return this; }
		public Builder port(int port) { this.port = Integer.toString(port); return this; }
		public Builder path(String... path) { this.path = path; return this; }
		public Builder query(String... query) { this.query = query; return this; }
		
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

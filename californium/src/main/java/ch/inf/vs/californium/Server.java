package ch.inf.vs.californium;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.network.Endpoint;

/**
 * A server contains a resource structure and can listen to one or more
 * endpoints to handle requests. Resources of a server can send requests over
 * any endpoint the server is associated to.
 **/
public class Server implements ServerInterface {

	private final static Logger LOGGER = Logger.getLogger(Server.class.getName());
	
	private List<Endpoint> endpoints;
	
	private Resource root;
	
	private Executor stackExecutor;
	
	public Server() {
		endpoints = new ArrayList<Endpoint>();
		stackExecutor = Executors.newCachedThreadPool();
	}
	
	public Server(int... ports) {
		this();
		for (int port:ports)
			registerEndpoint(port);
	}
	
	public void start() {
		for (Endpoint ep:endpoints) {
			try {
				ep.start();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Exception in thread \"" + Thread.currentThread().getName() + "\"", e);
			}
		}
	}
	
	public void stop() {
		for (Endpoint ep:endpoints)
			ep.stop();
	}
	
	public void destroy() {
		for (Endpoint ep:endpoints)
			ep.destroy();
	}
	
	public void registerEndpoint(/*InetAddress, */ int port) {
		Endpoint endpoint = new Endpoint(port);
		addEndpoint(endpoint);
	}
	
	public void addEndpoint(Endpoint endpoint) {
		// TODO: make the endpoint deliver Requests to this server
		endpoint.setExecutor(stackExecutor);
		endpoints.add(endpoint);
	}
	
	public static void main(String[] args) {
		System.out.println("Starting server");
		new Server(60000).start();
	}
}

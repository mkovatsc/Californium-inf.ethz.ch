package ch.inf.vs.californium;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.resources.AbstractResource;
import ch.inf.vs.californium.resources.Resource;

/**
 * A server contains a resource structure and can listen to one or more
 * endpoints to handle requests. Resources of a server can send requests over
 * any endpoint the server is associated to.
 **/
public class Server implements ServerInterface {

	public static boolean log = false;
	
	private final static Logger LOGGER = Logger.getLogger(Server.class.getName());

	private final Resource root;
	
	private final List<Endpoint> endpoints;
	
	private ScheduledExecutorService stackExecutor;
	private MessageDeliverer deliverer;
	
	public Server() {
		this.root = new AbstractResource("") { };
		this.endpoints = new ArrayList<Endpoint>();
		this.stackExecutor = Executors.newScheduledThreadPool(4);
		this.deliverer = new DefaultMessageDeliverer(root);
	}
	
	public Server(int... ports) {
		this();
		for (int port:ports)
			registerEndpoint(port);
	}
	
	public void start() {
		LOGGER.info("Start server");
		for (Endpoint ep:endpoints) {
			try {
				ep.start();
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.log(Level.WARNING, "Exception in thread \"" + Thread.currentThread().getName() + "\"", e);
			}
		}
	}
	
	public void stop() {
		LOGGER.info("Stop server");
		for (Endpoint ep:endpoints)
			ep.stop();
		stackExecutor.shutdown();
	}
	
	public void destroy() {
		LOGGER.info("Destroy server");
		for (Endpoint ep:endpoints)
			ep.destroy();
	}
	
	public void registerEndpoint(/*InetAddress, */ int port) {
		Endpoint endpoint = new Endpoint(port);
		addEndpoint(endpoint);
	}
	
	public void setMessageDeliverer(MessageDeliverer deliverer) {
		this.deliverer = deliverer;
		for (Endpoint endpoint:endpoints)
			endpoint.setMessageDeliverer(deliverer);
	}
	
	public void addEndpoint(Endpoint endpoint) {
		endpoint.setMessageDeliverer(deliverer);
		endpoint.setExecutor(stackExecutor);
		endpoints.add(endpoint);
	}
	
	public static void initializeLogger() {
		LogManager.getLogManager().reset();
		Logger logger = Logger.getLogger("");
		logger.addHandler(new StreamHandler(System.out, new Formatter() {
		    @Override
		    public synchronized String format(LogRecord record) {
		    	String stackTrace = "";
		    	Throwable throwable = record.getThrown();
		    	if (throwable != null) {
		    		StringWriter sw = new StringWriter();
		    		throwable.printStackTrace(new PrintWriter(sw));
		    		stackTrace = sw.toString();
		    	}
		    	
		    	int lineNo;
		    	StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		    	if (throwable != null && stack.length > 7)
		    		lineNo = stack[7].getLineNumber();
		    	else if (stack.length > 8)
		    		lineNo = stack[8].getLineNumber();
		    	else lineNo = -1;
		    	
		        return String.format("%2d", record.getThreadID()) + " " + record.getLevel()+": "
		        		+ record.getMessage()
		        		+ " - ("+record.getSourceClassName()+".java:"+lineNo+") "
		                + record.getSourceMethodName()+"()"
		                + " in " + Thread.currentThread().getName()+"\n"
		                + stackTrace;
		    }
		}) {
			@Override
			public synchronized void publish(LogRecord record) {
				super.publish(record);
				super.flush();
			}
			}
		);
	}
	
	public Server add(Resource resource) {
		root.add(resource);
		return this;
	}
	
	public boolean remove(Resource resource) {
		return root.remove(resource);
	}

	public Resource getRoot() {
		return root;
	}
}

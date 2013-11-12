package ch.ethz.inf.vs.californium.examples;

import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.examples.resources.FibonacciResource;
import ch.ethz.inf.vs.californium.examples.resources.HelloWorldResource;
import ch.ethz.inf.vs.californium.examples.resources.ImageResource;
import ch.ethz.inf.vs.californium.examples.resources.LargeResource;
import ch.ethz.inf.vs.californium.examples.resources.MirrorResource;
import ch.ethz.inf.vs.californium.examples.resources.StorageResource;
import ch.ethz.inf.vs.californium.server.Server;

/**
 * This is an example server that contains a few resources for demonstration.
 * 
 * @author Martin Lanter
 */
public class ExampleServer {
	
	public static void main(String[] args) throws Exception {
		Server server = new Server();
		server.setExecutor(Executors.newScheduledThreadPool(4));
		
		server.add(new HelloWorldResource("hello"));
		server.add(new FibonacciResource("fibonacci"));
		server.add(new StorageResource("storage"));
		server.add(new ImageResource("image"));
		server.add(new MirrorResource("mirror"));
		server.add(new LargeResource("large"));
		
		server.start();
	}
	
	/*
	 *  Sends a GET request to itself
	 */
	public static void selfTest() {
		try {
			Request request = Request.newGet();
			request.setURI("localhost:5683/hello");
			request.send();
			Response response = request.waitForResponse(1000);
			System.out.println("received "+response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

package ch.ethz.inf.vs.californium.examples.concurrent;

import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.examples.LargeResource;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.ConcurrentResourceBase;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * Creates an example server with resources that have different multi-threading
 * policies. The three resources on top with the name "server-thread" are normal
 * resources that do not define their own executor. Therefore, they all use
 * their parent's executor which ultimately is the server's. The resource
 * "single-threaded" defines its own executor with one thread. Therefore, all
 * requests to that resource will be executed by the same one thread. Its child
 * again is a single-threaded resource that uses its very own single-threaded
 * executor. The resource "four-threaded" uses an executor with four threads.
 * Request can be (concurrently) processed by any of them. The resource has a
 * child and a grand-child that both are normal resources and therefore also use
 * the executor with four threads. Finally, the resource "mt-large" reuses a
 * normal resource as implementation but uses a new executor to process the
 * requests. For the client, the resource behaves exactly like there were no
 * executor.
 * <hr>
 * <blockquote>
 * 
 * <pre>
 * Root
 *  |
 *  |-- server-thread: pool-1-thread-[1-4]
 *  |    `-- server-thread: pool-1-thread-[1-4]
 *  |         `-- server-thread: pool-1-thread-[1-4]
 *  |
 *  |-- single-threaded: pool-2-thread-1
 *  |    `-- single-threaded: pool-3-thread-1
 *  |
 *  |-- four-threaded: pool-4-thread-[1-4]
 *  |    `-- same-as-parent: pool-4-thread-[1-4]
 *  |         `-- same-as-parent: pool-4-thread-[1-4]
 *  |
 *  |-- mt-large: pool-1-thread-[1-4]
 * </pre>
 * 
 * </blockquote>
 * <hr>
 **/
public class ConcurrentExampleServer {

	public static void main(String[] args) {
		System.out.println("Starting Concurrent Example Server");
		
		Server server = new Server();
		server.add(new NoThreadResource("server-thread")
					.add(new NoThreadResource("server-thread")
						.add(new NoThreadResource("server-thread"))));
		server.add(new ConcurrentResource("single-threaded", 1)
					.add(new ConcurrentResource("single-threaded", 1)));
		server.add(new ConcurrentResource("four-threaded", 4)
					.add(new NoThreadResource("same-as-parent")
						.add(new NoThreadResource("same-as-parent"))));
		
		// Use an already created resource without executor as implementation
		// for a resource that has its own executor.
		server.add(ConcurrentResourceBase.createConcurrentResourceBase("mt-large", 4, new LargeResource()));
		
		// start the server
		server.start();
	}
	
	/**
	 * A resource that uses the executor of its parent/ancestor if defined or
	 * the server's executor otherwise.
	 */
	private static class NoThreadResource extends ResourceBase {
		
		public NoThreadResource(String name) {
			super(name);
		}
		
		@Override
		public void processGET(Exchange exchange) {
			Response response = new Response(ResponseCode.CONTENT);
			response.setPayload("You have been served by my parent's thread:"+Thread.currentThread().getName());
			respond(exchange, response);
		}
	}
	
	/**
	 * A resource with its own executor. Only threads of that executor will
	 * process GET requests.
	 */
	private static class ConcurrentResource extends ConcurrentResourceBase {
		
		public ConcurrentResource(String name) {
			super(name);
		}
		
		public ConcurrentResource(String name, int threads) {
			super(name, threads);
		}
		
		@Override
		public void processGET(Exchange exchange) {
			Response response = new Response(ResponseCode.CONTENT);
			response.setPayload("You have been served by one of my "+getThreadCount()+" threads: "+Thread.currentThread().getName());
			respond(exchange, response);
		}
		
		/**
		 * This method must only be executed by one thread at the time.
		 * Therefore, we make it synchronized. This does not affect processGET()
		 * which can be executed concurrently.
		 */
		@Override
		public void processPOST(Exchange exchange) {
			exchange.accept();
			synchronized (this) {
				try { Thread.sleep(5000); // waste some time
				} catch (Exception e) { e.printStackTrace(); }
				Response response = new Response(ResponseCode.CONTENT);
				response.setPayload("Your POST request has been processed by one of my "+getThreadCount()+" threads: "+Thread.currentThread().getName());
				respond(exchange, response);
			}
		}
	}
}

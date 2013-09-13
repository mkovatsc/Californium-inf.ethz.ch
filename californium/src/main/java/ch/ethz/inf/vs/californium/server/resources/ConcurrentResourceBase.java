package ch.ethz.inf.vs.californium.server.resources;

import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.network.Exchange;

/**
 * TODO
 */
public class ConcurrentResourceBase extends ResourceBase {
	
	public static int SINGLE_THREADED = 1;
	
	/** The number of threads. */
	private int threads;

	/**
	 * Constructs a new resource that uses an executor with as many threads as
	 * there are processors available.
	 * 
	 * @param name the name
	 */
	public ConcurrentResourceBase(String name) {
		super(name);
		this.threads = getAvailableProcessors();
		setExecutor(Executors.newFixedThreadPool(threads));
	}
	
	/**
	 * Constructs a new resource that uses the specified amount of threads to
	 * process requests.
	 * 
	 * @param name the name
	 * @param threads the number of threads
	 */
	public ConcurrentResourceBase(String name, int threads) {
		super(name);
		this.threads = threads;
		setExecutor(Executors.newFixedThreadPool(threads));
	}
	
	/**
	 * Gets the number of available processors.
	 *
	 * @return the maximum number of processors available to the virtual
     *          machine; never smaller than one
	 */
	protected int getAvailableProcessors() {
		return Runtime.getRuntime().availableProcessors();
	}
	
	/**
	 * Gets the number of threads
	 *
	 * @return the thread count
	 */
	public int getThreadCount() {
		return threads;
	}

	public static ConcurrentResourceBase createResourceThreadBase(String name, int threads, final RequestProcessor impl) {
		return new ConcurrentResourceBase(name, threads) {
			protected void processRequestImpl(Exchange exchange) {
				impl.processRequest(exchange);
			}
		};
	}
}

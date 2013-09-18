package ch.ethz.inf.vs.californium.server.resources;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.network.Exchange;

/**
 * TODO.
 */
public class ConcurrentResourceBase extends ResourceBase {
	
	/** The constant 1 for single threaded executors */
	public static int SINGLE_THREADED = 1;
	
	/** The number of threads. */
	private int threads;
	
	/** The executor of this resource or null */
	private Executor executor;

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
	 * Sets the specified executor to the resource.
	 * 
	 * @param executor the executor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.server.resources.ResourceBase#getExecutor()
	 */
	@Override
	public Executor getExecutor() {
		if (executor != null) return executor;
		else return super.getExecutor();
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

	/**
	 * Wraps the specified implementation in a ConcurrentResourceBase that uses
	 * the specified number of threads to process requests. This method can be
	 * used to reuse a given resource but with an own thread-pool.
	 * 
	 * @param threads the number of threads
	 * @param impl the implementation
	 * @return the wrapping resource
	 */
//	public static ConcurrentResourceBase createConcurrentResourceBase(String name, int threads, final RequestProcessor impl) {
	public static ConcurrentResourceBase createConcurrentResourceBase(int threads, final Resource impl) {
		return new ConcurrentResourceBase(impl.getName(), threads) {
			@Override
			protected void processRequestImpl(Exchange exchange) {
				impl.processRequest(exchange);
			}
		};
	}
}

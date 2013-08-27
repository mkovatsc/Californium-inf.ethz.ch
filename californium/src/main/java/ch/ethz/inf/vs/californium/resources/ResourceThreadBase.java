package ch.ethz.inf.vs.californium.resources;

import java.util.concurrent.Executors;

/**
 * TODO
 */
public class ResourceThreadBase extends ResourceBase {
	
	public static int SINGLE_THREADED = 1;

	/**
	 * Constructs a new resource that uses an executor with as many threads as
	 * there are processors available.
	 * 
	 * @param name the name
	 */
	public ResourceThreadBase(String name) {
		super(name);
		setExecutor(Executors.newFixedThreadPool(getAvailableProcessors()));
	}
	
	/**
	 * Constructs a new resource that uses the specified amount of threads to
	 * process requests.
	 * 
	 * @param name the name
	 * @param threads the number of threads
	 */
	public ResourceThreadBase(String name, int threads) {
		super(name);
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
}

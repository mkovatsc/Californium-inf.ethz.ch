/**
 * 
 */

package ch.ethz.inf.vs.californium.endpoint.resources;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

/**
 * @author Francesco Corazza
 * 
 */
public class CacheResource extends LocalResource {

	public CacheResource() {
		super("cache");
	}

	/**
	 * Send cached response.
	 * 
	 * @param request
	 *            the request
	 * @return the cached response
	 */
	public Response getCachedResponse(Request request) {
		return null;
		// TODO Auto-generated method stub
	}

	/**
	 * Checks if is cached.
	 * 
	 * @param request
	 *            the request
	 * @return true, if is cached
	 */
	public boolean isCached(Request request) {
		// TODO Auto-generated method stub
		return false;
	}

}

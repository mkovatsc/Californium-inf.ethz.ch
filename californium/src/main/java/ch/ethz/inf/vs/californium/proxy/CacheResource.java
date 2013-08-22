package ch.ethz.inf.vs.californium.proxy;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

import com.google.common.cache.CacheStats;

public interface CacheResource {

	/**
	 * 
	 */
	public void cacheResponse(Request request, Response response);

	public CacheStats getCacheStats();

	/**
	 * Gets cached response.
	 * 
	 * @param request
	 *            the request
	 * @return the cached response or null in case it is not present
	 */
	public Response getResponse(Request request);

	public void invalidateRequest(Request request);
}

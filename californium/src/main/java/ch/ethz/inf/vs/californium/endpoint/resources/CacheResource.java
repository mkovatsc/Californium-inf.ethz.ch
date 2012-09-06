
package ch.ethz.inf.vs.californium.endpoint.resources;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

public interface CacheResource {

	/**
	 * 
	 */
	public void cacheResponse(Response response);

	/**
	 * Gets cached response.
	 * 
	 * @param request
	 *            the request
	 * @return the cached response or null in case it is not present
	 */
	public Response getResponse(Request request);

	public void invalidateResponse(Response response);

}

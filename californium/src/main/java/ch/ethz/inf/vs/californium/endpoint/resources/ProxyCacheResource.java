/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/

package ch.ethz.inf.vs.californium.endpoint.resources;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Ints;

/**
 * Resource to handle the caching in the proxy.
 * 
 * @author Francesco Corazza
 * 
 */
public class ProxyCacheResource extends LocalResource implements CacheResource {

	/**
	 * Time after an entry is removed. Since it is not possible to set higher
	 * values for the single instances, this constant is the upper bound for the
	 * expiration of the responses. The lifetime lower values will be handled
	 * with the max-age option.
	 */
	private static final int CACHE_RESPONSE_MAX_AGE = 60 * 60 * 24;

	/**
	 * Max size for the cache.
	 */
	private static final long CACHE_SIZE = 10000;

	/**
	 * Default value in seconds if max-age option is not set.
	 */
	private static final int DEFAULT_MAX_AGE = 60;

	/**
	 * The cache.
	 */
	private final LoadingCache<CachedRequest, Response> responseCache;

	/**
	 * Instantiates a new proxy cache resource.
	 */
	public ProxyCacheResource() {
		super("cache");

		// add the sub-resource for debugging purposes
		add(new DebugResource("debug"));

		// builds a new cache that:
		// - has a limited size
		// - removes entries after a DEFAULT_AGE seconds after a write
		// - record statistics
		this.responseCache = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).recordStats().expireAfterWrite(CACHE_RESPONSE_MAX_AGE, TimeUnit.SECONDS).build(new CacheLoader<CachedRequest, Response>() {
			@Override
			public Response load(CachedRequest request) throws NullPointerException {
				Response response = request.getResponse();

				if (response == null) {
					throw new NullPointerException();
				}

				return response;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ch.ethz.inf.vs.californium.endpoint.resources.CacheResource#cacheResponse
	 * (ch.ethz.inf.vs.californium.coap.Response)
	 */
	@Override
	public void cacheResponse(Response response) {
		int code = response.getCode();

		// only the response with success codes should be cached
		if (CodeRegistry.isSuccess(code)) {
			// get the request
			Request request = response.getRequest();
			CachedRequest cachedRequest = CachedRequest.fromRequest(request);

			if (code == CodeRegistry.RESP_CREATED || code == CodeRegistry.RESP_DELETED || code == CodeRegistry.RESP_CHANGED) {
				// the stored response should be invalidated if the response has
				// codes: 2.01, 2.02, 2.04.
				invalidateResponse(response);
			} else if (code == CodeRegistry.RESP_VALID) {
				// increase the max-age value according to the new response
				Option maxAgeOption = response.getFirstOption(OptionNumberRegistry.MAX_AGE);
				if (maxAgeOption != null) {
					// get the cached response
					Response cachedResponse = this.responseCache.getUnchecked(cachedRequest);

					// calculate the new parameters
					long newCurrentTime = response.getTimestamp();
					int newMaxAge = maxAgeOption.getIntValue();

					// set the new parameters
					cachedResponse.getFirstOption(OptionNumberRegistry.MAX_AGE).setIntValue(newMaxAge);
					cachedResponse.setTimestamp(newCurrentTime);

					LOG.finer("Updated cached response");
				} else {
					LOG.warning("No max-age option set in response: " + response);
				}
			} else if (code == CodeRegistry.RESP_CONTENT) {
				// set max-age if not set
				Option maxAgeOption = response.getFirstOption(OptionNumberRegistry.MAX_AGE);
				if (maxAgeOption == null) {
					response.setMaxAge(DEFAULT_MAX_AGE);
				}

				// cache the request

				try {
					// Caches loaded by a CacheLoader will call
					// CacheLoader.load(K) to load new values into the cache
					// when used the get method.
					this.responseCache.get(cachedRequest);
				} catch (Exception e) {
					// swallow
				}

				LOG.finer("Cached response");
			} else {
				// this code should not be reached
				LOG.severe("Code not recognized: " + code);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ch.ethz.inf.vs.californium.endpoint.resources.CacheResource#getResponse
	 * (ch.ethz.inf.vs.californium.coap.Request)
	 */
	@Override
	public Response getResponse(Request request) {
		// create the wrapper for the request
		CachedRequest cachedRequest = CachedRequest.fromRequest(request);

		// get the response from the cache
		Response response = null;
		try {
			response = this.responseCache.getIfPresent(cachedRequest);
		} catch (Exception e) {
			// swallow
		}

		// if the response is not null, manage the cached response
		if (response != null) {
			LOG.finer("Cache hit");

			// check if it is expired
			if (isExpired(response)) {
				LOG.finer("Expired response");

				response = validate(cachedRequest);

				if (response != null) {
					LOG.finer("Validation successful");
				}

			} else {

				// if the response can be used, then update its max-age to
				// consider the aging of the response while in the cache
				Option maxAgeOption = response.getFirstOption(OptionNumberRegistry.MAX_AGE);
				int oldMaxAge = DEFAULT_MAX_AGE;
				if (maxAgeOption != null) {
					oldMaxAge = maxAgeOption.getIntValue();
				}

				// calculate the time that the response has spent in the cache
				long currentTime = System.nanoTime();
				double secondsInCache = TimeUnit.NANOSECONDS.toSeconds(currentTime - response.getTimestamp());
				int cacheTime = Ints.checkedCast(Math.round(secondsInCache));

				// set the remaining time as max-age and the current time as the
				// response timestamp
				int maxAge = oldMaxAge - cacheTime;
				response.setMaxAge(maxAge);
				response.setTimestamp(currentTime);
			}
		}

		return response;
	}

	/**
	 * Method to get the stats about the cache.
	 * 
	 * @return
	 */
	public String getStats() {
		StringBuilder stringBuilder = new StringBuilder();
		CacheStats cacheStats = this.responseCache.stats();

		stringBuilder.append("Ratio of hits to requests: " + cacheStats.hitRate());
		stringBuilder.append("\n");
		stringBuilder.append("Average time spent loading new values (nanoseconds): " + cacheStats.averageLoadPenalty());
		stringBuilder.append("\n");
		stringBuilder.append("Number of cache evictions: " + cacheStats.evictionCount());
		stringBuilder.append("\n");

		return stringBuilder.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.endpoint.resources.CacheResource#
	 * invalidateResponse(ch.ethz.inf.vs.californium.coap.Response)
	 */
	@Override
	public void invalidateResponse(Response response) {
		Request request = response.getRequest();
		this.responseCache.invalidate(CachedRequest.fromRequest(request));
		LOG.finer("Invalidated response");
	}

	/**
	 * Method that checks if the lifetime allowed for the response if expired.
	 * The result is calculated with the initial timestamp (when the response
	 * has been received) and the max-age option compared against the current
	 * timestamp. If the max-age option is not specificated, it will be assumed
	 * the default (60 seconds).
	 * 
	 * @param response
	 *            the response
	 * @return true, if is expired
	 */
	private boolean isExpired(Response response) {
		// get the timestamps
		long currentTime = System.nanoTime();
		long arriveTime = response.getTimestamp();

		// retrieve the max-age
		long maxAge = DEFAULT_MAX_AGE;
		Option maxAgeOption = response.getFirstOption(OptionNumberRegistry.MAX_AGE);
		if (maxAgeOption != null && maxAgeOption.getIntValue() > 0) {
			maxAge = maxAgeOption.getIntValue();
		}

		// calculate the timestamp of expiration for the current response
		// it is needed the translation from seconds to nano seconds
		maxAge = TimeUnit.SECONDS.toNanos(maxAge);
		long expirationTime = arriveTime + maxAge;
		// long expirationTime = getExpirationTime(response);

		if (expirationTime < currentTime) {
			// if the response is expired it should be removed from the cache
			invalidateResponse(response);
			return true;
		}

		return false;
	}

	private Response validate(CachedRequest cachedRequest) {
		// TODO
		return null;
	}

	/**
	 * Nested class to store a request as a key in the cache. It is needed to
	 * normalize the variable fields of the normal requests.
	 * 
	 * @author Francesco Corazza
	 */
	private static final class CachedRequest extends Request {

		/**
		 * From request.
		 * 
		 * @param request
		 *            the request
		 * @return the cached request
		 */
		private static CachedRequest fromRequest(Request request) {
			// set only the fields that cannot change between two same requests
			CachedRequest cachedRequest = new CachedRequest(request.getCode(), request.getType() == messageType.CON);
			cachedRequest.setPayload(request.getPayload());
			cachedRequest.setOptions(ImmutableList.copyOf(request.getOptions()));
			cachedRequest.setResponse(request.getResponse());

			// normalize the options: keep only the ones resource specific
			for (Option option : cachedRequest.getOptions()) {
				int optionNumber = option.getOptionNumber();

				// remove the unneeded options
				if (optionNumber == OptionNumberRegistry.TOKEN || optionNumber == OptionNumberRegistry.MAX_AGE || optionNumber == OptionNumberRegistry.OBSERVE || optionNumber == OptionNumberRegistry.BLOCK1 || optionNumber == OptionNumberRegistry.BLOCK2 || optionNumber == OptionNumberRegistry.MAX_AGE || optionNumber == OptionNumberRegistry.ETAG) {
					cachedRequest.removeOptions(optionNumber);
				}
			}

			return cachedRequest;
		}

		/**
		 * Instantiates a new cached request.
		 * 
		 * @param method
		 *            the method
		 * @param confirmable
		 *            the confirmable
		 */
		public CachedRequest(int method, boolean confirmable) {
			super(method, confirmable);
		}

		/*
		 * (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.coap.Message#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			CachedRequest other = (CachedRequest) obj;
			if (getCode() != other.getCode()) {
				return false;
			}
			if (getOptions() == null) {
				if (other.getOptions() != null) {
					return false;
				}
			} else if (!getOptions().equals(other.getOptions())) {
				return false;
			}
			if (!Arrays.equals(getPayload(), other.getPayload())) {
				return false;
			}
			if (getType() != other.getType()) {
				return false;
			}
			return true;
		}

		/*
		 * (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.coap.Message#hashCode()
		 */
		@Override
		public int hashCode() {
			HashFunction hashFunction = Hashing.md5();
			HashCode hashCode = hashFunction.newHasher().putInt(getCode()).putBytes(getPayload()).putInt(getType().ordinal()).putInt(getOptions().hashCode()).hash();

			return hashCode.asInt();
		}
	}

	/**
	 * The Class DebugResource.
	 * 
	 * @author Francesco Corazza
	 */
	private class DebugResource extends LocalResource {

		/**
		 * Instantiates a new debug resource.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public DebugResource(String resourceIdentifier) {
			super(resourceIdentifier);
		}

		@Override
		public void performDELETE(DELETERequest request) {
			ProxyCacheResource.this.responseCache.invalidateAll();
			request.respond(CodeRegistry.RESP_DELETED);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * ch.ethz.inf.vs.californium.endpoint.resources.LocalResource#performGET
		 * (ch.ethz.inf.vs.californium.coap.GETRequest)
		 */
		@Override
		public void performGET(GETRequest request) {
			String payload = getStats();
			request.respond(CodeRegistry.RESP_CONTENT, payload, MediaTypeRegistry.TEXT_PLAIN);
		}
	}
}

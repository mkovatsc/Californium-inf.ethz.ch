/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

import com.google.common.cache.CacheStats;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.io.Files;

/**
 * Resource that encapsulate the proxy statistics.
 * 
 * @author Francesco Corazza
 * 
 */
public class StatsResource extends LocalResource {
	private final static int PERIOD_SECONDS = 60;
	private final Table<String, String, StatHelper> statsTable = HashBasedTable.create();

	private static String CACHE_LOG_NAME = "_cache_log.log";

	/**
	 * Instantiates a new stats resource.
	 * 
	 * @param cacheResource
	 */
	public StatsResource(CacheResource cacheResource) {
		super("stats");
		setTitle("Keeps track of the requests served by the proxy.");

		// add the sub-resource to show stats
		add(new CacheStatResource("cache", cacheResource));
		add(new ProxyStatResource("proxy"));
	}

	public void updateStatistics(Request request, boolean cachedResponse) {
		URI proxyUri = null;
		try {
			proxyUri = request.getProxyUri();
		} catch (URISyntaxException e) {
			LOG.warning(String.format("Proxy-uri malformed: %s", request.getFirstOption(OptionNumberRegistry.PROXY_URI)));
		}

		if (proxyUri == null) {
			// throw new IllegalArgumentException("proxyUri == null");
			return;
		}

		// manage the address requester
		String addressString = proxyUri.getHost();
		if (addressString != null) {
			// manage the resource requested
			String resourceString = proxyUri.getPath();
			if (resourceString != null) {
				// check if there is already an entry for the row/column
				// association
				StatHelper statHelper = statsTable.get(addressString, resourceString);
				if (statHelper == null) {
					// create a new stat if it not present
					statHelper = new StatHelper();

					// add the new element to the table
					statsTable.put(addressString, resourceString, statHelper);
				}

				// increment the count of the requests
				statHelper.increment(cachedResponse);
			}
		}
	}

	/**
	 * Builds a pretty print from the statistics gathered.
	 * 
	 * @return
	 */
	private String getStatString() {
		StringBuilder builder = new StringBuilder();

		builder.append(String.format("Served %d addresses and %d resources\n", statsTable.rowKeySet().size(), statsTable.cellSet().size()));
		builder.append("＿\n");
		// iterate over every row (addresses)
		for (String address : statsTable.rowKeySet()) {
			builder.append(String.format("|- %s\n", address));
			builder.append("|\t ＿\n");
			// iterate over every column for a specific address
			for (String resource : statsTable.row(address).keySet()) {
				builder.append(String.format("|\t |- %s: \n", resource));

				// get the statistics
				StatHelper statHelper = statsTable.get(address, resource);
				builder.append(String.format("|\t |------ total requests: %d\n", statHelper.getTotalCount()));
				builder.append(String.format("|\t |------ total cached replies: %d\n", statHelper.getCachedCount()));
				// builder.append(String.format("|\t |------ last period (%d sec) requests: %d\n",
				// PERIOD_SECONDS, statHelper.getLastPeriodCount()));
				// builder.append(String.format("|\t |------ last period (%d sec) avg delay (nanosec): %d\n",
				// PERIOD_SECONDS, statHelper.getLastPeriodAvgDelay()));
				builder.append("|\t |\n");
			}
			builder.append("|\t ￣\n");
			builder.append("|\n");
		}
		builder.append("￣\n");

		return builder.length() == 0 ? "The proxy has not received any request, yet." : builder.toString();
	}

	private static final class CacheStatResource extends LocalResource {
		private CacheStats relativeCacheStats;
		private final CacheResource cacheResource;

		private static final long DEFAULT_LOGGING_DELAY = 5;
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

		/**
		 * Instantiates a new debug resource.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 * @param cacheResource
		 */
		public CacheStatResource(String resourceIdentifier, CacheResource cacheResource) {
			super(resourceIdentifier);

			this.cacheResource = cacheResource;
			relativeCacheStats = cacheResource.getCacheStats();
		}

		/**
		 * Method to get the stats about the cache.
		 * 
		 * @return
		 */
		public String getStats() {
			StringBuilder stringBuilder = new StringBuilder();
			CacheStats cacheStats = cacheResource.getCacheStats().minus(relativeCacheStats);

			stringBuilder.append(String.format("Total succesful loaded values: %d %n", cacheStats.loadSuccessCount()));
			stringBuilder.append(String.format("Total requests: %d %n", cacheStats.requestCount()));
			stringBuilder.append(String.format("Hits ratio: %d/%d - %.3f %n", cacheStats.hitCount(), cacheStats.missCount(), cacheStats.hitRate()));
			stringBuilder.append(String.format("Average time spent loading new values (nanoseconds): %.3f %n", cacheStats.averageLoadPenalty()));
			stringBuilder.append(String.format("Number of cache evictions: %d %n", cacheStats.evictionCount()));

			return stringBuilder.toString();
		}

		@Override
		public void performDELETE(DELETERequest request) {
			// reset the cache
			relativeCacheStats = cacheResource.getCacheStats().minus(relativeCacheStats);
			request.respond(CodeRegistry.RESP_DELETED);
		}

		@Override
		public void performGET(GETRequest request) {
			String payload = "Available commands:\n - GET: show statistics\n - POST write stats to file\n - DELETE: reset statistics\n\n";
			payload += getStats();
			request.respond(CodeRegistry.RESP_CONTENT, payload, MediaTypeRegistry.TEXT_PLAIN);
		}

		@Override
		public void performPOST(POSTRequest request) {
			// TODO include stopping the writing => make something for the whole
			// proxy
			// executor.shutdown();
			// request.respond(CodeRegistry.RESP_DELETED, "Stopped",
			// MediaTypeRegistry.TEXT_PLAIN);

			// starting to log the stats on a new file

			// create the new file
			String logName = System.nanoTime() + CACHE_LOG_NAME;
			final File cacheLog = new File(logName);
			try {
				cacheLog.createNewFile();

				// write the header
				Files.write("hits%, avg. load, #evictions \n", cacheLog, Charset.defaultCharset());
			} catch (IOException e) {
			}

			executor.scheduleWithFixedDelay(new Runnable() {

				@Override
				public void run() {
					CacheStats cacheStats = cacheResource.getCacheStats().minus(relativeCacheStats);

					String csvStats = String.format("%.3f, %.3f, %d %n", cacheStats.hitRate(), cacheStats.averageLoadPenalty(), cacheStats.evictionCount());
					try {
						Files.append(csvStats, cacheLog, Charset.defaultCharset());
					} catch (IOException e) {
					}
				}
			}, 0, DEFAULT_LOGGING_DELAY, TimeUnit.SECONDS);

			request.respond(CodeRegistry.RESP_CREATED, "Creted log: " + logName, MediaTypeRegistry.TEXT_PLAIN);
		}
	}

	private final class ProxyStatResource extends LocalResource {

		public ProxyStatResource(String resourceIdentifier) {
			super(resourceIdentifier);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * ch.ethz.inf.vs.californium.endpoint.resources.LocalResource#performDELETE
		 * (ch.ethz.inf.vs.californium.coap.DELETERequest)
		 */
		@Override
		public void performDELETE(DELETERequest request) {
			// reset all the statistics
			statsTable.clear();
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
			String payload = "Available commands:\n - GET: show statistics\n - POST write stats to file\n - DELETE: reset statistics\n\n";
			payload += getStatString();
			request.respond(CodeRegistry.RESP_CONTENT, payload, MediaTypeRegistry.TEXT_PLAIN);
		}

		// TODO
		// @Override
		// public void performPOST(POSTRequest request) {
		// // TODO Auto-generated method stub
		// super.performPOST(request);
		// }
	}

	/**
	 * The Class StatisticsHelper.
	 * 
	 * @author Francesco Corazza
	 */
	private static class StatHelper {
		private int totalCount = 0;
		private final Set<Long> lastPeriodTimestamps = new ConcurrentSkipListSet<Long>();
		private int cachedCount = 0;

		public int getCachedCount() {
			return cachedCount;
		}

		/**
		 * @return the lastMinuteAvgDelay
		 */
		public long getLastPeriodAvgDelay() {
			cleanTimestamps();

			long totDelays = 0;
			long previousTimestamp = 0;
			for (Long timestamp : lastPeriodTimestamps) {
				if (previousTimestamp == 0) {
					previousTimestamp = timestamp;
				} else {
					long delay = timestamp - previousTimestamp;
					totDelays += delay;
				}
			}

			return totDelays == 0 ? 0 : totDelays / (lastPeriodTimestamps.size() - 1);
		}

		/**
		 * @return the lastMinuteCount
		 */
		public int getLastPeriodCount() {
			cleanTimestamps();
			return lastPeriodTimestamps.size();
		}

		/**
		 * @return the totalCount
		 */
		public int getTotalCount() {
			return totalCount;
		}

		public void increment(boolean cachedResponse) {
			// add the total request counter
			totalCount++;
			if (cachedResponse) {
				cachedCount++;
			}

			// add the new request's timestamp to the list
			// long currentTimestamp = System.nanoTime();
			// lastPeriodTimestamps.add(currentTimestamp);

			// clean the list by the old entries
			// cleanTimestamps(currentTimestamp);
		}

		private void cleanTimestamps() {
			cleanTimestamps(System.nanoTime());
		}

		private void cleanTimestamps(long currentTimestamp) {
			// set the lower bound for the list as 60 seconds before the
			// currentTimestamp
			long nanos = TimeUnit.SECONDS.toNanos(PERIOD_SECONDS);
			long lowerBound = currentTimestamp - nanos;

			// to remove elements in the underlying collection, it is needed an
			// iterator
			Iterator<Long> iterator = lastPeriodTimestamps.iterator();
			while (iterator.hasNext()) {
				Long timestamp = iterator.next();
				// remove the earlier timestamps
				if (timestamp < lowerBound) {
					iterator.remove();
				}
			}
		}
	}
}

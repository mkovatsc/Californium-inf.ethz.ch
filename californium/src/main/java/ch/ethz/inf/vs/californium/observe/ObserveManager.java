/*******************************************************************************
 * Copyright (c) 2014, Institute for Pervasive Computing, ETH Zurich.
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
package ch.ethz.inf.vs.californium.observe;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The observe manager holds a mapping of endpoint addresses to
 * {@link ObservingEndpoint}s. It makes sure that there be only one
 * ObservingEndpoint that represents the observe relations from one endpoint to
 * this server. This important in case we want to cancel all relations to a
 * specific endpoint, e.g., when a confirmable notification timeouts.
 * <p>
 * Notice that each server has its own ObserveManager. If a server binds to
 * multiple endpoints, the ObserveManager keeps the observe relations for all of
 * them.
 */
//TODO: find a better name... how about ObserveObserver -.-
public class ObserveManager {

	/** The mapping from endpoint addresses to ObservingEndpoints */
	private final ConcurrentHashMap<InetSocketAddress, ObservingEndpoint> endpoints;
	
	/**
	 * Constructs a new ObserveManager for this server.
	 */
	public ObserveManager() {
		endpoints = new ConcurrentHashMap<InetSocketAddress, ObservingEndpoint>();
	}
	
	/**
	 * Find the ObservingEndpoint for the specified endpoint address or create
	 * a new one if none exists yet. Does not return null.
	 * 
	 * @param address the address
	 * @return the ObservingEndpoint for the address
	 */
	public ObservingEndpoint findObservingEndpoint(InetSocketAddress address) {
		ObservingEndpoint ep = endpoints.get(address);
		if (ep == null)
			ep = createObservingEndpoint(address);
		return ep;
	}
	
	/**
	 * Return the ObservingEndpoint for the specified endpoint address or null
	 * if none exists.
	 * 
	 * @param address the address
	 * @return the ObservingEndpoint or null
	 */
	public ObservingEndpoint getObservingEndpoint(InetSocketAddress address) {
		return endpoints.get(address);
	}
	
	/**
	 * Atomically creates a new ObservingEndpoint for the specified address.
	 * 
	 * @param address the address
	 * @return the ObservingEndpoint
	 */
	private ObservingEndpoint createObservingEndpoint(InetSocketAddress address) {
		ObservingEndpoint ep = new ObservingEndpoint(address);
		
		// Make sure, there is exactly one ep with the specified address (atomic creation)
		ObservingEndpoint previous = endpoints.putIfAbsent(address, ep);
		if (previous != null) {
			return previous; // and forget ep again
		} else {
			return ep;
		}
	}

	public ObserveRelation getRelation(InetSocketAddress source, byte[] token) {
		ObservingEndpoint remote = getObservingEndpoint(source);
		if (remote!=null) {
			return remote.getObserveRelation(token);
		} else {
			return null;
		}
	}
	
}

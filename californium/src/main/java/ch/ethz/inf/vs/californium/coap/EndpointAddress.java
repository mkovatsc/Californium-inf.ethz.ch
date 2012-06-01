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
package ch.ethz.inf.vs.californium.coap;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class EndpointAddress stores IP address and port. It is mainly used to handle {@link Message}s.
 * 
 * @author Matthias Kovatsch
 */
public class EndpointAddress {

// Logging /////////////////////////////////////////////////////////////////////
	
	protected static final Logger LOG = Logger.getLogger(EndpointAddress.class.getName());

// Members /////////////////////////////////////////////////////////////////////
	
	/** The address. */
	private InetAddress address = null;
	
	/** The port. */
	private int port = Properties.std.getInt("DEFAULT_PORT");

// Constructors ////////////////////////////////////////////////////////////////
	
	/**
	 * Instantiates a new endpoint address using the default port.
	 *
	 * @param address the IP address
	 */
	public EndpointAddress(InetAddress address) {
		this.address = address;
	}
	
	/**
	 * Instantiates a new endpoint address, setting both, IP and port.
	 *
	 * @param address the IP address
	 * @param port the custom port
	 */
	public EndpointAddress(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	/**
	 * A convenience constructor that takes the address information from a URI object.
	 *
	 * @param uri the URI
	 */
	public EndpointAddress(URI uri) {
		// Allow for correction later, as host might be unknown at initialization time.
		try {
			this.address = InetAddress.getByName(uri.getHost());
		} catch (UnknownHostException e) {
			LOG.warning(String.format("Cannot fully initialize: %s", e.getMessage()));
		}
		if (uri.getPort()!=-1) this.port = uri.getPort();
	}

// Methods /////////////////////////////////////////////////////////////////////
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (this.address instanceof Inet6Address) {
			return String.format("[%s]:%d", this.address.getHostAddress(), this.port);
		} else {
			return String.format("%s:%d", this.address.getHostAddress(), this.port);
		}
	}
	
	/**
	 * Returns the IP address.
	 *
	 * @return the address
	 */
	public InetAddress getAddress() {
		return this.address;
	}
	
	/**
	 * Returns the port number.
	 *
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}
}

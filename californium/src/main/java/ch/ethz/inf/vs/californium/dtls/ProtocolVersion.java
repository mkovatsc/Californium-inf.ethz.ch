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
package ch.ethz.inf.vs.californium.dtls;

/**
 * Represents the protocol version.
 */
public class ProtocolVersion implements Comparable<ProtocolVersion> {
	
	/** The minor. */
	private int minor;
	
	/** The major. */
	private int major;
	
	/**
	 * The latest version supported.
	 */
	public ProtocolVersion() {
		this.major = 254;
		this.minor = 253;
	}

	/**
	 * Instantiates a new protocol version.
	 *
	
	 * @param major the major
	 * @param minor the minor
	 */
	public ProtocolVersion(int major, int minor) {
		this.minor = minor;
		this.major = major;
	}
	
	public int getMinor() {
		return minor;
	}

	public int getMajor() {
		return major;
	}

	@Override
	public int compareTo(ProtocolVersion o) {
		/*
		 * Example, version 1.0 (254,255) is smaller than version 1.2 (254,253)
		 */
		
		if (major == o.getMajor()) {
			if (minor < o.getMinor()) {
				return 1;
			} else if (minor > o.getMinor()) {
				return -1;
			} else {
				return 0;
			}
		} else if (major < o.getMajor()) {
			return 1;
		} else {
			return -1;
		}
	}
	
	
}
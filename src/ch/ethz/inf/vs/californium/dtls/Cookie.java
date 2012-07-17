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
 * Represents a stateless cookie which is used in the {@link HelloVerifyRequest}
 * in the DTLS handshake to prevent denial-of-service attacks. See <a
 * href="http://tools.ietf.org/html/rfc6347#section-4.3.2">RFC 6347</a> for
 * further details.
 * 
 * @author Stefan Jucker
 * 
 */
public class Cookie {

	/** The cookie as byte array. */
	private byte[] cookie;

	/**
	 * Used by client, when sending a {@link ClientHello} for the first time
	 * (empty cookie).
	 */
	public Cookie() {
		this.cookie = new byte[] {};
	}

	/**
	 * Called when sending a {@link HelloVerifyRequest} (server) or
	 * {@link ClientHello} (client) for the second time.
	 * 
	 * @param cookie
	 *            the Cookie.
	 */
	public Cookie(byte[] cookie) {
		this.cookie = cookie;
	}

	/**
	 * 
	 * @return the number of bytes of the cookie.
	 */
	public int length() {
		return cookie.length;
	}

	public byte[] getCookie() {
		return cookie;
	}

	public void setCookie(byte[] cookie) {
		this.cookie = cookie;
	}
}

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

import ch.ethz.inf.vs.californium.util.ByteArrayUtils;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * The server send this request after receiving a {@link ClientHello} message to
 * prevent Denial-of-Service Attacks. See <a
 * href="http://tools.ietf.org/html/rfc6347#section-4.2.1">RFC 6347</a> for the
 * definition.
 * 
 * @author Stefan Jucker
 * 
 */
public class HelloVerifyRequest extends HandshakeMessage {

	// DTLS-specific constants ///////////////////////////////////////////

	private static final int VERSION_BITS = 8; // for major and minor each

	private static final int COOKIE_LENGTH_BITS = 8;

	// Members ///////////////////////////////////////////////////////////

	/**
	 * This field will contain the lower of that suggested by the client in the
	 * client hello and the highest supported by the server.
	 */
	private ProtocolVersion serverVersion;

	/** The cookie which needs to be replayed by the client. */
	private Cookie cookie;
	
	// Constructor ////////////////////////////////////////////////////

	public HelloVerifyRequest(ProtocolVersion version, Cookie cookie) {
		this.serverVersion = version;
		this.cookie = cookie;
	}
	
	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] fragmentToByteArray() {
		DatagramWriter writer = new DatagramWriter();

		writer.write(serverVersion.getMajor(), VERSION_BITS);
		writer.write(serverVersion.getMinor(), VERSION_BITS);

		writer.write(cookie.length(), COOKIE_LENGTH_BITS);
		writer.writeBytes(cookie.getCookie());

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		int major = reader.read(VERSION_BITS);
		int minor = reader.read(VERSION_BITS);
		ProtocolVersion version = new ProtocolVersion(major, minor);

		int cookieLength = reader.read(COOKIE_LENGTH_BITS);
		Cookie cookie = new Cookie(reader.readBytes(cookieLength));

		return new HelloVerifyRequest(version, cookie);
	}
	
	// Methods ////////////////////////////////////////////////////////

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.HELLO_VERIFY_REQUEST;
	}

	@Override
	public int getMessageLength() {
		// fixed: version (2) + cookie length (1)
		return 3 + cookie.length();
	}

	public ProtocolVersion getServerVersion() {
		return serverVersion;
	}

	public Cookie getCookie() {
		return cookie;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\tServer Version: " + serverVersion.getMajor() + ", " + serverVersion.getMinor() + "\n");
		sb.append("\t\tCookie Length: " + cookie.length() + "\n");
		sb.append("\t\tCookie: " + ByteArrayUtils.toHexString(cookie.getCookie()) + "\n");

		return sb.toString();
	}

}

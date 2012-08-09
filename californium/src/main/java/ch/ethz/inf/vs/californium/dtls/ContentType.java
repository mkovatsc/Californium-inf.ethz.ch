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
 * The content type represents a higher-level protocol to process the enclosed
 * fragment. It is one of the four types: ChangeCipherSpec, Alert, Handshake,
 * ApplicationData. For further details see <a
 * href="http://tools.ietf.org/html/rfc5246#appendix-A.1">RFC 5246</a>.
 * 
 * @author Stefan Jucker
 * 
 */
public enum ContentType {

	CHANGE_CIPHER_SPEC(20), ALERT(21), HANDSHAKE(22), APPLICATION_DATA(23);

	private int code;

	public int getCode() {
		return code;
	}

	ContentType(int code) {
		this.code = code;
	}

	/**
	 * Returns the content type according to the given code. Needed when
	 * reconstructing a received byte array.
	 * 
	 * @param code
	 *            the code representation of the content type (i.e. 20, 21, 22,
	 *            23).
	 * @return the corresponding content type.
	 */
	public static ContentType getTypeByValue(int code) {
		switch (code) {
		case 20:
			return ContentType.CHANGE_CIPHER_SPEC;
		case 21:
			return ContentType.ALERT;
		case 22:
			return ContentType.HANDSHAKE;
		case 23:
			return ContentType.APPLICATION_DATA;

		default:
			return null;
		}
	}

	@Override
	public String toString() {
		switch (code) {
		case 20:
			return "Change Cipher Spec (20)";
		case 21:
			return "Alert (21)";
		case 22:
			return "Handshake (22)";
		case 23:
			return "Application Data (23)";

		default:
			return "Unknown Content Type";
		}
	}
}
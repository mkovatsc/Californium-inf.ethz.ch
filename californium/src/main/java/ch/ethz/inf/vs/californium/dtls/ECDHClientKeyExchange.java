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

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

import ch.ethz.inf.vs.californium.util.ByteArrayUtils;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * {@link ClientKeyExchange} message for all ECDH based key exchange methods.
 * Contains the client's ephemeral public value. See <a
 * href="http://tools.ietf.org/html/rfc4492#section-5.7">RFC 4492</a> for further details. It is assumed, that the client's
 * ECDH public key is not in the client's certificate, so it must be provided
 * here.
 * 
 * @author Stefan Jucker
 * 
 */
public class ECDHClientKeyExchange extends ClientKeyExchange {

	// DTLS-specific constants ////////////////////////////////////////
	
	protected static final int LENGTH_BITS = 8; // opaque point <1..2^8-1>;

	// Members ////////////////////////////////////////////////////////

	private byte[] pointEncoded;

	// Constructors ///////////////////////////////////////////////////

	/**
	 * Called by the client. Generates the client's ephemeral ECDH public key
	 * (encoded) which represents an elliptic curve point.
	 * 
	 * @param clientPublicKey
	 *            the client's public key.
	 */
	public ECDHClientKeyExchange(PublicKey clientPublicKey) {
		ECPublicKey publicKey = (ECPublicKey) clientPublicKey;
		ECPoint point = publicKey.getW();
		ECParameterSpec params = publicKey.getParams();
		
		pointEncoded = ECDHECryptography.encodePoint(point, params.getCurve());
	}

	/**
	 * Called by the server when receiving a {@link ClientKeyExchange} message.
	 * Stores the encoded point which will be later used, to generate the
	 * premaster secret.
	 * 
	 * @param pointEncoded
	 *            the client's ephemeral public key (encoded point).
	 */
	public ECDHClientKeyExchange(byte[] pointEncoded) {
		this.pointEncoded = pointEncoded;
	}
	
	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] fragmentToByteArray() {
		DatagramWriter writer = new DatagramWriter();

		// TODO only true, if the public value encoding is explicit (not in the
		// client's certificate), see
		// http://tools.ietf.org/html/rfc4492#section-5.7
		int length = pointEncoded.length;
		writer.write(length, LENGTH_BITS);
		writer.writeBytes(pointEncoded);

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		int length = reader.read(LENGTH_BITS);
		byte[] pointEncoded = reader.readBytes(length);

		return new ECDHClientKeyExchange(pointEncoded);
	}
	
	// Methods ////////////////////////////////////////////////////////

	@Override
	public int getMessageLength() {
		// TODO only true, if the public value encoding is explicit
		return 1 + pointEncoded.length;
	}

	byte[] getEncodedPoint() {
		return pointEncoded;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\t" + ByteArrayUtils.toHexString(pointEncoded) + "\n");

		return sb.toString();
	}

}

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

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * This message is used to provide explicit verification of a client
 * certificate. This message is only sent following a client certificate that
 * has signing capability (i.e., all certificates except those containing fixed
 * Diffie-Hellman parameters). When sent, it MUST immediately follow the
 * {@link ClientKeyExchange} message. For further details see <a
 * href="http://tools.ietf.org/html/rfc5246#section-7.4.8">RFC 5246</a>.
 * 
 * @author Stefan Jucker
 * 
 */
public class CertificateVerify extends HandshakeMessage {

	// DTLS-specific constants ////////////////////////////////////////

	private static final int SIGNATURE_LENGTH_BITS = 2;

	// Members ////////////////////////////////////////////////////////

	/** The digitally signed handshake messages. */
	private byte[] signature;

	// Constructor ////////////////////////////////////////////////////

	public CertificateVerify(byte[] signature) {
		this.signature = signature;
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.CERTIFICATE_VERIFY;
	}

	@Override
	public int getMessageLength() {
		/*
		 * fixed: signature length field (2 bytes), see
		 * http://tools.ietf.org/html/rfc5246#section-4.7
		 */
		return 2 + signature.length;
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		// TODO according to http://tools.ietf.org/html/rfc5246#section-4.7 the
		// signature algorithm must also be included

		writer.write(signature.length, SIGNATURE_LENGTH_BITS);
		writer.writeBytes(signature);

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		// TODO according to http://tools.ietf.org/html/rfc5246#section-4.7 the
		// signature algorithm must also be included

		int length = reader.read(SIGNATURE_LENGTH_BITS);
		byte[] signature = reader.readBytes(length);

		return new CertificateVerify(signature);
	}

}

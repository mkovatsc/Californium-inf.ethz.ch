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

import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * The change cipher spec protocol exists to signal transitions in ciphering
 * strategies. The protocol consists of a single message, which is encrypted and
 * compressed under the current (not the pending) connection state. The
 * ChangeCipherSpec message is sent by both the client and the server to notify
 * the receiving party that subsequent records will be protected under the newly
 * negotiated CipherSpec and keys. For further details see <a
 * href="http://tools.ietf.org/html/rfc5246#section-7.1">RFC 5246</a>.
 * 
 * @author Stefan Jucker
 * 
 */
public class ChangeCipherSpecMessage implements DTLSMessage {

	// DTLS-specific constants ////////////////////////////////////////

	private static final int CCS_BITS = 8;

	// Members ////////////////////////////////////////////////////////

	private CCSType CCSProtocolType;

	// Constructor ////////////////////////////////////////////////////

	public ChangeCipherSpecMessage() {
		CCSProtocolType = CCSType.CHANGE_CIPHER_SPEC;
	}

	// Change Cipher Spec Enum ////////////////////////////////////////

	/**
	 * See <a href="http://tools.ietf.org/html/rfc5246#section-7.1">RFC 5246</a>
	 * for specification.
	 * 
	 * @author Stefan Jucker
	 * 
	 */
	public enum CCSType {
		CHANGE_CIPHER_SPEC(1);

		private int code;

		private CCSType(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}
	}
	
	// Methods ////////////////////////////////////////////////////////

	public CCSType getCCSProtocolType() {
		return CCSProtocolType;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\tChange Cipher Spec Message\n");
		return sb.toString();
	}
	
	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.write(CCSProtocolType.getCode(), CCS_BITS);

		return writer.toByteArray();
	}

	public static DTLSMessage fromByteArray(byte[] byteArray) throws HandshakeException {
		DatagramReader reader = new DatagramReader(byteArray);
		int code = reader.read(CCS_BITS);
		if (code == CCSType.CHANGE_CIPHER_SPEC.getCode()) {
			return new ChangeCipherSpecMessage();
		} else {
			String message = "Unknown Change Cipher Spec code received: " + code;
			AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
			throw new HandshakeException(message, alert);
		}
	}

}

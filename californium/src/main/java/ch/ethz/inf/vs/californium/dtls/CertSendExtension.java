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

import ch.ethz.inf.vs.californium.dtls.HelloExtensions.ExtensionType;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * The cert-send extension indicates the certificate format found in the
 * Certificate payload itself. See <a
 * href="http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-04#section-4.2"
 * >TLS Handshake Extension</a> for details.
 * 
 * @author Stefan Jucker
 * 
 */
public class CertSendExtension extends HelloExtension {

	// DTLS-specific constants ////////////////////////////////////////

	private static final int CERT_TYPE_BITS = 8;

	// Members ////////////////////////////////////////////////////////

	/** The type of the following Certificate message. */
	private CertType certType;

	// Constructors ///////////////////////////////////////////////////

	public CertSendExtension(CertType certType) {
		super(ExtensionType.CERT_SEND);
		this.certType = certType;
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		writer.write(1, LENGTH_BITS);
		writer.write(certType.getCode(), CERT_TYPE_BITS);

		return writer.toByteArray();
	}

	public static HelloExtension fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		CertType type = CertType.getTypeFromCode(reader.read(CERT_TYPE_BITS));

		return new CertSendExtension(type);
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public int getLength() {
		// fixed: extension type (2 bytes) + extension length field (2 bytes) +
		// certificate type (1 byte)
		return 5;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\t\t\t\tCert-Send: " + certType.toString() + "\n");

		return sb.toString();
	}
	
	public CertType getCertType() {
		return certType;
	}

	// Enum ///////////////////////////////////////////////////////////

	/**
	 * Represents the possible certificate types defined <a href=
	 * "http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-04#section-3"
	 * >here</a>. The value will not be greater than 255, therefore 1 byte
	 * suffices to encode it.
	 * 
	 * @author Stefan Jucker
	 * 
	 */
	public enum CertType {
		X_509(0), RAW_PUBLIC_KEY(1);

		private int code;

		private CertType(int code) {
			this.code = code;
		}

		public static CertType getTypeFromCode(int code) {
			switch (code) {
			case 0:
				return X_509;
			case 1:
				return RAW_PUBLIC_KEY;

			default:
				return null;
			}
		}

		int getCode() {
			return code;
		}
	}

}

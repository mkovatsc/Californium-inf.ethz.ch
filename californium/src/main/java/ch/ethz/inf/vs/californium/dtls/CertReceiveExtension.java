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

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.dtls.CertSendExtension.CertType;
import ch.ethz.inf.vs.californium.dtls.HelloExtensions.ExtensionType;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * the cert-receive extension indicates the client's ability to process certain
 * certificate types when receiving the server's {@link CertificateMessage}. See
 * <a
 * href="http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-04#section-4.1">
 * TLS Handshake Extension</a> for details.
 * 
 * @author Stefan Jucker
 * 
 */
public class CertReceiveExtension extends HelloExtension {

	// DTLS-specific constants ////////////////////////////////////////

	private static final int LIST_LENGTH_BITS = 8;

	private static final int CERT_TYPE_BITS = 8;

	// Members ////////////////////////////////////////////////////////

	/**
	 * The supported types of certificates the peer is allowed to send. Ordered
	 * by preference.
	 */
	private List<CertType> certTypes;

	// Constructors ///////////////////////////////////////////////////

	public CertReceiveExtension(List<CertType> certTypes) {
		super(ExtensionType.CERT_RECEIVE);
		this.certTypes = certTypes;
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		int listLength = certTypes.size();
		// list length + list length field (1 byte)
		writer.write(listLength + 1, LENGTH_BITS);
		writer.write(listLength, LIST_LENGTH_BITS);

		for (CertType type : certTypes) {
			writer.write(type.getCode(), CERT_TYPE_BITS);
		}

		return writer.toByteArray();
	}

	public static HelloExtension fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		int listLength = reader.read(LIST_LENGTH_BITS);

		List<CertType> certTypes = new ArrayList<CertType>();
		while (listLength > 0) {
			certTypes.add(CertType.getTypeFromCode((reader.read(CERT_TYPE_BITS))));

			// one cert type uses 1 byte
			listLength -= 1;
		}

		return new CertReceiveExtension(certTypes);
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public int getLength() {
		// fixed: extension type (2 bytes) + extension length field (2 bytes) +
		// list length field (1 byte)
		return 5 + certTypes.size();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\t\t\t\tCert-Receive:\n");
		for (CertType type : certTypes) {
			sb.append("\t\t\t\t\t" + type.toString() + "\n");
		}

		return sb.toString();
	}

	public List<CertType> getCertTypes() {
		return certTypes;
	}

}

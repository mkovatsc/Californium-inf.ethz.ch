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

import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;
import ch.ethz.inf.vs.californium.dtls.HelloExtensions.ExtensionType;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * 
 * An abstract class representing the functionality for all possible defined
 * extensions. See <a
 * href="http://tools.ietf.org/html/rfc5246#section-7.4.1.4">RFC 5246</a> for
 * the extension format.
 * 
 * @author Stefan Jucker
 * 
 */
public abstract class HelloExtension {

	// Logging ///////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(HelloExtension.class.getName());

	// DTLS-specific constants ////////////////////////////////////////

	private static final int TYPE_BITS = 16;

	protected static final int LENGTH_BITS = 16;

	// Members ////////////////////////////////////////////////////////

	private ExtensionType type;

	// Constructors ///////////////////////////////////////////////////

	public HelloExtension(ExtensionType type) {
		this.type = type;
	}

	// Abstract methods ///////////////////////////////////////////////

	public abstract int getLength();

	// Serialization //////////////////////////////////////////////////

	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();

		writer.write(type.getId(), TYPE_BITS);

		return writer.toByteArray();
	}
	
	public static HelloExtension fromByteArray(byte[] byteArray, ExtensionType type) throws HandshakeException {

		switch (type) {
		// the currently supported extensions, throws an exception if other extension type received
		case ELLIPTIC_CURVES:
			return SupportedEllipticCurvesExtension.fromByteArray(byteArray);
		case EC_POINT_FORMATS:
			return SupportedPointFormatsExtension.fromByteArray(byteArray);
		case CERT_TYPE:
			return CertificateTypeExtension.fromByteArray(byteArray);
		case CERT_SEND:
			return CertSendExtension.fromByteArray(byteArray);
		case CERT_RECEIVE:
			return CertReceiveExtension.fromByteArray(byteArray);

		default:
			AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.UNSUPPORTED_EXTENSION);
			throw new HandshakeException("Unsupported extension type received: " + type.toString(), alert);
		}

	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\t\t\tExtension: " + type.toString() + " (" + type.getId() + ")\n");

		return sb.toString();
	}
}

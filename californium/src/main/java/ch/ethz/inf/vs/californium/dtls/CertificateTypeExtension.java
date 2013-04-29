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

import ch.ethz.inf.vs.californium.dtls.HelloExtensions.ExtensionType;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * This represents the Certificate Type Extension. See <a
 * href="http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-03">Draft</a> for
 * details.
 * 
 * @author Stefan Jucker
 * 
 */
public abstract class CertificateTypeExtension extends HelloExtension {

	// DTLS-specific constants ////////////////////////////////////////
	
	protected static final int LIST_FIELD_LENGTH_BITS = 8;
	
	protected static final int EXTENSION_TYPE_BITS = 8;

	// Members ////////////////////////////////////////////////////////

	/**
	 * Indicates whether this extension belongs to a client or a server. This
	 * has an impact upon the message format. See <a href=
	 * "http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-03#section-3.1"
	 * >CertificateTypeExtension</a> definition.
	 */
	private boolean isClientExtension;

	/**
	 * For the client: a list of certificate types the client supports, sorted
	 * by client preference.<br />
	 * For the server: the certificate selected by the server out of the
	 * client's list.
	 */
	protected List<CertificateType> certificateTypes;
	
	// Constructors ///////////////////////////////////////////////////

	public CertificateTypeExtension(ExtensionType type, boolean isClient) {
		super(type);
		this.isClientExtension = isClient;
		this.certificateTypes = new ArrayList<CertificateType>();
	}

	public CertificateTypeExtension(ExtensionType type, boolean isClient, List<CertificateType> certificateTypes) {
		super(type);
		this.isClientExtension = isClient;
		this.certificateTypes = certificateTypes;
	}

	// Methods ////////////////////////////////////////////////////////
	
	public boolean isClientExtension() {
		return isClientExtension;
	}

	@Override
	public int getLength() {
		if (isClientExtension) {
			// fixed:  type (2 bytes), length (2 bytes), the list length field (1 byte)
			// each certificate type in the list uses 1 byte
			return 5 + certificateTypes.size();
		} else {
			//  type (2 bytes), length (2 bytes), the certificate type (1 byte)
			return 5;
		}
	}
	
	public String toString() {
		return super.toString();
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());
		
		if (isClientExtension) {
			int listLength = certificateTypes.size();			
			writer.write(listLength + 1, LENGTH_BITS);
			writer.write(listLength, LIST_FIELD_LENGTH_BITS);
			for (CertificateType type : certificateTypes) {
				writer.write(type.getCode(), EXTENSION_TYPE_BITS);
			}
		} else {
			// we assume the list contains exactly one element
			writer.write(1, LENGTH_BITS);
			writer.write(certificateTypes.get(0).getCode(), EXTENSION_TYPE_BITS);
		}
		
		return writer.toByteArray();
	}
	
	// Enums //////////////////////////////////////////////////////////

	/**
	 * See <a href=
	 * "http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-03#section-3.1"
	 * >3.1. ClientHello</a> for the definition. Note: The RawPublicKey code is
	 * <tt>TBD</tt>, but we assumed for now the reasonable value 2.
	 * 
	 * @author Stefan Jucker
	 * 
	 */
	public enum CertificateType {
		// TODO this maybe change in the future
		X_509(0), OPEN_PGP(1), RAW_PUBLIC_KEY(2);

		private int code;

		private CertificateType(int code) {
			this.code = code;
		}
		
		public static CertificateType getTypeFromCode(int code) {
			switch (code) {
			case 0:
				return X_509;
			case 1:
				return OPEN_PGP;
			case 2:
				return RAW_PUBLIC_KEY;

			default:
				return null;
			}
		}

		int getCode() {
			return code;
		}
	}
	
	// Getters and Setters ////////////////////////////////////////////
	
	public void addCertificateType(CertificateType certificateType) {
		if (!isClientExtension && this.certificateTypes.size() > 0) {
			// the server is only allowed to include 1 certificate type in its ServerHello
			return;
		}
		this.certificateTypes.add(certificateType);
	}

	public List<CertificateType> getCertificateTypes() {
		return certificateTypes;
	}

}

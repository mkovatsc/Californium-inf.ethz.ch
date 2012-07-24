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

import ch.ethz.inf.vs.californium.dtls.CertificateRequest.HashAlgorithm;
import ch.ethz.inf.vs.californium.dtls.CertificateRequest.SignatureAlgorithm;

/**
 * See <a href="http://tools.ietf.org/html/rfc5246#appendix-A.4.1">RFC 5246</a>
 * for details.
 * 
 * @author Stefan Jucker
 * 
 */
public class SignatureAndHashAlgorithm {
	
	// Members ////////////////////////////////////////////////////////

	private HashAlgorithm hash;

	private SignatureAlgorithm signature;
	
	// Constructors ///////////////////////////////////////////////////

	public SignatureAndHashAlgorithm(HashAlgorithm hashAlgorithm, SignatureAlgorithm signatureAlgorithm) {
		this.signature = signatureAlgorithm;
		this.hash = hashAlgorithm;
	}
	
	/**
	 * Constructs it with the corresponding codes (received when parsing the
	 * received message).
	 * 
	 * @param hashAlgorithmCode
	 *            the hash algorithm's code.
	 * @param signatureAlgorithmCode
	 *            the signature algorithm's code.
	 */
	public SignatureAndHashAlgorithm(int hashAlgorithmCode, int signatureAlgorithmCode) {
		this.signature = SignatureAlgorithm.getAlgorithmByCode(signatureAlgorithmCode);
		this.hash = HashAlgorithm.getAlgorithmByCode(hashAlgorithmCode);
	}
	
	// Getters and Setters ////////////////////////////////////////////

	public SignatureAlgorithm getSignature() {
		return signature;
	}

	public void setSignature(SignatureAlgorithm signature) {
		this.signature = signature;
	}

	public HashAlgorithm getHash() {
		return hash;
	}

	public void setHash(HashAlgorithm hash) {
		this.hash = hash;
	}
	
	@Override
	public String toString() {
		// Construct the signature algorithm according to
		// http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Signature
		return hash.toString() + "with" + signature.toString();
	}
}
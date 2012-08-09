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
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * A cipher suite defines key exchange algorithm, the bulk cipher algorithm, the
 * mac algorithm, the prf algorithm and the cipher type. See <a
 * href="http://tools.ietf.org/html/rfc5246#appendix-A.6">RFC 5246</a> for
 * details. See <a
 * href="http://www.iana.org/assignments/tls-parameters/tls-parameters.xml"
 * >IANA</a> for Transport Layer Security Parameters.
 * 
 * @author Stefan Jucker
 * 
 */
public enum CipherSuite {
	
	// Cipher suites //////////////////////////////////////////////////

	SSL_NULL_WITH_NULL_NULL("SSL_NULL_WITH_NULL_NULL", 0x0000, KeyExchangeAlgorithm.NULL, BulkCipherAlgorithm.NULL, MACAlgorithm.NULL, PRFAlgorithm.TLS_PRF_SHA256, CipherType.NULL),
	TLS_PSK_WITH_AES_128_CCM_8("TLS_PSK_WITH_AES_128_CCM_8", 0xC0A8, KeyExchangeAlgorithm.PSK, BulkCipherAlgorithm.AES,	MACAlgorithm.NULL, PRFAlgorithm.TLS_PRF_SHA256,	CipherType.AEAD),
	// TODO change code once officially specified by IANA
	TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8("TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8", 0xC0AC, KeyExchangeAlgorithm.EC_DIFFIE_HELLMAN, BulkCipherAlgorithm.AES, MACAlgorithm.NULL, PRFAlgorithm.TLS_PRF_SHA256, CipherType.AEAD);
	
	// Logging ////////////////////////////////////////////////////////

	private static final Logger LOG = Logger.getLogger(CipherSuite.class.getName());

	// DTLS-specific constants ////////////////////////////////////////

	private static final int CIPHER_SUITE_BITS = 16;

	// Members ////////////////////////////////////////////////////////

	/** The name of the cipher suite. */
	private String name;

	/**
	 * 16 bit identification, i.e. 0x0000 for SSL_NULL_WITH_NULL_NULL, see <a
	 * href="http://tools.ietf.org/html/rfc5246#appendix-A.5">RFC 5246</a>.
	 */
	private int code;

	private KeyExchangeAlgorithm keyExchange;
	private BulkCipherAlgorithm bulkCipher;
	private MACAlgorithm macAlgorithm;
	private PRFAlgorithm pseudoRandomFunction;
	private CipherType cipherType;

	// Constructor ////////////////////////////////////////////////////

	private CipherSuite(String name, int code, KeyExchangeAlgorithm keyExchange, BulkCipherAlgorithm bulkCipher, MACAlgorithm macAlgorithm, PRFAlgorithm prf, CipherType cipherType) {
		this.name = name;
		this.code = code;
		this.keyExchange = keyExchange;
		this.bulkCipher = bulkCipher;
		this.macAlgorithm = macAlgorithm;
		this.pseudoRandomFunction = prf;
		this.cipherType = cipherType;
	}
	
	// Getters ////////////////////////////////////////////////////////

	public String getName() {
		return name;
	}

	public int getCode() {
		return code;
	}

	public KeyExchangeAlgorithm getKeyExchange() {
		return keyExchange;
	}

	public BulkCipherAlgorithm getBulkCipher() {
		return bulkCipher;
	}

	public MACAlgorithm getMacAlgorithm() {
		return macAlgorithm;
	}

	public PRFAlgorithm getPseudoRandomFunction() {
		return pseudoRandomFunction;
	}

	public CipherType getCipherType() {
		return cipherType;
	}

	/**
	 * Returns the cipher suite to a given code.
	 * 
	 * @param code
	 * @return the according cipher suite.
	 */
	public static CipherSuite getTypeByCode(int code) {
		switch (code) {
		case 0x0000:
			return CipherSuite.SSL_NULL_WITH_NULL_NULL;
		case 0xC0A8:
			return CipherSuite.TLS_PSK_WITH_AES_128_CCM_8;
		case 0xC0AC:
			return CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;

		default:
			LOG.info("Unknown cipher suite code: " + code);
			return CipherSuite.SSL_NULL_WITH_NULL_NULL;
		}
	}

	// Serialization //////////////////////////////////////////////////

	/**
	 * Transform a list of cipher suites into the appropriate bit-format.
	 * 
	 * @param cipherSuites
	 *            the cipher suites
	 * @return the byte[]
	 */
	public static byte[] listToByteArray(List<CipherSuite> cipherSuites) {

		DatagramWriter writer = new DatagramWriter();
		for (CipherSuite cipherSuite : cipherSuites) {
			writer.write(cipherSuite.getCode(), CIPHER_SUITE_BITS);
		}

		return writer.toByteArray();
	}

	public static List<CipherSuite> listFromByteArray(byte[] byteArray, int numElements) {
		List<CipherSuite> cipherSuites = new ArrayList<CipherSuite>();
		DatagramReader reader = new DatagramReader(byteArray);

		for (int i = 0; i < numElements; i++) {
			int code = reader.read(CIPHER_SUITE_BITS);
			cipherSuites.add(CipherSuite.getTypeByCode(code));
		}
		return cipherSuites;
	}

	// Algorithm Enums ////////////////////////////////////////////////
	
	/*
	 * See http://tools.ietf.org/html/rfc5246#appendix-A.6
	 */

	public enum MACAlgorithm {
		NULL, HMAC_MD5, HMAC_SHA1, HMAC_SHA256, HMAC_SHA384, HMAC_SHA512;
	}

	public enum BulkCipherAlgorithm {
		NULL(0, 0, 0, 0),
		RC4(0, 16, 4, 8), // don't know
		B_3DES(0, 16, 4, 8), // don't know
		AES(0, 16, 4, 8); // http://www.ietf.org/mail-archive/web/tls/current/msg08445.html
		
		// values in octets!
		private int macKeyLength;
		private int encKeyLength;
		private int fixedIvLength;
		private int recordIvLength;
		
		private BulkCipherAlgorithm(int mac_key_length, int enc_key_length, int fixed_iv_length, int recordIvLength) {
			this.macKeyLength = mac_key_length;
			this.encKeyLength = enc_key_length;
			this.fixedIvLength = fixed_iv_length;
			this.recordIvLength = recordIvLength;
		}

		public int getMacKeyLength() {
			return macKeyLength;
		}

		public int getEncKeyLength() {
			return encKeyLength;
		}

		public int getFixedIvLength() {
			return fixedIvLength;
		}

		public int getRecordIvLength() {
			return recordIvLength;
		}
	}

	public enum KeyExchangeAlgorithm {
		NULL, DHE_DSS, DHE_RSA, DH_ANON, RSA, DH_DSS, DH_RSA, PSK, EC_DIFFIE_HELLMAN;
	}
	
	public enum PRFAlgorithm {
		TLS_PRF_SHA256;
	}

	public enum CipherType {
		NULL, STREAM, BLOCK, AEAD;
	}

}

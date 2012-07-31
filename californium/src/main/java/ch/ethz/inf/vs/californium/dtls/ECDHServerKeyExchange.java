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

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * 
 * The server's Ephemeral ECDH with ECDSA signatures. See <a
 * href="http://tools.ietf.org/html/rfc4492">RFC 4492</a>, <a
 * href="http://tools.ietf.org/html/rfc4492#section-5.4">Section 5.4. Server Key
 * Exchange</a>, for details on the message format.
 * 
 * @author Stefan Jucker
 * 
 */
public class ECDHServerKeyExchange extends ServerKeyExchange {

	// Logging ////////////////////////////////////////////////////////

	private static final Logger LOG = Logger.getLogger(ECDHServerKeyExchange.class.getName());

	// DTLS-specific constants ////////////////////////////////////////

	private static final int CURVE_TYPE_BITS = 8;
	private static final int NAMED_CURVE_BITS = 16;
	private static final int PUBLIC_LENGTH_BITS = 8;
	private static final int SIGNATURE_LENGTH_BITS = 16;

	/**
	 * The name of the ECDSA signature algorithm. See also <a href=
	 * "http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Signature"
	 * >Signature Algorithms</a>.
	 */
	private static final String SIGNATURE_INSTANCE = "SHA1withECDSA";

	/**
	 * The algorithm name to generate elliptic curve keypairs. See also <a href=
	 * "http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#KeyPairGenerator"
	 * >KeyPairGenerator Algorithms</a>.
	 */
	private static final String KEYPAIR_GENERATOR_INSTANCE = "EC";

	/** The ECCurveType */
	// parameters are conveyed verbosely; underlying finite field is a prime
	// field
	private static final int EXPLICIT_PRIME = 1;
	// parameters are conveyed verbosely; underlying finite field is a
	// characteristic-2 field
	private static final int EXPLICIT_CHAR2 = 2;
	// a named curve is used
	private static final int NAMED_CURVE = 3;

	// Members ////////////////////////////////////////////////////////

	/** ephemeral keys */
	private ECPublicKey publicKey = null;

	private ECPoint point = null;
	private byte[] pointEncoded = null;

	private int curveId;

	private byte[] signatureEncoded = null;

	// TODO make this variable, 3 = named_curve
	private int curveType = 3;

	// Constructors //////////////////////////////////////////////////

	/**
	 * Called by server, generates ephemeral keys and signature.
	 * 
	 * @param ecdhe
	 *            the ECDHE helper class.
	 * @param serverPrivateKey
	 *            the server's private key.
	 * @param clientRandom
	 *            the client's random (used for signature).
	 * @param serverRandom
	 *            the server's random (used for signature).
	 */
	public ECDHServerKeyExchange(ECDHECryptography ecdhe, PrivateKey serverPrivateKey, Random clientRandom, Random serverRandom) {

		try {
			publicKey = ecdhe.getPublicKey();

			// create public point
			ECParameterSpec parameters = publicKey.getParams();
			// namedCurve will look like this: secp192k1 (1.3.132.0.31)
			String namedCurve = parameters.toString();
			// we only need secp192k1
			namedCurve = namedCurve.substring(0, 9);

			curveId = NAMED_CURVE_INDEX.get(namedCurve);
			point = publicKey.getW();
			pointEncoded = ECDHECryptography.encodePoint(point, parameters.getCurve());

			// make signature
			// See http://tools.ietf.org/html/rfc4492#section-2.2
			// These parameters MUST be signed with ECDSA using the private key
			// corresponding to the public key in the server's Certificate.
			Signature signature = Signature.getInstance(SIGNATURE_INSTANCE);
			signature.initSign(serverPrivateKey);

			updateSignature(signature, clientRandom, serverRandom);

			signatureEncoded = signature.sign();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when reconstructing the byte array.
	 * 
	 * @param curveId
	 *            the named curve index
	 * @param pointEncoded
	 *            the point on the curve (encoded)
	 * @param signatureEncoded
	 *            the signature (encoded)
	 */
	public ECDHServerKeyExchange(int curveId, byte[] pointEncoded, byte[] signatureEncoded) {
		this.curveId = curveId;
		this.pointEncoded = pointEncoded;
		this.signatureEncoded = signatureEncoded;
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		switch (curveType) {
		case EXPLICIT_PRIME:

			break;

		case EXPLICIT_CHAR2:

			break;

		case NAMED_CURVE:
			// http://tools.ietf.org/html/rfc4492#section-5.4
			writer.write(NAMED_CURVE, CURVE_TYPE_BITS);
			writer.write(curveId, NAMED_CURVE_BITS);
			int length = pointEncoded.length;
			writer.write(length, PUBLIC_LENGTH_BITS);
			writer.writeBytes(pointEncoded);

			// signature
			if (signatureEncoded != null) {
				length = signatureEncoded.length;
				writer.write(length, SIGNATURE_LENGTH_BITS);
				writer.writeBytes(signatureEncoded);
			}
			break;

		default:
			LOG.severe("Unknown curve type: " + curveId);
			break;
		}

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		int curveType = reader.read(CURVE_TYPE_BITS);
		switch (curveType) {
		case EXPLICIT_PRIME:

			break;
		case EXPLICIT_CHAR2:

			break;
		case NAMED_CURVE:
			int curveId = reader.read(NAMED_CURVE_BITS);
			int length = reader.read(PUBLIC_LENGTH_BITS);
			byte[] pointEncoded = reader.readBytes(length);

			byte[] bytesLeft = reader.readBytesLeft();
			byte[] signatureEncoded = null;
			if (bytesLeft.length > 0) {
				reader = new DatagramReader(bytesLeft);
				length = reader.read(SIGNATURE_LENGTH_BITS);
				signatureEncoded = reader.readBytes(length);
			}

			return new ECDHServerKeyExchange(curveId, pointEncoded, signatureEncoded);

		default:
			LOG.severe("Unknown curve type: " + curveType);
			break;
		}

		return null;
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public int getMessageLength() {
		// the signature length field uses 2 bytes, if a signature available
		int signatureLength = (signatureEncoded == null) ? 0 : 2 + signatureEncoded.length;
		// TODO this is only true for curve_type == namedcurve
		return 4 + pointEncoded.length + signatureLength;
	}

	/**
	 * Called by the client after receiving the server's
	 * {@link ServerKeyExchange} message. Verifies the contained signature.
	 * 
	 * @param serverPublicKey
	 *            the server's public key.
	 * @param clientRandom
	 *            the client's random (used in signature).
	 * @param serverRandom
	 *            the server's random (used in signature).
	 * @throws HandshakeException
	 *             if the signature could not be verified.
	 */
	public void verifySignature(PublicKey serverPublicKey, Random clientRandom, Random serverRandom) throws HandshakeException {
		if (signatureEncoded == null) {
			// no signature available, nothing to verify
			return;
		}
		boolean verified = false;
		try {
			Signature signature = Signature.getInstance(SIGNATURE_INSTANCE);
			signature.initVerify(serverPublicKey);

			updateSignature(signature, clientRandom, serverRandom);

			verified = signature.verify(signatureEncoded);

		} catch (Exception e) {
			LOG.severe("Could not verify the server's signature.");
			e.printStackTrace();
		}
		
		if (!verified) {
			String message = "The server's ECDHE key exchange message's signature could not be verified.";
			AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
			throw new HandshakeException(message, alert);
		}
	}

	/**
	 * Update the signature: SHA(ClientHello.random + ServerHello.random +
	 * ServerKeyExchange.params). See <a
	 * href="http://tools.ietf.org/html/rfc4492#section-5.4">RFC 4492, Section
	 * 5.4. Server Key Exchange</a> for further details on the signature format.
	 * 
	 * @param signature
	 *            the signature
	 * @param clientRandom
	 *            the client random
	 * @param serverRandom
	 *            the server random
	 * @throws SignatureException
	 *             the signature exception
	 */
	private void updateSignature(Signature signature, Random clientRandom, Random serverRandom) throws SignatureException {
		signature.update(clientRandom.getRandomBytes());
		signature.update(serverRandom.getRandomBytes());

		switch (curveType) {
		case EXPLICIT_PRIME:

			break;

		case EXPLICIT_CHAR2:

			break;

		case NAMED_CURVE:
			signature.update((byte) NAMED_CURVE);
			signature.update((byte) (curveId >> 8));
			signature.update((byte) curveId);
			signature.update((byte) pointEncoded.length);
			signature.update(pointEncoded);
			break;

		default:
			LOG.severe("Unknown curve type: " + curveId);
			break;
		}
	}

	/**
	 * Called by the client after receiving the {@link ServerKeyExchange}
	 * message and verification.
	 * 
	 * @return the server's ephemeral public key.
	 */
	public ECPublicKey getPublicKey(ECParameterSpec params) {
		if (publicKey == null) {
			// TODO assumption: the server's public key (from certificate) lies
			// on the same elliptic curve as its ephemeral public key
			
			try {
				point = ECDHECryptography.decodePoint(pointEncoded, params.getCurve());

				KeyFactory keyFactory = KeyFactory.getInstance(KEYPAIR_GENERATOR_INSTANCE);
				publicKey = (ECPublicKey) keyFactory.generatePublic(new ECPublicKeySpec(point, params));
			} catch (Exception e) {
				LOG.severe("Could not reconstruct the server's ephemeral public key.");
				e.printStackTrace();
			}

		}
		return publicKey;
	}
	
	private ECPublicKey getPublicKey() {
		return publicKey;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\t" + getPublicKey().toString() + "\n");

		return sb.toString();
	}

	/**
	 * Maps the the named curves indices to their names.
	 * 
	 * See <a href="http://tools.ietf.org/html/rfc4492#section-5.1.1">RFC 4492,
	 * Section 5.1.1 Supported Elliptic Curves Extension</a>
	 */
	private final static String[] NAMED_CURVE_TABLE = new String[] { null, // 0
			"sect163k1", // 1
			"sect163r1", // 2
			"sect163r2", // 3
			"sect193r1", // 4
			"sect193r2", // 5
			"sect233k1", // 6
			"sect233r1", // 7
			"sect239k1", // 8
			"sect283k1", // 9
			"sect283r1", // 10
			"sect409k1", // 11
			"sect409r1", // 12
			"sect571k1", // 13
			"sect571r1", // 14
			"secp160k1", // 15
			"secp160r1", // 16
			"secp160r2", // 17
			"secp192k1", // 18
			"secp192r1", // 19
			"secp224k1", // 20
			"secp224r1", // 21
			"secp256k1", // 22
			"secp256r1", // 23
			"secp384r1", // 24
			"secp521r1" // 25
	};

	/**
	 * Maps the named curves names to its indices. This is done statically
	 * according to the named curve table.
	 */
	private final static Map<String, Integer> NAMED_CURVE_INDEX;

	static {
		NAMED_CURVE_INDEX = new HashMap<String, Integer>();
		for (int i = 1; i < NAMED_CURVE_TABLE.length; i++) {
			NAMED_CURVE_INDEX.put(NAMED_CURVE_TABLE[i], i);
		}
	}

}

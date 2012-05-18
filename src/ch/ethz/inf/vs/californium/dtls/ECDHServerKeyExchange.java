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

import sun.security.ec.ECParameters;
import sun.security.ec.NamedCurve;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

public class ECDHServerKeyExchange extends ServerKeyExchange {

	private static final int CURVE_TYPE_BITS = 8;
	private static final int NAMED_CURVE_BITS = 16;
	private static final int PUBLIC_LENGTH_BITS = 8;
	private static final int SIGNATURE_LENGTH_BITS = 16;

	private static final String SIGNATURE_INSTANCE = "SHA1withECDSA";
	private static final String KEYPAIR_GENERATOR_INSTANCE = "EC";
	private static final String SPEC_PARAMETER = "secp192k1";

	/** The ECCurveType */
	// parameters are conveyed verbosely; underlying finite field is a prime
	// field
	private static final int EXPLICIT_PRIME = 1;
	// parameters are conveyed verbosely; underlying finite field is a
	// characteristic-2 field
	private static final int EXPLICIT_CHAR2 = 2;
	// a named curve is used
	private static final int NAMED_CURVE = 3;

	/** ephemeral keys */
	private ECPublicKey publicKey = null;

	ECPoint point = null;
	byte[] pointEncoded = null;

	// TODO
	int curveId = 24; // secp192k1

	byte[] signatureEncoded = null;

	/**
	 * Called by {@link ServerHandshaker}, generates ephemeral keys and
	 * signature.
	 * 
	 * @param serverPrivateKey
	 * @param clientRandom
	 * @param serverRandom
	 */
	public ECDHServerKeyExchange(ECDHECryptography ecdhe, PrivateKey serverPrivateKey, Random clientRandom, Random serverRandom) {

		try {
			publicKey = ecdhe.getPublicKey();

			// create public point
			ECParameterSpec parameters = publicKey.getParams();
			point = publicKey.getW();
			pointEncoded = ECParameters.encodePoint(point, parameters.getCurve());

			// make signature
			Signature signature = Signature.getInstance(SIGNATURE_INSTANCE);
			signature.initSign(serverPrivateKey);

			updateSignature(signature, clientRandom, serverRandom);

			signatureEncoded = signature.sign();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ECDHServerKeyExchange(int curveId, byte[] pointEncoded, byte[] signatureEncoded) {
		this.curveId = curveId;
		this.pointEncoded = pointEncoded;
		this.signatureEncoded = signatureEncoded;
	}

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		// TODO only valid for curve type NAMED_CURVE
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

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		int curveType = reader.read(CURVE_TYPE_BITS);
		switch (curveType) {
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
			break;
		}

		return null;
	}

	@Override
	public int getMessageLength() {
		int signatureLength = (signatureEncoded == null) ? 0 : 2 + signatureEncoded.length;
		// TODO this is only true for curve_type == namedcurve
		return 4 + pointEncoded.length + signatureLength;
	}

	public boolean verifySignature(PublicKey serverPublicKey, Random clientRandom, Random serverRandom) {
		if (signatureEncoded == null) {
			// no signature available
			return true;
		}
		boolean verified = false;
		try {
			Signature signature = Signature.getInstance(SIGNATURE_INSTANCE);
			signature.initVerify(serverPublicKey);

			updateSignature(signature, clientRandom, serverRandom);

			verified = signature.verify(signatureEncoded);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return verified;
	}

	/**
	 * Update the signature: SHA(ClientHello.random + ServerHello.random +
	 * ServerKeyExchange.params)
	 * 
	 * @param signature
	 * @param clientRandom
	 * @param serverRandom
	 * @throws SignatureException
	 */
	private void updateSignature(Signature signature, Random clientRandom, Random serverRandom) throws SignatureException {
		signature.update(clientRandom.getRandomBytes());
		signature.update(serverRandom.getRandomBytes());
		signature.update((byte) NAMED_CURVE);
		signature.update((byte) (curveId >> 8));
		signature.update((byte) curveId);
		signature.update((byte) pointEncoded.length);
		signature.update(pointEncoded);
	}

	public ECPublicKey getPublicKey() {
		if (publicKey == null) {
			// client case: reconstruct public key
			// TODO make this variable
			ECParameterSpec params = NamedCurve.getECParameterSpec(SPEC_PARAMETER);
			try {
				point = ECParameters.decodePoint(pointEncoded, params.getCurve());

				KeyFactory keyFactory = KeyFactory.getInstance(KEYPAIR_GENERATOR_INSTANCE);
				publicKey = (ECPublicKey) keyFactory.generatePublic(new ECPublicKeySpec(point, params));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return publicKey;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\t" + getPublicKey().toString() + "\n");

		return sb.toString();
	}
	
	

}

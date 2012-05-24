package ch.ethz.inf.vs.californium.dtls;

import java.security.GeneralSecurityException;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

import sun.security.ec.ECParameters;

/**
 * A helper class to execute the ECDHE key agreement and key generation.
 * 
 * @author Stefan Jucker
 * 
 */
public class ECDHECryptography {
	private static final String KEYPAIR_GENERATOR_INSTANCE = "EC";
	private static final String KEY_AGREEMENT_INSTANCE = "ECDH";

	private PrivateKey privateKey;

	private ECPublicKey publicKey;

	/**
	 * Called by Server, create ephemeral key ECDH keypair.
	 */
	public ECDHECryptography(PrivateKey privateKey2) {
		// create ephemeral key pair
		try {
			// get the curve name by the parameters of the private key
			ECParameterSpec parameters = ((ECPrivateKey) privateKey2).getParams();
			String namedCurve = parameters.toString(); // like this: secp192k1 (1.3.132.0.31)
			namedCurve = namedCurve.substring(0, 9); // we only need secp192k1
			
			KeyPairGenerator kpg;
			kpg = KeyPairGenerator.getInstance(KEYPAIR_GENERATOR_INSTANCE);

			ECGenParameterSpec params = new ECGenParameterSpec(namedCurve);

			kpg.initialize(params, new SecureRandom());

			KeyPair kp = kpg.generateKeyPair();

			privateKey = kp.getPrivate();
			publicKey = (ECPublicKey) kp.getPublic();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Called by client, with parameters provided by server.
	 * 
	 * @param params
	 */
	public ECDHECryptography(ECParameterSpec params) {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEYPAIR_GENERATOR_INSTANCE);
			keyPairGenerator.initialize(params, new SecureRandom());

			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			privateKey = keyPair.getPrivate();
			publicKey = (ECPublicKey) keyPair.getPublic();

		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public ECPublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(ECPublicKey publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Called by the server.
	 * 
	 * @param encodedPoint
	 *            the client's public key (encoded)
	 */
	public SecretKey getSecret(byte[] encodedPoint) {
		SecretKey secretKey = null;
		try {
			// extract public key
			ECParameterSpec params = publicKey.getParams();
			ECPoint point = ECParameters.decodePoint(encodedPoint, params.getCurve());
			KeyFactory keyFactory = KeyFactory.getInstance(KEYPAIR_GENERATOR_INSTANCE);
			ECPublicKeySpec keySpec = new ECPublicKeySpec(point, params);
			PublicKey peerPublicKey = keyFactory.generatePublic(keySpec);
			secretKey = getSecret(peerPublicKey);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return secretKey;
	}

	/**
	 * Called by client.
	 * 
	 * @param peerPublicKey
	 * @return
	 */
	public SecretKey getSecret(PublicKey peerPublicKey) {
		SecretKey secretKey = null;
		try {
			KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_INSTANCE);
			keyAgreement.init(privateKey);
			keyAgreement.doPhase(peerPublicKey, true);

			secretKey = keyAgreement.generateSecret("TlsPremasterSecret");
		} catch (Exception e) {
			// TODO: handle exception
		}
		return secretKey;
	}

}

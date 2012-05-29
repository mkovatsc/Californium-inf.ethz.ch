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
import java.util.logging.Logger;

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

	// Logging ////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(ECDHECryptography.class.getName());

	// Static members /////////////////////////////////////////////////

	/**
	 * The algorithm for the elliptic curve keypair generation. See also <a
	 * href=
	 * "http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#KeyPairGenerator"
	 * >KeyPairGenerator Algorithms</a>.
	 */
	private static final String KEYPAIR_GENERATOR_INSTANCE = "EC";

	/**
	 * Elliptic Curve Diffie-Hellman algorithm name. See also <a href=
	 * "http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#KeyAgreement"
	 * >KeyAgreement Algorithms</a>.
	 */
	private static final String KEY_AGREEMENT_INSTANCE = "ECDH";

	// Members ////////////////////////////////////////////////////////

	private PrivateKey privateKey;

	private ECPublicKey publicKey;

	// Constructors ///////////////////////////////////////////////////

	/**
	 * Called by Server, create ephemeral key ECDH keypair.
	 * 
	 * @param key
	 *            the server's private key.
	 */
	public ECDHECryptography(PrivateKey key) {
		// create ephemeral key pair
		try {
			// get the curve name by the parameters of the private key
			ECParameterSpec parameters = ((ECPrivateKey) key).getParams();

			// namedCurve will look like this: secp192k1 (1.3.132.0.31)
			String namedCurve = parameters.toString();
			// we only need secp192k1 the
			namedCurve = namedCurve.substring(0, 9);

			// initialize the key pair generator
			KeyPairGenerator kpg;
			kpg = KeyPairGenerator.getInstance(KEYPAIR_GENERATOR_INSTANCE);
			ECGenParameterSpec params = new ECGenParameterSpec(namedCurve);
			kpg.initialize(params, new SecureRandom());

			KeyPair kp = kpg.generateKeyPair();

			privateKey = kp.getPrivate();
			publicKey = (ECPublicKey) kp.getPublic();
		} catch (GeneralSecurityException e) {
			LOG.severe("Could not generate the ECDHE keypair.");
			e.printStackTrace();
		}

	}

	/**
	 * Called by client, with parameters provided by server.
	 * 
	 * @param params
	 *            the parameters provided by the server's ephemeral public key.
	 */
	public ECDHECryptography(ECParameterSpec params) {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEYPAIR_GENERATOR_INSTANCE);
			keyPairGenerator.initialize(params, new SecureRandom());

			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			privateKey = keyPair.getPrivate();
			publicKey = (ECPublicKey) keyPair.getPublic();

		} catch (GeneralSecurityException e) {
			LOG.severe("Could not generate the ECDHE keypair.");
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
	 * Called by the server. Extracts the client's public key from the encoded
	 * point and then runs the specified key agreement algorithm (ECDH) to
	 * generate the premaster secret.
	 * 
	 * @param encodedPoint
	 *            the client's public key (encoded)
	 * @return the premaster secret
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
			LOG.severe("Could not generate the premaster secret.");
			e.printStackTrace();
		}
		return secretKey;
	}

	/**
	 * Runs the specified key agreement algorithm (ECDH) to generate the
	 * premaster secret.
	 * 
	 * @param peerPublicKey
	 *            the peer's ephemeral public key.
	 * @return the premaster secret.
	 */
	public SecretKey getSecret(PublicKey peerPublicKey) {
		SecretKey secretKey = null;
		try {
			KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_INSTANCE);
			keyAgreement.init(privateKey);
			keyAgreement.doPhase(peerPublicKey, true);

			secretKey = keyAgreement.generateSecret("TlsPremasterSecret"); // TODO
		} catch (Exception e) {

			e.printStackTrace();
		}
		return secretKey;
	}

}

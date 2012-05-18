package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

public enum CipherSuite {

	SSL_NULL_WITH_NULL_NULL("SSL_NULL_WITH_NULL_NULL", 0x0000, KeyExchangeAlgorithm.NULL, BulkCipherAlgorithm.NULL, MACAlgorithm.NULL, PRFAlgorithm.NULL, CipherType.NULL),
	TLS_PSK_WITH_AES_128_CCM_8("TLS_PSK_WITH_AES_128_CCM_8", 0x0001, KeyExchangeAlgorithm.PSK, BulkCipherAlgorithm.AES, MACAlgorithm.NULL, PRFAlgorithm.TLS_PRF_SHA256, CipherType.AEAD),
	TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8("TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8", 0x0002, KeyExchangeAlgorithm.EC_DIFFIE_HELLMAN, BulkCipherAlgorithm.AES, MACAlgorithm.NULL, PRFAlgorithm.TLS_PRF_SHA256, CipherType.AEAD);

	private static final int CIPHER_SUITE_BITS = 16;
	
	private String name;
	
	/** 16 bit identification, i.e. 0x0000 for SSL_NULL_WITH_NULL_NULL */
	private int code;

	private KeyExchangeAlgorithm keyExchange;
	private BulkCipherAlgorithm bulkCipher;
	private MACAlgorithm macAlgorithm;
	private PRFAlgorithm pseudoRandomFunction;
	private CipherType cipherType;

	private CipherSuite(String name, int code, KeyExchangeAlgorithm keyExchange, BulkCipherAlgorithm bulkCipher, MACAlgorithm macAlgorithm, PRFAlgorithm prf, CipherType cipherType) {
		this.name = name;
		this.code = code;
		this.keyExchange = keyExchange;
		this.bulkCipher = bulkCipher;
		this.macAlgorithm = macAlgorithm;
		this.pseudoRandomFunction = prf;
		this.cipherType = cipherType;
	}

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

	public static CipherSuite getTypeByCode(int code) {
		switch (code) {
		case 0x0000:
			return CipherSuite.SSL_NULL_WITH_NULL_NULL;
		case 0x0001:
			return CipherSuite.TLS_PSK_WITH_AES_128_CCM_8;
		case 0x002:
			return CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;

		default:
			return null;
		}
	}

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

	public enum MACAlgorithm {
		NULL, HMAC_MD5, HMAC_SHA1, HMAC_SHA256, HMAC_SHA384, HMAC_SHA512;
	}

	public enum BulkCipherAlgorithm {
		NULL, RC4, B_3DES, AES;
		
		// TODO keysize, etc.
	}

	public enum KeyExchangeAlgorithm {
		NULL, DHE_DSS, DHE_RSA, DH_ANON, RSA, DH_DSS, DH_RSA, PSK, EC_DIFFIE_HELLMAN;
	}
	
	public enum PRFAlgorithm {
		NULL, TLS_PRF_SHA256;
	}
	
	public enum CipherType {
		NULL, STREAM, BLOCK, AEAD;
	}

}

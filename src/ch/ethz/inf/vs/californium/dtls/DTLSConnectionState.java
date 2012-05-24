package ch.ethz.inf.vs.californium.dtls;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * 
 * @author Stefan Jucker
 * 
 */
public class DTLSConnectionState {
	private CipherSuite cipherSuite;
	private CompressionMethod compressionMethod;
	private SecretKey encryptionKey;
	private IvParameterSpec iv;
	private SecretKey macKey;
	
	/**
	 * Constructor for the initial state.
	 */
	public DTLSConnectionState() {
		this.cipherSuite = CipherSuite.SSL_NULL_WITH_NULL_NULL;
		this.compressionMethod = CompressionMethod.NULL;
		this.encryptionKey = null;
		this.iv = null;
		this.macKey = null;
	}
	
	/**
	 * 
	 * @param cipherSuite
	 * @param compressionMethod
	 * @param encryptionKey
	 * @param iv
	 * @param macKey
	 */
	public DTLSConnectionState(CipherSuite cipherSuite, CompressionMethod compressionMethod, SecretKey encryptionKey, IvParameterSpec iv, SecretKey macKey) {
		this.cipherSuite = cipherSuite;
		this.compressionMethod = compressionMethod;
		this.encryptionKey = encryptionKey;
		this.iv = iv;
		this.macKey = macKey;
	}

	public CipherSuite getCipherSuite() {
		return cipherSuite;
	}

	public void setCipherSuite(CipherSuite cipherSuite) {
		this.cipherSuite = cipherSuite;
	}

	public CompressionMethod getCompressionMethod() {
		return compressionMethod;
	}

	public void setCompressionMethod(CompressionMethod compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	public SecretKey getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(SecretKey encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public IvParameterSpec getIv() {
		return iv;
	}

	public void setIv(IvParameterSpec iv) {
		this.iv = iv;
	}

	public SecretKey getMacKey() {
		return macKey;
	}

	public void setMacKey(SecretKey macKey) {
		this.macKey = macKey;
	}
}

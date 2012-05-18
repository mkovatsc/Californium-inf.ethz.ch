package ch.ethz.inf.vs.californium.dtls;

import java.security.cert.X509Certificate;

import javax.crypto.SecretKey;

/**
 * 
 * @author Stefan Jucker
 * 
 */
public class DTLSSession {
	/**
	 * An arbitrary byte sequence chosen by the server to identify an active or
	 * resumable session state.
	 */
	private SessionId sessionIdentifier;

	/** X509v3 certificate of the peer. This element of the state may be null. */
	private X509Certificate peerCertificate;

	/** The algorithm used to compress data prior to encryption. */
	private CompressionMethod compressionMethod;

	/**
	 * Specifies the pseudorandom function (PRF) used to generate keying
	 * material, the bulk data encryption algorithm (such as null, AES, etc.)
	 * and the MAC algorithm (such as HMAC-SHA1). It also defines cryptographic
	 * attributes such as the mac_length. (See Appendix A.6 for formal
	 * definition.)
	 */
	private CipherSuite cipherSuite;

	/** 48-byte secret shared between the client and server. */
	private SecretKey masterSecret;

	/**
	 * A flag indicating whether the session can be used to initiate new
	 * connections.
	 */
	private boolean isResumable;
	
	public DTLSSession(SessionId sessionId, X509Certificate peerCertificate, CompressionMethod compressionMethod, CipherSuite cipherSuite, SecretKey masterSecret, boolean isResumable) {
		this.sessionIdentifier = sessionId;
		this.peerCertificate = peerCertificate;
		this.compressionMethod = compressionMethod;
		this.cipherSuite = cipherSuite;
		this.masterSecret = masterSecret;
		this.isResumable = isResumable;
	}

	public SessionId getSessionIdentifier() {
		return sessionIdentifier;
	}

	public void setSessionIdentifier(SessionId sessionIdentifier) {
		this.sessionIdentifier = sessionIdentifier;
	}

	public X509Certificate getPeerCertificate() {
		return peerCertificate;
	}

	public void setPeerCertificate(X509Certificate peerCertificate) {
		this.peerCertificate = peerCertificate;
	}

	public CompressionMethod getCompressionMethod() {
		return compressionMethod;
	}

	public void setCompressionMethod(CompressionMethod compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	public CipherSuite getCipherSuite() {
		return cipherSuite;
	}

	public void setCipherSuite(CipherSuite cipherSuite) {
		this.cipherSuite = cipherSuite;
	}

	public SecretKey getMasterSecret() {
		return masterSecret;
	}

	public void setMasterSecret(SecretKey masterSecret) {
		this.masterSecret = masterSecret;
	}

	public boolean isResumable() {
		return isResumable;
	}

	public void setResumable(boolean isResumable) {
		this.isResumable = isResumable;
	}

}

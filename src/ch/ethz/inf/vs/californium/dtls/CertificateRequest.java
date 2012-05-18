package ch.ethz.inf.vs.californium.dtls;

import java.util.List;

/**
 * A non-anonymous server can optionally request a certificate from the client,
 * if appropriate for the selected cipher suite. This message, if sent, will
 * immediately follow the {@link ServerKeyExchange} message (if it is sent;
 * otherwise, this message follows the server's {@link CertificateMessage}
 * message).
 * 
 * @author Stefan Jucker
 * 
 */
public class CertificateRequest extends HandshakeMessage {

	/** A list of the types of certificate types that the client may offer. */
	// ClientCertificateType certificate_types<1..2^8-1>;
	private List<ClientCertificateType> certificateTypes;

	/**
	 * A list of the hash/signature algorithm pairs that the server is able to
	 * verify, listed in descending order of preference.
	 */
	// SignatureAndHashAlgorithm supported_signature_algorithms<2^16-1>;
	private List<SignatureAndHashAlgorithm> supportedSignatureAlgorithms;

	// TODO DistinguishedName certificate_authorities<0..2^16-1>;

	@Override
	public HandshakeType getMessageType() {
		// TODO Auto-generated method stub
		return HandshakeType.CERTIFICATE_REQUEST;
	}

	@Override
	public int getMessageLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	public enum ClientCertificateType {
		RSA_SIGN(1), DSS_SIGN(2), RSA_FIXED_DH(3), DSS_FIXED_DH(4), RSA_EPHEMERAL_DH_RESERVED(5), DSS_EPHEMERAL_DH_RESERVED(6), FORTEZZA_DMS_RESERVED(20),
		ECDSA_SIGN(64), RSA_FIXED_ECDH(65), ECDSA_FIXED_ECDH(66);

		private int code;

		private ClientCertificateType(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}
	}

	public enum HashAlgorithm {
		NONE(0), MD5(1), SHA1(2), SHA224(3), SHA256(4), SHA384(5), SHA512(6);

		private int code;

		private HashAlgorithm(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}
	}

	public enum SignatureAlgorithm {
		ANONYMOUS(0), RSA(1), DSA(2), ECDSA(3);

		private int code;

		private SignatureAlgorithm(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}
	}

	public class SignatureAndHashAlgorithm {
		private HashAlgorithm hash;

		private SignatureAlgorithm signature;

		public SignatureAndHashAlgorithm() {
			// TODO Auto-generated constructor stub
		}

		public SignatureAndHashAlgorithm(HashAlgorithm hashAlgorithm, SignatureAlgorithm signatureAlgorithm) {
			this.signature = signatureAlgorithm;
			this.hash = hashAlgorithm;
		}

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
	}

	public List<ClientCertificateType> getCertificateTypes() {
		return certificateTypes;
	}

	public List<SignatureAndHashAlgorithm> getSupportedSignatureAlgorithms() {
		return supportedSignatureAlgorithms;
	}

}

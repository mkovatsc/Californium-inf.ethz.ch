
package ch.ethz.inf.vs.californium.dtls;

import ch.ethz.inf.vs.californium.dtls.HelloExtensions.ExtensionType;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * The cert-send extension indicates the certificate format found in the
 * Certificate payload itself. See <a
 * href="http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-04#section-4.2"
 * >TLS Handshake Extension</a> for details.
 * 
 * @author Stefan Jucker
 * 
 */
public class CertSendExtension extends HelloExtension {

	// DTLS-specific constants ////////////////////////////////////////

	private static final int CERT_TYPE_BITS = 8;

	// Members ////////////////////////////////////////////////////////

	/** The type of the following Certificate message. */
	CertType certType;

	// Constructors ///////////////////////////////////////////////////

	public CertSendExtension(CertType certType) {
		super(ExtensionType.CERT_SEND);
		this.certType = certType;
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		writer.write(1, LENGTH_BITS);
		writer.write(certType.getCode(), CERT_TYPE_BITS);

		return writer.toByteArray();
	}

	public static HelloExtension fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		CertType type = CertType.getTypeFromCode(reader.read(CERT_TYPE_BITS));

		return new CertSendExtension(type);
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public int getLength() {
		// fixed: extension type (2 bytes) + extension length field (2 bytes) +
		// certificate type (1 byte)
		return 5;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\t\t\t\tCert-Send: " + certType.toString() + "\n");

		return sb.toString();
	}

	// Enum ///////////////////////////////////////////////////////////

	/**
	 * Represents the possible certificate types defined <a href=
	 * "http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-04#section-3"
	 * >here</a>. The value will not be greater than 255, therefore 1 byte
	 * suffices to encode it.
	 * 
	 * @author Stefan Jucker
	 * 
	 */
	public enum CertType {
		X_509(0), RAW_PUBLIC_KEY(1);

		private int code;

		private CertType(int code) {
			this.code = code;
		}

		public static CertType getTypeFromCode(int code) {
			switch (code) {
			case 0:
				return X_509;
			case 1:
				return RAW_PUBLIC_KEY;

			default:
				return null;
			}
		}

		int getCode() {
			return code;
		}
	}

}


package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.dtls.CertSendExtension.CertType;
import ch.ethz.inf.vs.californium.dtls.HelloExtensions.ExtensionType;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * the cert-receive extension indicates the client's ability to process certain
 * certificate types when receiving the server's {@link CertificateMessage}. See
 * <a
 * href="http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-04#section-4.1">
 * TLS Handshake Extension</a> for details.
 * 
 * @author Stefan Jucker
 * 
 */
public class CertReceiveExtension extends HelloExtension {

	// DTLS-specific constants ////////////////////////////////////////

	private static final int LIST_LENGTH_BITS = 8;

	private static final int CERT_TYPE_BITS = 8;

	// Members ////////////////////////////////////////////////////////

	/**
	 * The supported types of certificates the peer is allowed to send. Ordered
	 * by preference.
	 */
	List<CertType> certTypes;

	// Constructors ///////////////////////////////////////////////////

	public CertReceiveExtension(List<CertType> certTypes) {
		super(ExtensionType.CERT_RECEIVE);
		this.certTypes = certTypes;
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		int listLength = certTypes.size();
		// list length + list length field (1 byte)
		writer.write(listLength + 1, LENGTH_BITS);
		writer.write(listLength, LIST_LENGTH_BITS);

		for (CertType type : certTypes) {
			writer.write(type.getCode(), CERT_TYPE_BITS);
		}

		return writer.toByteArray();
	}

	public static HelloExtension fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		int listLength = reader.read(LIST_LENGTH_BITS);

		List<CertType> certTypes = new ArrayList<CertType>();
		while (listLength > 0) {
			certTypes.add(CertType.getTypeFromCode((reader.read(CERT_TYPE_BITS))));

			// one cert type uses 1 byte
			listLength -= 1;
		}

		return new CertReceiveExtension(certTypes);
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public int getLength() {
		// fixed: extension type (2 bytes) + extension length field (2 bytes) +
		// list length field (1 byte)
		return 5 + certTypes.size();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\t\t\t\tCert-Receive:\n");
		for (CertType type : certTypes) {
			sb.append("\t\t\t\t\t" + type.toString() + "\n");
		}

		return sb.toString();
	}

}

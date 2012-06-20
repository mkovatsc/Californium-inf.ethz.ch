package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.dtls.HelloExtensions.ExtensionType;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * This represents the Certificate Type Extension. See <a
 * href="http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-03">Draft</a> for
 * details.
 * 
 * @author Stefan Jucker
 * 
 */
public class CertificateTypeExtension extends HelloExtension {

	// DTLS-specific constants ////////////////////////////////////////
	
	private static final int LENGTH_BITS = 16;

	private static final int LIST_FIELD_LENGTH_BITS = 8;
	
	private static final int EXTENSION_TYPE_BITS = 8;

	// Members ////////////////////////////////////////////////////////

	/**
	 * Indicates whether this extension belongs to a client or a server. This
	 * has an impact upon the message format. See <a href=
	 * "http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-03#section-3.1"
	 * >CertificateTypeExtension</a> definition.
	 */
	private boolean isClientExtension;

	/**
	 * For the client: a list of certificate types the client supports, sorted
	 * by client preference.<br />
	 * For the server: the certificate selected by the server out of the
	 * client's list.
	 */
	private List<CertificateType> certificateTypes;

	// Constructors ///////////////////////////////////////////////////

	public CertificateTypeExtension(boolean isClient) {
		super(ExtensionType.CERT_TYPE);
		this.isClientExtension = isClient;
		this.certificateTypes = new ArrayList<CertificateType>();
	}
	
	public CertificateTypeExtension(List<CertificateType> certificateTypes, boolean isClient) {
		super(ExtensionType.CERT_TYPE);
		this.isClientExtension = isClient;
		this.certificateTypes = certificateTypes;
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public int getLength() {
		if (isClientExtension) {
			// fixed:  type (2 bytes), length (2 bytes), the list length field (1 byte)
			// each certificate type in the list uses 1 byte
			return 5 + certificateTypes.size();
		} else {
			//  type (2 bytes), length (2 bytes), the certificate type (1 byte)
			return 5;
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());

		for (CertificateType type : certificateTypes) {
			sb.append("\t\t\t\tCertificate type: " + type.toString() + "\n");
		}

		return sb.toString();
	};

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());
		
		if (isClientExtension) {
			int listLength = certificateTypes.size();			
			writer.write(listLength + 1, LENGTH_BITS);
			writer.write(listLength, LIST_FIELD_LENGTH_BITS);
			for (CertificateType type : certificateTypes) {
				writer.write(type.getCode(), EXTENSION_TYPE_BITS);
			}
		} else {
			// we assume the list contains exactly one element
			writer.write(1, LENGTH_BITS);
			writer.write(certificateTypes.get(0).getCode(), EXTENSION_TYPE_BITS);
		}
		
		return writer.toByteArray();
	}
	
	public static HelloExtension fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		
		List<CertificateType> certificateTypes = new ArrayList<CertificateType>();
		
		// the client's extension needs at least 2 bytes, while the server's is exactly 1 byte long
		boolean isClientExtension = true;
		if (byteArray.length > 1) {
			int length = reader.read(LIST_FIELD_LENGTH_BITS);
			for (int i = 0; i < length; i++) {
				certificateTypes.add(CertificateType.getTypeFromCode(reader.read(EXTENSION_TYPE_BITS)));
			}
		} else {
			certificateTypes.add(CertificateType.getTypeFromCode(reader.read(EXTENSION_TYPE_BITS)));
			isClientExtension = false;
		}

		return new CertificateTypeExtension(certificateTypes, isClientExtension);
	}

	// Enums //////////////////////////////////////////////////////////

	/**
	 * See <a href=
	 * "http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-03#section-3.1"
	 * >3.1. ClientHello</a> for the definition. Note: The RawPublicKey code is
	 * <tt>TBD</tt>, but we assumed for now the reasonable value 2.
	 * 
	 * @author Stefan Jucker
	 * 
	 */
	public enum CertificateType {
		X_509(0), OPEN_PGP(1), RAW_PUBLIC_KEY(2);

		private int code;

		private CertificateType(int code) {
			this.code = code;
		}
		
		public static CertificateType getTypeFromCode(int code) {
			switch (code) {
			case 0:
				return X_509;
			case 1:
				return OPEN_PGP;
			case 2:
				return RAW_PUBLIC_KEY;

			default:
				return null;
			}
		}

		int getCode() {
			return code;
		}
	}
	
	// Getters and Setters ////////////////////////////////////////////
	
	public void addCertificateType(CertificateType certificateType) {
		this.certificateTypes.add(certificateType);
	}

}

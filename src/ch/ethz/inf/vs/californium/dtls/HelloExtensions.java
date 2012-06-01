package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * 
 * @author Stefan Jucker
 * 
 */
public class HelloExtensions {
	
	// Logging ///////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(HelloExtensions.class.getName());

	// DTLS-specific constants ////////////////////////////////////////

	private static final int LENGTH_BITS = 16;

	private static final int EXTENSION_LENGTH_BITS = 16;

	private static final int TYPE_BITS = 16;

	// Members ////////////////////////////////////////////////////////

	private List<HelloExtension> extensions;

	// Constructors ///////////////////////////////////////////////////

	public HelloExtensions() {
		this.extensions = new ArrayList<>();
	}

	public HelloExtensions(List<HelloExtension> extensions) {
		this.extensions = extensions;
	}

	/**
	 * 
	 * @return the length of the whole extension fragment.
	 */
	public int getLength() {
		int length = 0;
		for (HelloExtension extension : extensions) {
			length += extension.getLength();
		}

		return length;
	}

	// Serialization //////////////////////////////////////////////////

	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();

		writer.write(getLength(), LENGTH_BITS);
		for (HelloExtension extension : extensions) {
			writer.writeBytes(extension.toByteArray());
		}

		return writer.toByteArray();
	}

	public static HelloExtensions fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		List<HelloExtension> extensions = new ArrayList<HelloExtension>();

		int length = reader.read(LENGTH_BITS);

		while (length > 0) {

			ExtensionType type = ExtensionType.getExtensionTypeById(reader.read(TYPE_BITS));
			int extensionLength = reader.read(EXTENSION_LENGTH_BITS);

			HelloExtension helloExtension = HelloExtension.fromByteArray(reader.readBytes(extensionLength), type);
			extensions.add(helloExtension);

			// the extensions length + 2 bytes for type field and 2 bytes for
			// length field
			length -= extensionLength + 4;
		}

		return new HelloExtensions(extensions);
	}

	public void addExtension(HelloExtension extension) {
		this.extensions.add(extension);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\t\tExtensions Length: " + getLength() + "\n");
		for (HelloExtension ext : extensions) {
			sb.append(ext.toString());
		}
		return sb.toString();
	}

	public enum ExtensionType {
		/** See <a href="http://www.ietf.org/rfc/rfc3546">RFC 3546</a> */
		SERVER_NAME(0, "server_name"),
		MAX_FRAGMENT_LENGTH(1, "max_fragment_length"),
		CLIENT_CERTIFICATE_URL(2, "client_certificate_url"),
		TRUSTED_CA_KEYS(3, "trusted_ca_keys"),
		TRUNCATED_HMAC(4, "truncated_hmac"),
		STATUS_REQUEST(5, "status_request"),

		/** See <a href="http://tools.ietf.org/html/rfc4681">RFC 4681</a> */
		USER_MAPPING(6, "user_mapping"),

		/**
		 * See <a href="http://tools.ietf.org/html/rfc4492#section-5.1">RFC
		 * 4492</a>
		 */
		ELLIPTIC_CURVES(10, "elliptic_curves"),
		EC_POINT_FORMATS(11, "ec_point_formats");

		private int id;

		private String name;

		ExtensionType(int id, String name) {
			this.id = id;
			this.name = name;
		}

		/**
		 * @param id
		 *            the extension id.
		 * @return
		 */
		public static ExtensionType getExtensionTypeById(int id) {
			switch (id) {
			case 0:
				return ExtensionType.SERVER_NAME;
			case 1:
				return ExtensionType.MAX_FRAGMENT_LENGTH;
			case 2:
				return ExtensionType.CLIENT_CERTIFICATE_URL;
			case 3:
				return ExtensionType.TRUSTED_CA_KEYS;
			case 4:
				return ExtensionType.TRUNCATED_HMAC;
			case 5:
				return ExtensionType.STATUS_REQUEST;
			case 6:
				return ExtensionType.USER_MAPPING;
			case 10:
				return ExtensionType.ELLIPTIC_CURVES;
			case 11:
				return ExtensionType.EC_POINT_FORMATS;

			default:
				return null;
			}
		}

		@Override
		public String toString() {
			return name;
		}

		public int getId() {
			return id;
		}
	}
}

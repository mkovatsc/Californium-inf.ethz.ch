package ch.ethz.inf.vs.californium.dtls;

import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.dtls.HelloExtensions.ExtensionType;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * 
 * An abstract class representing the functionality for all possible defined
 * extensions. See <a
 * href="http://tools.ietf.org/html/rfc5246#section-7.4.1.4">RFC 5246</a> for
 * the extension format.
 * 
 * @author Stefan Jucker
 * 
 */
public abstract class HelloExtension {

	// Logging ///////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(HelloExtension.class.getName());

	// DTLS-specific constants ////////////////////////////////////////

	protected static final int TYPE_BITS = 16;

	// Members ////////////////////////////////////////////////////////

	protected ExtensionType type;

	// Constructors ///////////////////////////////////////////////////

	public HelloExtension(ExtensionType type) {
		this.type = type;
	}

	// Abstract methods ///////////////////////////////////////////////

	public abstract int getLength();

	// Serialization //////////////////////////////////////////////////

	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();

		writer.write(type.getId(), TYPE_BITS);

		return writer.toByteArray();
	}

	public static HelloExtension fromByteArray(byte[] byteArray, ExtensionType type) {

		switch (type) {
		case ELLIPTIC_CURVES:
			return SupportedEllipticCurvesExtension.fromByteArray(byteArray);

		default:
			LOG.severe("Unknown extension type received: " + type.toString());
			return null;
		}

	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\t\t\tExtension: " + type.toString() + "\n");

		return sb.toString();
	}
}
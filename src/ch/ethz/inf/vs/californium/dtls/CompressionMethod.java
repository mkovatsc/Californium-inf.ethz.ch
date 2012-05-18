package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

public enum CompressionMethod {
	NULL(0x00);

	private static final int COMPRESSION_METHOD_BITS = 8;

	private int code;

	private CompressionMethod(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static CompressionMethod getTypeByCode(int code) {
		switch (code) {
		case 0x00:
			return CompressionMethod.NULL;

		default:
			return null;
		}
	}

	/**
	 * Takes a list of compression methods and creates the representing byte stream.
	 *
	 * @param compressionMethods the list of the compression methods
	 * @return the corresponding byte array
	 */
	public static byte[] listToByteArray(List<CompressionMethod> compressionMethods) {

		DatagramWriter writer = new DatagramWriter();
		for (CompressionMethod compressionMethod : compressionMethods) {
			writer.write(compressionMethod.getCode(), COMPRESSION_METHOD_BITS);
		}

		return writer.toByteArray();
	}

	/**
	 * Takes a byte array and creates the representing list of compression methods.
	 * 
	 * @param byteArray the encoded compression methods as byte array
	 * @param numElements the number of compression methods represented in the byte array
	 * @return corresponding list of compression methods
	 */
	public static List<CompressionMethod> listFromByteArray(byte[] byteArray, int numElements) {
		List<CompressionMethod> compressionMethods = new ArrayList<CompressionMethod>();
		DatagramReader reader = new DatagramReader(byteArray);

		for (int i = 0; i < numElements; i++) {
			int code = reader.read(COMPRESSION_METHOD_BITS);
			compressionMethods.add(CompressionMethod.getTypeByCode(code));
		}
		return compressionMethods;
	}

}

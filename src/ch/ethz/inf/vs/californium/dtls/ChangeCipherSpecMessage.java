package ch.ethz.inf.vs.californium.dtls;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * The change cipher spec protocol exists to signal transitions in ciphering
 * strategies. The protocol consists of a single message, which is encrypted and
 * compressed under the current (not the pending) connection state. The
 * ChangeCipherSpec message is sent by both the client and the server to notify
 * the receiving party that subsequent records will be protected under the newly
 * negotiated CipherSpec and keys.
 * 
 * @author Stefan Jucker
 * 
 */
public class ChangeCipherSpecMessage implements DTLSMessage {

	private static final int CCS_BITS = 8;

	private CCSType CCSProtocolType;

	public ChangeCipherSpecMessage() {
		CCSProtocolType = CCSType.CHANGE_CIPHER_SPEC;
	}

	public enum CCSType {
		CHANGE_CIPHER_SPEC(1);

		private int code;

		private CCSType(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}
	}

	public CCSType getCCSProtocolType() {
		return CCSProtocolType;
	}

	@Override
	public int getLength() {
		// The message consists of a single byte of value 1.
		return 1;
	}

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.write(CCSProtocolType.getCode(), CCS_BITS);

		return writer.toByteArray();
	}

	public static DTLSMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		int code = reader.read(CCS_BITS);
		if (code == CCSType.CHANGE_CIPHER_SPEC.getCode()) {
			return new ChangeCipherSpecMessage();
		} else {
			// TODO error handling
			return null;
		}
	}

}

package ch.ethz.inf.vs.californium.dtls;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * This message is used to provide explicit verification of a client
 * certificate. This message is only sent following a client certificate that
 * has signing capability (i.e., all certificates except those containing fixed
 * Diffie-Hellman parameters). When sent, it MUST immediately follow the {@link ClientKeyExchange} message.
 * 
 * @author Stefan Jucker
 * 
 */
public class CertificateVerify extends HandshakeMessage {
	
	private static final int SIGNATURE_LENGTH_BITS = 2;
	
	private byte[] signature;
	
	public CertificateVerify(byte[] signature) {
		this.signature = signature;
	}
	
	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());
		
		writer.write(signature.length, SIGNATURE_LENGTH_BITS);
		writer.writeBytes(signature);
		
		return writer.toByteArray();
	}
	
	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		int length = reader.read(SIGNATURE_LENGTH_BITS);
		byte[] signature = reader.readBytes(length);
		
		return new CertificateVerify(signature);
	}

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.CERTIFICATE_VERIFY;
	}

	@Override
	public int getMessageLength() {
		return 2 + signature.length;
	}

}

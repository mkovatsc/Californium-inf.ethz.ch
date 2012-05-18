package ch.ethz.inf.vs.californium.dtls;

import java.math.BigInteger;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * This structure conveys the client's Diffie-Hellman public value (Yc) if it
 * was not already included in the client's {@link CertificateMessage}. The
 * encoding used for Yc is determined by the enumerated PublicValueEncoding.
 * This structure is a variant of the {@link ClientKeyExchange} message, and not
 * a message in itself.
 * 
 * @author Stefan Jucker
 * 
 */
public class DHClientKeyExchange extends ClientKeyExchange {


	private static final int LENGTH_BITS = 16;
	/** The client's Diffie-Hellman public value (Yc). */
	private byte[] dh_Yc; // may be empty if it was included in client's
							// certificate

	/**
	 * If the client has sent a certificate which contains a suitable
	 * Diffie-Hellman key, then Yc is implicit and does not need to be sent
	 * again. In this case, the client key exchange will be sent empty.
	 */
	public DHClientKeyExchange() {
		this.dh_Yc = null;
	}

	/**
	 * The client's public key is set explicitly.
	 * 
	 * @param publicKey the client's public key.
	 */
	public DHClientKeyExchange(BigInteger publicKey) {
		this.dh_Yc = toByteArray(publicKey);
	}
	
	public DHClientKeyExchange(byte[] dh_Yc) {
		this.dh_Yc = dh_Yc;
	}
	
	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());
		
		if (dh_Yc != null) {
			writer.write(dh_Yc.length, LENGTH_BITS);
			writer.writeBytes(dh_Yc);
		} // else send empty message
		
		return writer.toByteArray();
	}
	
	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		
		if (byteArray.length > 0) {
			DatagramReader reader = new DatagramReader(byteArray);
			int length = reader.read(LENGTH_BITS);
			byte[] publicKey = reader.readBytes(length);
			
			return new DHClientKeyExchange(publicKey);
		} else {
			return new DHClientKeyExchange();
		}

	}

	private byte[] toByteArray(BigInteger publicKey) {
		byte[] b = publicKey.toByteArray();
		// BigInteger uses two's-complement, remove extra zero at beginning if
		// necessary
		if (b.length > 1 && b[0] == 0) {
			int length = b.length - 1;
			byte[] newArray = new byte[length];
			// copy the array without the leading zero
			System.arraycopy(b, 1, newArray, 0, length);
		}
		return b;
	}

	@Override
	public int getMessageLength() {
		if (dh_Yc == null) {
			return 0;
		} else {
			return 2 + dh_Yc.length;
		}
	}
	
	public BigInteger getClientPublicKey() {
		return new BigInteger(1, dh_Yc);
	}

}

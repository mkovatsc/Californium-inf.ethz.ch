package ch.ethz.inf.vs.californium.dtls;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.util.Arrays;

import sun.security.ec.ECParameters;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * {@link ClientKeyExchange} message for all ECDH based key exchange methods.
 * Contains the client's ephemeral public value.
 * 
 * @author Stefan Jucker
 * 
 */
public class ECDHClientKeyExchange extends ClientKeyExchange {
	
	protected static final int LENGTH_BITS = 8; // opaque point <1..2^8-1>

	private byte[] pointEncoded;

	/**
	 * Called by the client with its public key.
	 * 
	 * @param clientPublicKey
	 */
	public ECDHClientKeyExchange(PublicKey clientPublicKey) {
		ECPublicKey publicKey = (ECPublicKey) clientPublicKey;
		ECPoint point = publicKey.getW();
		ECParameterSpec params = publicKey.getParams();
		
		pointEncoded = ECParameters.encodePoint(point, params.getCurve());
	}
	
	public ECDHClientKeyExchange(byte[] pointEncoded) {
		this.pointEncoded = pointEncoded;
	}
	
	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());
		
		int length = pointEncoded.length;
		writer.write(length, LENGTH_BITS);
		writer.writeBytes(pointEncoded);
		
		return writer.toByteArray();
	}
	
	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		int length = reader.read(LENGTH_BITS);
		byte[] pointEncoded = reader.readBytes(length);
		
		return new ECDHClientKeyExchange(pointEncoded);
	}

	@Override
	public int getMessageLength() {
		return 1 + pointEncoded.length;
	}
	
	byte[] getEncodedPoint() {
		return pointEncoded;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\t" + Arrays.toString(pointEncoded) + "\n");

		return sb.toString();
	}

}

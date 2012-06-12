package ch.ethz.inf.vs.californium.dtls;

import java.io.UnsupportedEncodingException;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

public class PSKClientKeyExchange extends ClientKeyExchange {

	// DTLS-specific constants ////////////////////////////////////////

	private static final int IDENTITY_LENGTH_BITS = 16;
	
	private static final String CHAR_SET = "UTF8";

	// Members ////////////////////////////////////////////////////////

	/**
	 * The PSK identity MUST be first converted to a character string, and then
	 * encoded to octets using UTF-8. See <a
	 * href="http://tools.ietf.org/html/rfc4279#section-5.1">RFC 4279</a>.
	 */
	private byte[] identityEncoded;

	/** The identity in cleartext. */
	private String identity;

	// Constructors ///////////////////////////////////////////////////
	
	public PSKClientKeyExchange(String identity) {
		this.identity = identity;
		try {
			this.identityEncoded = identity.getBytes(CHAR_SET);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public PSKClientKeyExchange(byte[] identityEncoded) {
		this.identityEncoded = identityEncoded;
		try {
			this.identity = new String(identityEncoded, CHAR_SET);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public int getMessageLength() {
		// fixed: 2 bytes for the length field
		// http://tools.ietf.org/html/rfc4279#section-2: opaque psk_identity<0..2^16-1>;
		return 2 + identityEncoded.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append(super.toString());
		sb.append("\t\tPSK Identity: " + identity + "\n");

		return sb.toString();
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());
		
		writer.write(identityEncoded.length, IDENTITY_LENGTH_BITS);
		writer.writeBytes(identityEncoded);
		
		return writer.toByteArray();
	}
	
	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		
		int length = reader.read(IDENTITY_LENGTH_BITS);
		byte[] identityEncoded = reader.readBytes(length);
		
		return new PSKServerKeyExchange(identityEncoded);
	}
	
	// Getters and Setters ////////////////////////////////////////////

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

}

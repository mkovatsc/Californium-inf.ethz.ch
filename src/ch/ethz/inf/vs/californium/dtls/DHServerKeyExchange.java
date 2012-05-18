package ch.ethz.inf.vs.californium.dtls;

import java.math.BigInteger;

public class DHServerKeyExchange extends ServerKeyExchange {
	
	private static final int LENGTH_BITS = 16;

	/** The prime modulus used for the Diffie-Hellman operation. */
	private byte[] dh_p; // <1..2^16-1>;

	/** The generator used for the Diffie-Hellman operation. */
	private byte[] dh_g; // <1..2^16-1>;

	/** The server's Diffie-Hellman public value (g^X mod p). */
	private byte[] dh_Ys; // <1..2^16-1>;

	/**
	 * For non-anonymous key exchanges, a signature over the server's key
	 * exchange parameters.
	 */
	private byte[] signature = null;
	
	public DHServerKeyExchange() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getMessageLength() {
		// fixed: dh_p length (2) + dh_g length (2) + dh_Ys (length) = 6
		int length = 6 + dh_p.length + dh_g.length + dh_Ys.length;
		if (signature != null) {
			// TODO
		}
		return length;
	}
	
	@Override
	public byte[] toByteArray() {
		// TODO Auto-generated method stub
		return super.toByteArray();
	}
	
	/**
	 * 
	 * @return the Diffie-Hellman prime modulus.
	 */
	public BigInteger getModulus() {
		return new BigInteger(1, dh_p);
	}
	
	/**
	 * 
	 * @return the Diffie-Hellman generator.
	 */
	public BigInteger getGenerator() {
		return new BigInteger(1, dh_g);
	}
	
	public BigInteger getPublicKey() {
		return new BigInteger(1, dh_Ys);
	}

}

package ch.ethz.inf.vs.californium.dtls;

/**
 * Defines the DTLS message interface used by {@link Record} and implemented by
 * the 4 message {@link ContentType}: {@link ChangeCipherSpecMessage},
 * {@link AlertMessage}, {@link HandshakeMessage} and {@link ApplicationMessage}
 * .
 * 
 * @author Stefan Jucker
 * 
 */
public interface DTLSMessage {

	/**
	 * 
	 * @return the length of this DTLS message <strong>in bytes</strong>.
	 */
	public int getLength();

	/**
	 * 
	 * @return the byte representation of this DTLS message.
	 */
	public byte[] toByteArray();
}

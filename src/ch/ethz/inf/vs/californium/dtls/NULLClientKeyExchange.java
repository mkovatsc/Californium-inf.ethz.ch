package ch.ethz.inf.vs.californium.dtls;


/**
 * According to <a href="http://tools.ietf.org/html/rfc5246#section-7.3">RFC
 * 5246</a>, the ClientKeyExchange is never optional. Therefore, to support the
 * NULL key exchange, this empty message is sent.
 * 
 * @author Stefan Jucker
 * 
 */
public class NULLClientKeyExchange extends ClientKeyExchange {
	
	// Methods ////////////////////////////////////////////////////////

	@Override
	public int getMessageLength() {
		return 0;
	}

	// Serialization //////////////////////////////////////////////////

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		return new NULLClientKeyExchange();
	}

}

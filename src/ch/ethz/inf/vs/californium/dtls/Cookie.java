package ch.ethz.inf.vs.californium.dtls;

/**
 * Represents a stateless cookie which is used in the {@link HelloVerifyRequest}
 * in the DTLS handshake to prevent denial-of-service attacks. See <a
 * href="http://tools.ietf.org/html/rfc6347#section-4.3.2">RFC 6347, 4.3.2.
 * Handshake Protocol</a> for further details.
 * 
 * @author Stefan Jucker
 * 
 */
public class Cookie {

	/** The cookie as byte array. */
	private byte[] cookie;

	/**
	 * Used by client, when sending a {@link ClientHello} for the first time
	 * (empty cookie).
	 */
	public Cookie() {
		/*
		 * TODO The DTLS server SHOULD generate cookies in such a way that they can
		 * be verified without retaining any per-client state on the server. One
		 * technique is to have a randomly generated secret and generate cookies
		 * as:
		 * 
		 * Cookie = HMAC(Secret, Client-IP, Client-Parameters)
		 */
		this.cookie = new byte[0];
	}

	/**
	 * Called when sending a {@link HelloVerifyRequest} (server) or
	 * {@link ClientHello} (client) for the second time.
	 * 
	 * @param cookie
	 *            the Cookie.
	 */
	public Cookie(byte[] cookie) {
		this.cookie = cookie;
	}

	/**
	 * 
	 * @return the number of bytes of the cookie.
	 */
	public int length() {
		return cookie.length;
	}
	
	public byte[] getCookie() {
		return cookie;
	}

	public void setCookie(byte[] cookie) {
		this.cookie = cookie;
	}
}

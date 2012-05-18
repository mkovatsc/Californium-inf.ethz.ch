package ch.ethz.inf.vs.californium.dtls;

public class Cookie {
	private byte[] cookie;

	public Cookie() {
		/*
		 * The DTLS server SHOULD generate cookies in such a way that they can
		 * be verified without retaining any per-client state on the server. One
		 * technique is to have a randomly generated secret and generate cookies
		 * as:
		 * 
		 * Cookie = HMAC(Secret, Client-IP, Client-Parameters)
		 */
		this.cookie = new byte[0];
	}

	public Cookie(byte[] cookie) {
		this.cookie = cookie;
	}

	public boolean isValidCookie() {
		/*
		 * When the second ClientHello is received, the server can verify that
		 * the Cookie is valid and that the client can receive packets at the
		 * given IP address.
		 */

		return false;
	}
	
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

package ch.ethz.inf.vs.californium.dtls;

import java.util.Arrays;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * 
 * @author Jucker
 * 
 */
public class HelloVerifyRequest extends HandshakeMessage {

	// Logging ///////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(HelloVerifyRequest.class.getName());

	// DTLS-specific constants ///////////////////////////////////////////

	private static final int VERSION_BITS = 8; // for major and minor each

	private static final int COOKIE_LENGTH_BITS = 8;

	// Members ///////////////////////////////////////////////////////////

	/**
	 * This field will contain the lower of that suggested by the client in the
	 * client hello and the highest supported by the server.
	 */
	private ProtocolVersion serverVersion;

	/**  */
	private Cookie cookie;

	public HelloVerifyRequest(ProtocolVersion version, Cookie cookie) {
		this.serverVersion = version;
		this.cookie = cookie;
	}

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		writer.write(serverVersion.getMajor(), VERSION_BITS);
		writer.write(serverVersion.getMinor(), VERSION_BITS);

		writer.write(cookie.length(), COOKIE_LENGTH_BITS);
		writer.writeBytes(cookie.getCookie());

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		int major = reader.read(VERSION_BITS);
		int minor = reader.read(VERSION_BITS);
		ProtocolVersion version = new ProtocolVersion(major, minor);

		int cookieLength = reader.read(COOKIE_LENGTH_BITS);
		Cookie cookie = new Cookie(reader.readBytes(cookieLength));

		return new HelloVerifyRequest(version, cookie);
	}

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.HELLO_VERIFY_REQUEST;
	}

	@Override
	public int getMessageLength() {
		// fixed: version (2) + cookie length (1)
		return 3 + cookie.length();
	}
	
	public ProtocolVersion getServerVersion() {
		return serverVersion;
	}

	public Cookie getCookie() {
		return cookie;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\tServer Version: " + serverVersion.getMajor() + ", " + serverVersion.getMinor() + "\n");
		sb.append("\t\tCookie Length: " + cookie.length() + "\n");
		sb.append("\t\tCookie: " + Arrays.toString(cookie.getCookie()) + "\n");

		return sb.toString();
	}

}

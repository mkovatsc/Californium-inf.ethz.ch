package ch.ethz.inf.vs.californium.dtls;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * When a client first connects to a server, it is required to send the
 * ClientHello as its first message. The client can also send a ClientHello in
 * response to a {@link HelloRequest} or on its own initiative in order to
 * renegotiate the security parameters in an existing connection.
 * 
 * @author Stefan Jucker
 */
public class ClientHello extends HandshakeMessage {

	// Logging ///////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(ClientHello.class.getName());

	// DTLS-specific constants ///////////////////////////////////////////

	private static final int VERSION_BITS = 8; // for major and minor each

	private static final int RANDOM_BYTES = 32;

	private static final int SESSION_ID_LENGTH_BITS = 8;

	private static final int COOKIE_LENGTH = 8;

	private static final int CIPHER_SUITS_LENGTH_BITS = 16;

	private static final int COMPRESSION_METHODS_LENGTH_BITS = 8;

	// Members ///////////////////////////////////////////////////////////

	/**
	 * The version of the DTLS protocol by which the client wishes to
	 * communicate during this session.
	 */
	private ProtocolVersion clientVersion = new ProtocolVersion(254, 253);

	/** A client-generated random structure. */
	private Random random;

	/** The ID of a session the client wishes to use for this connection. */
	private SessionId sessionId;

	/**  */
	private Cookie cookie;

	/**
	 * This is a list of the cryptographic options supported by the client, with
	 * the client's first preference first.
	 */
	private List<CipherSuite> cipherSuites;

	/**
	 * This is a list of the compression methods supported by the client, sorted
	 * by client preference.
	 */
	private List<CompressionMethod> compressionMethods;

	// Constructors ///////////////////////////////////////////////////////////

	public ClientHello(ProtocolVersion version, SecureRandom secureRandom) {
		this.clientVersion = version;
		this.random = new Random(secureRandom);
		this.sessionId = new SessionId(new byte[0]);
		this.cookie = new Cookie(new byte[0]);
	}

	/**
	 * Used when resume session -> session id known
	 * 
	 * @param version
	 * @param secureRandom
	 * @param sessionId
	 */
	public ClientHello(ProtocolVersion version, SecureRandom secureRandom, DTLSSession session) {
		this.clientVersion = version;
		this.random = new Random(secureRandom);
		this.sessionId = session.getSessionIdentifier();
		this.cookie = new Cookie(new byte[0]);
		
		// TODO set cipher suite and compression according to session
	}

	public ClientHello(ProtocolVersion clientVersion, Random random, SessionId sessionId, Cookie cookie, List<CipherSuite> cipherSuites, List<CompressionMethod> compressionMethods) {
		this.clientVersion = clientVersion;
		this.random = random;
		this.sessionId = sessionId;
		this.cookie = cookie;
		this.cipherSuites = cipherSuites;
		this.compressionMethods = compressionMethods;
	}

	@Override
	public byte[] toByteArray() {

		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());

		writer.write(clientVersion.getMajor(), VERSION_BITS);
		writer.write(clientVersion.getMinor(), VERSION_BITS);

		writer.writeBytes(random.getRandomBytes());

		writer.write(sessionId.length(), SESSION_ID_LENGTH_BITS);
		writer.writeBytes(sessionId.getSessionId());

		writer.write(cookie.length(), COOKIE_LENGTH);
		writer.writeBytes(cookie.getCookie());

		writer.write(cipherSuites.size() * 2, CIPHER_SUITS_LENGTH_BITS);
		writer.writeBytes(CipherSuite.listToByteArray(cipherSuites));

		writer.write(compressionMethods.size(), COMPRESSION_METHODS_LENGTH_BITS);
		writer.writeBytes(CompressionMethod.listToByteArray(compressionMethods));

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		int major = reader.read(VERSION_BITS);
		int minor = reader.read(VERSION_BITS);
		ProtocolVersion clientVersion = new ProtocolVersion(major, minor);

		Random random = new Random(reader.readBytes(RANDOM_BYTES));

		int sessionIdLength = reader.read(SESSION_ID_LENGTH_BITS);
		SessionId sessionId = new SessionId(reader.readBytes(sessionIdLength));

		int cookieLength = reader.read(COOKIE_LENGTH);
		Cookie cookie = new Cookie(reader.readBytes(cookieLength));

		int cipherSuitesLength = reader.read(CIPHER_SUITS_LENGTH_BITS);
		List<CipherSuite> cipherSuites = CipherSuite.listFromByteArray(reader.readBytes(cipherSuitesLength), cipherSuitesLength / 2); // 2

		int compressionMethodsLength = reader.read(COMPRESSION_METHODS_LENGTH_BITS);
		List<CompressionMethod> compressionMethods = CompressionMethod.listFromByteArray(reader.readBytes(compressionMethodsLength), compressionMethodsLength);

		return new ClientHello(clientVersion, random, sessionId, cookie, cipherSuites, compressionMethods);

	}

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.CLIENT_HELLO;
	}

	@Override
	public int getMessageLength() {
		// fixed sizes: version (2) + random (32) + session ID length (1) +
		// cookie length (1) + cipher suites length (2) + compression methods
		// length (1)
		return 39 + sessionId.length() + cookie.length() + cipherSuites.size() * 2 + compressionMethods.size();
	}

	public ProtocolVersion getClientVersion() {
		return clientVersion;
	}

	public void setClientVersion(ProtocolVersion clientVersion) {
		this.clientVersion = clientVersion;
	}

	public Random getRandom() {
		return random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public SessionId getSessionId() {
		return sessionId;
	}

	public void setSessionId(SessionId sessionId) {
		this.sessionId = sessionId;
	}

	public Cookie getCookie() {
		return cookie;
	}

	public void setCookie(Cookie cookie) {
		this.cookie = cookie;
	}

	public List<CipherSuite> getCipherSuits() {
		return cipherSuites;
	}

	public void setCipherSuits(List<CipherSuite> cipherSuits) {
		this.cipherSuites = cipherSuits;
	}

	public void addCipherSuite(CipherSuite cipherSuite) {
		if (cipherSuites == null) {
			cipherSuites = new ArrayList<CipherSuite>();
		}
		cipherSuites.add(cipherSuite);
	}

	public List<CompressionMethod> getCompressionMethods() {
		return compressionMethods;
	}

	public void setCompressionMethods(List<CompressionMethod> compressionMethods) {
		this.compressionMethods = compressionMethods;
	}

	public void addCompressionMethod(CompressionMethod compressionMethod) {
		if (compressionMethods == null) {
			compressionMethods = new ArrayList<CompressionMethod>();
		}
		compressionMethods.add(compressionMethod);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\tVersion: " + clientVersion.getMajor() + ", " + clientVersion.getMinor() + "\n");
		sb.append("\t\tClient Random: " + Arrays.toString(random.getRandomBytes()) + "\n");
		sb.append("\t\tSession ID Length: " + sessionId.length() + "\n");
		if (sessionId.length() > 0) {
			sb.append("\t\tSession ID: " + sessionId.getSessionId() + "\n");
		}
		sb.append("\t\tCookie Length: " + cookie.length() + "\n");
		if (cookie.length() > 0) {
			sb.append("\t\tCookie: " + Arrays.toString(cookie.getCookie()) + "\n");
		}
		sb.append("\t\tCipher Suites Length: " + cipherSuites.size() * 2 + "\n");
		sb.append("\t\tCipher Suites (" + cipherSuites.size() + " suites)\n");
		for (CipherSuite cipher : cipherSuites) {
			sb.append("\t\t\tCipher Suite: " + cipher.toString() + "\n");
		}
		sb.append("\t\tCompression Methods Length: " + compressionMethods.size() + "\n");
		sb.append("\t\tCompression Methods (" + compressionMethods.size() + " method)" + "\n");
		for (CompressionMethod method : compressionMethods) {
			sb.append("\t\t\tCompression Method: " + method.toString() + "\n");
		}

		return sb.toString();
	}

}
/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.dtls;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.dtls.CertificateTypeExtension.CertificateType;
import ch.ethz.inf.vs.californium.dtls.SupportedPointFormatsExtension.ECPointFormat;
import ch.ethz.inf.vs.californium.util.ByteArrayUtils;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * When a client first connects to a server, it is required to send the
 * ClientHello as its first message. The client can also send a ClientHello in
 * response to a {@link HelloRequest} or on its own initiative in order to
 * renegotiate the security parameters in an existing connection. See <a
 * href="http://tools.ietf.org/html/rfc5246#section-7.4.1.2">RFC 5246</a>.
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
	private ProtocolVersion clientVersion = new ProtocolVersion();

	/** A client-generated random structure. */
	private Random random;

	/** The ID of a session the client wishes to use for this connection. */
	private SessionId sessionId;

	/** The cookie used to prevent flooding attacks (potentially empty). */
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

	/**
	 * Clients MAY request extended functionality from servers by sending data
	 * in the extensions field.
	 */
	private HelloExtensions extensions = null;

	// Constructors ///////////////////////////////////////////////////////////

	/**
	 * 
	 * @param version
	 * @param secureRandom
	 */
	public ClientHello(ProtocolVersion version, SecureRandom secureRandom) {
		this.clientVersion = version;
		this.random = new Random(secureRandom);
		this.sessionId = new SessionId(new byte[] {});
		this.cookie = new Cookie();
		this.extensions = new HelloExtensions();
		
		// the supported elliptic curves
		List<Integer> curves = Arrays.asList(
				ECDHServerKeyExchange.NAMED_CURVE_INDEX.get("secp256r1"),
				ECDHServerKeyExchange.NAMED_CURVE_INDEX.get("secp384r1"),
				ECDHServerKeyExchange.NAMED_CURVE_INDEX.get("secp521r1"));
		HelloExtension supportedCurvesExtension = new SupportedEllipticCurvesExtension(curves);
		this.extensions.addExtension(supportedCurvesExtension);
		
		// the supported point formats
		List<ECPointFormat> formats = Arrays.asList(ECPointFormat.UNCOMPRESSED);
		HelloExtension supportedPointFormatsExtension = new SupportedPointFormatsExtension(formats);
		this.extensions.addExtension(supportedPointFormatsExtension);
		
		// the supported certificate types
		CertificateTypeExtension certificateTypeExtension = new CertificateTypeExtension(true);
		if (Properties.std.getBool("USE_RAW_PUBLIC_KEY")) {
			certificateTypeExtension.addCertificateType(CertificateType.RAW_PUBLIC_KEY);
			certificateTypeExtension.addCertificateType(CertificateType.X_509);
		} else {
			// the client supports rawPublicKeys but prefers X.509 certificates
			
			// http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-03#section-3.1:
			// this extension MUST be omitted if the client only supports X.509 certificates
			certificateTypeExtension.addCertificateType(CertificateType.X_509);
			certificateTypeExtension.addCertificateType(CertificateType.RAW_PUBLIC_KEY);
		}
		
		this.extensions.addExtension(certificateTypeExtension);
	}

	/**
	 * Constructor used when resuming a session; session ID must be known.
	 * 
	 * @param version
	 *            the version
	 * @param secureRandom
	 *            the secure random
	 * @param session
	 *            the session
	 */
	public ClientHello(ProtocolVersion version, SecureRandom secureRandom, DTLSSession session) {
		this.clientVersion = version;
		this.random = new Random(secureRandom);
		this.sessionId = session.getSessionIdentifier();
		this.cookie = new Cookie();
		addCipherSuite(session.getWriteState().getCipherSuite());
		addCompressionMethod(session.getReadState().getCompressionMethod());
	}

	/**
	 * Constructor used when reconstructing from byteArray.
	 * 
	 * @param clientVersion
	 *            the requested version.
	 * @param random
	 *            the client the client's random.
	 * @param sessionId
	 *            the session id (potentially empty).
	 * @param cookie
	 *            the cookie (potentially empty).
	 * @param cipherSuites
	 *            the available cipher suites.
	 * @param compressionMethods
	 *            the available compression methods.
	 * @param extensions
	 *            the extensions (potentially empty).
	 */
	public ClientHello(ProtocolVersion clientVersion, Random random, SessionId sessionId, Cookie cookie, List<CipherSuite> cipherSuites, List<CompressionMethod> compressionMethods, HelloExtensions extensions) {
		this.clientVersion = clientVersion;
		this.random = random;
		this.sessionId = sessionId;
		this.cookie = cookie;
		this.cipherSuites = cipherSuites;
		this.compressionMethods = compressionMethods;
		this.extensions = extensions;
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] fragmentToByteArray() {

		DatagramWriter writer = new DatagramWriter();

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

		if (extensions != null) {
			writer.writeBytes(extensions.toByteArray());
		}

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) throws HandshakeException {
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

		byte[] bytesLeft = reader.readBytesLeft();
		HelloExtensions extensions = null;
		if (bytesLeft.length > 0) {
			extensions = HelloExtensions.fromByteArray(bytesLeft);
		}

		return new ClientHello(clientVersion, random, sessionId, cookie, cipherSuites, compressionMethods, extensions);

	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.CLIENT_HELLO;
	}

	@Override
	public int getMessageLength() {
		/*
		 * if no extensions set, empty; otherwise 2 bytes for field length and
		 * then the length of the extensions. See
		 * http://tools.ietf.org/html/rfc5246#section-7.4.1.2
		 */
		int extensionsLength = (extensions != null) ? (2 + extensions.getLength()) : 0;

		/*
		 * fixed sizes: version (2) + random (32) + session ID length (1) +
		 * cookie length (1) + cipher suites length (2) + compression methods
		 * length (1) = 39
		 */
		return 39 + sessionId.length() + cookie.length() + cipherSuites.size() * 2 + compressionMethods.size() + extensionsLength;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\tVersion: " + clientVersion.getMajor() + ", " + clientVersion.getMinor() + "\n");
		sb.append("\t\tRandom: \n" + random.toString());
		sb.append("\t\tSession ID Length: " + sessionId.length() + "\n");
		if (sessionId.length() > 0) {
			sb.append("\t\tSession ID: " + sessionId.getSessionId() + "\n");
		}
		sb.append("\t\tCookie Length: " + cookie.length() + "\n");
		if (cookie.length() > 0) {
			sb.append("\t\tCookie: " + ByteArrayUtils.toHexString(cookie.getCookie()) + "\n");
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
		if (extensions != null) {
			sb.append(extensions.toString());
		}

		return sb.toString();
	}

	// Getters and Setters ////////////////////////////////////////////

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

	public List<CipherSuite> getCipherSuites() {
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
	
	/**
	 * Gets the client's 'cert-receive' extension if available. As described in
	 * <a href="http://tools.ietf.org/html/draft-ietf-tls-oob-pubkey-04">
	 * Out-of-Band Public Key Validation</a>.
	 * 
	 * @return the 'cert-receive' extensions, if available, <code>null</code>
	 *         otherwise.
	 */
	public CertReceiveExtension getCertReceiveExtension() {
		if (extensions != null) {
			List<HelloExtension> exts = extensions.getExtensions();
			for (HelloExtension helloExtension : exts) {
				if (helloExtension instanceof CertReceiveExtension) {
					return (CertReceiveExtension) helloExtension;
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @return the 'cert-send' extension, if available, <code>null</code>
	 *         otherwise.
	 */
	public CertSendExtension getCertSendExtension() {
		if (extensions != null) {
			List<HelloExtension> exts = extensions.getExtensions();
			for (HelloExtension helloExtension : exts) {
				if (helloExtension instanceof CertSendExtension) {
					return (CertSendExtension) helloExtension;
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @return the client's supported elliptic curves extension if available,
	 *         otherwise <code>null</code>.
	 */
	public SupportedEllipticCurvesExtension getSupportedEllipticCurvesExtension() {
		if (extensions != null) {
			List<HelloExtension> exts = extensions.getExtensions();
			for (HelloExtension helloExtension : exts) {
				if (helloExtension instanceof SupportedEllipticCurvesExtension) {
					return (SupportedEllipticCurvesExtension) helloExtension;
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @return the client's certificate type extension if available,
	 *         otherwise <code>null</code>.
	 */
	public CertificateTypeExtension getCertificateTypeExtension() {
		if (extensions != null) {
			List<HelloExtension> exts = extensions.getExtensions();
			for (HelloExtension helloExtension : exts) {
				if (helloExtension instanceof CertificateTypeExtension) {
					return (CertificateTypeExtension) helloExtension;
				}
			}
		}
		return null;
	}

}
package ch.ethz.inf.vs.californium.dtls;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.SecretKey;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;

/**
 * Does the protocol handshaking from the server's point of view. It is
 * message-driven by the parent {@link Handshaker} class.
 * 
 * @author Stefan Jucker
 * 
 */
public class ServerHandshaker extends Handshaker {

	/** Is the client required to authenticate itself? */
	private boolean clientAuthenticationRequired;

	/** The server's certificate. */
	private X509Certificate[] certificates;

	/** The server's private key. */
	private PrivateKey privateKey;

	/*
	 * Store the client's messages for later use (handshake hash).
	 */
	private ClientHello clientHello = null;
	private CertificateMessage clientCertificate = null;
	private ClientKeyExchange clientKeyExchange = null;
	private CertificateVerify certificateVerify = null;
	private Finished clientFinished = null;

	private MessageDigest md;

	private Cookie cookie;

	private ECDHECryptography ecdhe;

	public ServerHandshaker(DatagramSocket socket, EndpointAddress endpointAddress, X509Certificate[] certificates) {
		super(socket, endpointAddress, false);
		this.certificates = certificates;
		this.privateKey = loadPrivateKey();
		try {
			this.md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void processMessage(HandshakeMessage message) throws IOException {

		switch (message.getMessageType()) {
		case CLIENT_HELLO:
			clientHello((ClientHello) message);
			break;

		case CERTIFICATE:
			clientCertificate = (CertificateMessage) message;
			// TODO
			break;

		case CLIENT_KEY_EXCHANGE:
			SecretKey premasterSecret;
			switch (keyExchange) {
			case PSK:
				// TODO what to do with preshared key

			case EC_DIFFIE_HELLMAN:
				premasterSecret = clientKeyExchange((ECDHClientKeyExchange) message);
				generateKeys(premasterSecret);
			default:
				break;
			}
			break;

		case CERTIFICATE_VERIFY:
			certificateVerify = (CertificateVerify) message;
			// TODO
			break;

		case FINISHED:
			clientFinished = (Finished) message;
			break;

		default:
			// unsupported handshake message received at server side
			break;
		}

		checkClientFinished();
	}

	/**
	 * Checks whether all mandatory client handshake messages have arrived. If
	 * so, the server continues by sending a {@link ChangeCipherSpecMessage} and
	 * a {@link Finished}.
	 * 
	 * @throws IOException
	 */
	private void checkClientFinished() throws IOException {
		// the mandatory messages: must always be sent by client
		if (clientHello == null || clientKeyExchange == null || clientFinished == null) {
			return;
		}
		// the optional messages: only sent by client, if the server requires
		// client authentication
		if (clientAuthenticationRequired) {
			if (clientCertificate == null || certificateVerify == null) {
				return;
			}
		}

		clientFinished();
	}

	/**
	 * Called when all required client handshake messages have arrived. Send a
	 * {@link ChangeCipherSpecMessage} and {@link Finished} message. If the
	 * client's finished verify data does not match, abort.
	 * 
	 * @throws IOException
	 */
	private void clientFinished() throws IOException {
		// create handshake hash
		if (clientCertificate != null) { // optional
			md.update(clientCertificate.toByteArray());
		}

		md.update(clientKeyExchange.toByteArray()); // mandatory

		if (certificateVerify != null) { // optional
			md.update(certificateVerify.toByteArray());
		}

		
		MessageDigest md2 = null;
		try {
			/*
			 * the handshake_messages for the Finished message
			 * sent by the client will be different from that for the Finished
			 * message sent by the server, because the one that is sent second
			 * will include the prior one.
			 */
			md2 = (MessageDigest) md.clone();
			md2.update(clientFinished.toByteArray());
		} catch (CloneNotSupportedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Verify client's data
		byte[] handshakeHash = md.digest();
		if (!clientFinished.verifyData(getMasterSecret(), true, handshakeHash)) {
			// TODO
			LOG.severe("Server could not verify client's finished handshake message!");
		}

		// TODO First, send change cipher spec

		// Second, send own finished message
		handshakeHash = md2.digest();
		Finished finished = new Finished(getMasterSecret(), isClient, handshakeHash);
		sendHandshakeMessage(finished);

		// Finally, create session
		DTLSSession session = new DTLSSession(sessionId, null, compressionMethod, cipherSuite, getMasterSecret(), true);
		try {
			queue.put(session);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Called after the server receives a {@link ClientHello} handshake message.
	 * If the message has a {@link Cookie} set, verify it and continue with
	 * {@link ServerHello}, otherwise reply with a {@link HelloVerifyRequest}.
	 * 
	 * @param message
	 *            the client's hello message.
	 * @throws IOException
	 */
	private void clientHello(ClientHello message) throws IOException {

		if (message.getCookie().length() > 0 && isValidCookie(message.getCookie())) {
			// second client hello, with cookie, verify this, send hello server
			// afterwards
			clientHello = message;
			md.update(clientHello.toByteArray());

			if (message.getSessionId().length() > 0) {
				// TODO client has sent non-empty session id, try resuming it

			} else { // client did not try to resume

				/*
				 * First, send SERVER HELLO
				 */

				// TODO negotiate version
				ProtocolVersion serverVersion = new ProtocolVersion();

				// store client random
				clientRandom = message.getRandom();
				// server random
				serverRandom = new Random(new SecureRandom());

				// TODO create session id
				sessionId = new SessionId();
				// TODO store session for later

				// TODO negotiate cipher suite and compression method
				CipherSuite cipherSuite = CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;
				CompressionMethod compressionMethod = CompressionMethod.NULL;
				setCipherSuite(cipherSuite);

				ServerHello serverHello = new ServerHello(serverVersion, serverRandom, sessionId, cipherSuite, compressionMethod);
				sendHandshakeMessage(serverHello);
				md.update(serverHello.toByteArray());

				/*
				 * Second, send CERTIFICATE
				 */

				CertificateMessage certificate = new CertificateMessage(certificates);
				sendHandshakeMessage(certificate);
				md.update(certificate.toByteArray());

				/*
				 * Third, send SERVER KEY EXCHANGE
				 */
				switch (keyExchange) {
				case EC_DIFFIE_HELLMAN:
					ecdhe = new ECDHECryptography();
					ServerKeyExchange serverKeyExchange = new ECDHServerKeyExchange(ecdhe, privateKey, clientRandom, serverRandom);
					sendHandshakeMessage(serverKeyExchange);
					md.update(serverKeyExchange.toByteArray());

					break;
					
				case PSK:
					// TODO
					break;

				default:
					break;
				}

				/*
				 * Fourth, if required, send certificate request for client
				 */
				if (clientAuthenticationRequired) {
					// TODO
					CertificateRequest certificateRequest = new CertificateRequest();
					md.update(certificateRequest.toByteArray());
				}

				/*
				 * Last, send server hello done
				 */
				ServerHelloDone serverHelloDone = new ServerHelloDone();
				sendHandshakeMessage(serverHelloDone);
				md.update(serverHelloDone.toByteArray());

			}

		} else { // either first time, or cookies did not match
			// first client hello -> send hello verify request
			cookie = generateCookie();
			HelloVerifyRequest helloVerifyRequest = new HelloVerifyRequest(new ProtocolVersion(), cookie);
			sendHandshakeMessage(helloVerifyRequest);
		}
	}

	private SecretKey clientKeyExchange(ECDHClientKeyExchange message) {
		clientKeyExchange = message;
		return ecdhe.getSecret(message.getEncodedPoint());
	}

	private Cookie generateCookie() {
		// TODO as suggested in the spec.
		return new Cookie(new Random(new SecureRandom()).getRandomBytes());
	}

	private boolean isValidCookie(Cookie clientCookie) {
		return Arrays.equals(cookie.getCookie(), clientCookie.getCookie());
	}

	@Override
	public void startHandshake() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public HandshakeMessage getStartHandshakeMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendHandshakeMessage(HandshakeMessage message) throws IOException {
		Record record = new Record(ContentType.HANDSHAKE, 0, message);

		// retrieve payload
		byte[] payload = record.toByteArray(this);

		// create datagram
		DatagramPacket datagram = new DatagramPacket(payload, payload.length, endpointAddress.getAddress(), endpointAddress.getPort());

		socket.send(datagram);

	}

	private PrivateKey loadPrivateKey() {
		PrivateKey privateKey = null;
		try {
			RandomAccessFile raf = new RandomAccessFile("C:\\Users\\Jucker\\git\\Californium\\src\\ch\\ethz\\inf\\vs\\californium\\dtls\\ec.pk8", "r");
			byte[] buf = new byte[(int) raf.length()];
			raf.readFully(buf);
			raf.close();
			PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(buf);
			KeyFactory keyF = KeyFactory.getInstance("EC");
			privateKey = keyF.generatePrivate(kspec);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return privateKey;
	}

}

package ch.ethz.inf.vs.californium.dtls;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;

import javax.crypto.SecretKey;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;

/**
 * ClientHandshaker does the protocol handshaking from the point of view of a
 * client. It is driven by handshake messages as delivered by the parent
 * {@link Handshaker} class.
 * 
 * @author Stefan Jucker
 * 
 */
public class ClientHandshaker extends Handshaker {

	// Members ////////////////////////////////////////////////////////

	private ProtocolVersion maxProtocolVersion = new ProtocolVersion();

	/** the server's public key from its certificate */
	private PublicKey serverPublicKey;

	/** the server's ephemeral public key, used for key agreement */
	private ECPublicKey ephemeralServerPublicKey;

	/** A helper class to perform ECDHE key agreement and key generation */
	private ECDHECryptography ecdhe;

	/** the client's hello handshake message */
	private ClientHello clientHello = null;

	/*
	 * Store the server's messages to check if every message has been received.
	 * Since we can't rely on the fact that when a ServerHelloDone is received
	 * that each message has already arrived.
	 */
	private ServerHello serverHello = null;
	private CertificateMessage serverCertificate = null;
	private CertificateRequest certificateRequest = null;
	private ServerKeyExchange serverKeyExchange = null;
	private ServerHelloDone serverHelloDone = null;

	/** the hash of all received handshake messages sent in the finished message */
	private byte[] handshakeHash = null;

	// Constructors ///////////////////////////////////////////////////

	/**
	 * Called when trying to establish a new DTLS connection.
	 * 
	 * @param socket
	 * @param endpointAddress
	 */
	public ClientHandshaker(DatagramSocket socket, EndpointAddress endpointAddress) {
		super(socket, endpointAddress, true);
	}

	/**
	 * Called when trying to resume a available DTLS session.
	 * 
	 * @param socket
	 * @param endpointAddress
	 * @param session
	 */
	public ClientHandshaker(DatagramSocket socket, EndpointAddress endpointAddress, DTLSSession session) {
		super(socket, endpointAddress, true);
		setSession(session);
	}

	@Override
	public void processMessage(HandshakeMessage message) throws IOException {

		switch (message.getMessageType()) {
		case HELLO_REQUEST:
			helloRequest((HelloRequest) message);
			break;

		case HELLO_VERIFY_REQUEST:
			helloVerifyRequest((HelloVerifyRequest) message);
			break;

		case SERVER_HELLO:
			serverHello((ServerHello) message);
			break;

		case CERTIFICATE:
			serverCertificate((CertificateMessage) message);
			break;

		case SERVER_KEY_EXCHANGE:

			switch (keyExchange) {
			case EC_DIFFIE_HELLMAN:
				serverKeyExchange((ECDHServerKeyExchange) message);
				break;

			case PSK:

				break;

			default:
				LOG.severe("Not supported server key exchange algorithm: " + keyExchange.toString());
				break;
			}
			break;

		case CERTIFICATE_REQUEST:
			// save for later, will be handled by server hello done
			certificateRequest = (CertificateRequest) message;
			break;

		case SERVER_HELLO_DONE:
			serverHelloDone = (ServerHelloDone) message;
			break;

		case FINISHED:
			state = HandshakeType.FINISHED.getCode();
			serverFinished((Finished) message);
			break;

		default:
			LOG.severe("Not supported handshake message received:\n" + message.toString());
			break;
		}
		checkServerHelloDone();
	}

	/**
	 * Called when the client received the server's finished message. If the
	 * data can be verified, encrypted application data can be sent.
	 * 
	 * @param message
	 *            the {@link Finished} message.
	 */
	private void serverFinished(Finished message) {
		/*
		 * RFC 5249 - 7.4.9. Finished: It is a fatal error if a Finished message
		 * is not preceded by a ChangeCipherSpec message at the appropriate
		 * point in the handshake.
		 */
		// TODO check this

		if (!message.verifyData(getMasterSecret(), false, handshakeHash)) {
			// TODO send some alert message:
			LOG.severe("Client could not verify server's finished message!");
		}

		// add to queue, to signal, that finished
		try {
			DTLSSession session = new DTLSSession(sessionId, null, compressionMethod, cipherSuite, getMasterSecret(), true);
			queue.put(session);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Check if all message received from server.
	 * 
	 * @throws IOException
	 */
	private void checkServerHelloDone() throws IOException {

		if (state > HandshakeType.SERVER_HELLO_DONE.getCode()) {
			// server already done
			return;
		}
		if (keyExchange != null) { // if key exchange not set, server not
									// finished
			switch (keyExchange) {
			case EC_DIFFIE_HELLMAN:
				if (serverHello == null || serverHelloDone == null || serverCertificate == null || serverKeyExchange == null) {
					// ECDHE requires all these messages
					return;
				}

				if ((serverHelloDone.getMessageSeq() - serverKeyExchange.getMessageSeq()) > 1 && certificateRequest == null) {
					// certificate request needs also to be set
					return;
				}

				break;

			case PSK:
				// TODO
				return;

			default:
				return;
			}

			// all mandatory messages have arrived
			serverHelloDone();
		}

	}

	/**
	 * Used by the server to kickstart negotiations.
	 * 
	 * @param message
	 *            the hello request message
	 */
	private void helloRequest(HelloRequest message) {
		if (state < HandshakeType.HELLO_REQUEST.getCode()) {
			// TODO start renegotiations
		} else {
			// already started with handshake, drop this message
		}
	}

	/**
	 * Sent by the server to prevent flooding of a client.
	 * 
	 * @param message
	 * @throws IOException
	 */
	private void helloVerifyRequest(HelloVerifyRequest message) throws IOException {
		Cookie cookie = message.getCookie();
		clientHello.setCookie(cookie);

		setSequenceNumber(clientHello);
		sendHandshakeMessage(clientHello);
	}

	/**
	 * Stores the negotiated security parameters and creates a session.
	 * 
	 * @param message
	 *            the {@link ServerHello} message.
	 */
	private void serverHello(ServerHello message) {
		// TODO check if server made valid selections

		// store the negotiated values
		usedProtocol = message.getServerVersion();
		serverRandom = message.getRandom();
		sessionId = message.getSessionId();
		setCipherSuite(message.getCipherSuite());
		compressionMethod = message.getCompressionMethod();

		// TODO create a new session / resume session

		serverHello = message;

	}

	/**
	 * Unless a anonymous cipher suite is used, the server always sends a
	 * certificate.
	 * 
	 * @param message
	 *            the server's certificate
	 */
	private void serverCertificate(CertificateMessage message) {
		X509Certificate[] certificateChain = message.getCertificateChain();
		serverCertificate = message;

		// TODO verify certificate chain

		// TODO store certificate chain in session

		// get server's public key
		serverPublicKey = certificateChain[0].getPublicKey();
	}

	private void serverKeyExchange(ECDHServerKeyExchange message) {
		serverKeyExchange = message;
		if (message.verifySignature(serverPublicKey, clientRandom, serverRandom)) {
			ephemeralServerPublicKey = message.getPublicKey();
			ecdhe = new ECDHECryptography(ephemeralServerPublicKey.getParams());
		} else {
			// TODO
			LOG.severe("Could not verify server's key exchange message");
		}
	}

	private void serverHelloDone() throws IOException {

		/*
		 * All possible handshake messages sent in this flight. Used to compute
		 * handshake hash.
		 */
		CertificateMessage clientCertificate = null;
		ClientKeyExchange clientKeyExchange = null;
		CertificateVerify certificateVerify = null;

		/*
		 * First, if required by server, send client certificate
		 */
		if (certificateRequest != null) {
			// TODO
			clientCertificate = new CertificateMessage(null);
			setSequenceNumber(clientCertificate);
			sendHandshakeMessage(clientCertificate);
		}

		/*
		 * Second, send client key exchange as specified by the key exchange
		 * algorithm
		 */

		SecretKey premasterSecret;
		switch (keyExchange) {
		case EC_DIFFIE_HELLMAN:
			try {
				clientKeyExchange = new ECDHClientKeyExchange(ecdhe.getPublicKey());
				premasterSecret = ecdhe.getSecret(ephemeralServerPublicKey);

				generateKeys(premasterSecret);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		default:
			break;
		}
		setSequenceNumber(clientKeyExchange);
		sendHandshakeMessage(clientKeyExchange);

		/*
		 * Third, send certificate verify message if necessary.
		 */
		if (certificateRequest != null) {
			certificateVerify = new CertificateVerify(null);
			setSequenceNumber(certificateVerify);
			sendHandshakeMessage(certificateVerify);
		}

		/*
		 * Fourth, send a change cipher spec.
		 */
		// TODO

		/*
		 * Fifth, send the finished message.
		 */
		try {
			// create hash of handshake messages
			// can't do this on the fly, since there is no explicit ordering of
			// messages

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(clientHello.toByteArray());
			md.update(serverHello.toByteArray());
			if (serverCertificate != null) {
				md.update(serverCertificate.toByteArray());
			}
			if (serverKeyExchange != null) {
				md.update(serverKeyExchange.toByteArray());
			}
			if (certificateRequest != null) {
				md.update(certificateRequest.toByteArray());
			}
			md.update(serverHelloDone.toByteArray());

			if (clientCertificate != null) {
				// TODO
				md.update(clientCertificate.toByteArray());
			}
			md.update(clientKeyExchange.toByteArray());

			if (certificateVerify != null) {
				// md.update(certificateVerify.toByteArray());
			}

			MessageDigest md2 = null;

			try {
				md2 = (MessageDigest) md.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			handshakeHash = md.digest();
			Finished finished = new Finished(getMasterSecret(), isClient, handshakeHash);

			// TODO encrypt finished message
			setSequenceNumber(finished);

			// compute handshake hash with client's finished message also
			// included, used for server's finished message
			md2.update(finished.toByteArray());
			handshakeHash = md2.digest();

			sendHandshakeMessage(finished);

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void startHandshake() throws IOException {
		HandshakeMessage fragment = getStartHandshakeMessage();

		sendHandshakeMessage(fragment);
	}

	@Override
	public HandshakeMessage getStartHandshakeMessage() {
		ClientHello message;
		if (session != null) {
			// we want to resume a session
			message = new ClientHello(maxProtocolVersion, new SecureRandom(), session);
		} else {
			message = new ClientHello(maxProtocolVersion, new SecureRandom());
		}

		// store client random for later calculations
		clientRandom = message.getRandom();

		// TODO make this variable
		message.addCipherSuite(CipherSuite.TLS_PSK_WITH_AES_128_CCM_8);
		message.addCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8);
		message.addCompressionMethod(CompressionMethod.NULL);
		setSequenceNumber(message);

		// set current state
		state = message.getMessageType().getCode();

		clientHello = message;

		return message;
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
}

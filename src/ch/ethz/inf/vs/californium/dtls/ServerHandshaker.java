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

import java.io.RandomAccessFile;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;
import ch.ethz.inf.vs.californium.dtls.CertificateRequest.ClientCertificateType;
import ch.ethz.inf.vs.californium.dtls.CertificateRequest.DistinguishedName;
import ch.ethz.inf.vs.californium.dtls.CertificateRequest.HashAlgorithm;
import ch.ethz.inf.vs.californium.dtls.CertificateRequest.SignatureAlgorithm;
import ch.ethz.inf.vs.californium.dtls.CipherSuite.KeyExchangeAlgorithm;

/**
 * Server handshaker does the protocol handshaking from the point of view of a
 * server. It is message-driven by the parent {@link Handshaker} class.
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

	/** The helper class to execute the ECDHE key agreement and key generation. */
	private ECDHECryptography ecdhe;

	/* Store the client's messages for later use */

	/** The client's {@link ClientHello} message. Must always be sent. */
	private ClientHello clientHello = null;

	/**
	 * The client's {@link CertificateMessage}. It is not sent unless client
	 * authentication is required.
	 */
	private CertificateMessage clientCertificate = null;

	/** The client's {@link ClientKeyExchange} message. Must always be sent. */
	private ClientKeyExchange clientKeyExchange = null;

	/**
	 * The client's {@link CertificateVerify} message. It is not sent unless
	 * client authentication is required.
	 */
	private CertificateVerify certificateVerify = null;

	/** The client's {@link Finished} message. Must always be sent. */
	private Finished clientFinished = null;

	// /////////////////////////////////////////////////////////////////

	/**
	 * The message digest to compute the handshake hashes sent in the
	 * {@link Finished} messages.
	 */
	private MessageDigest md;

	/**
	 * The cookie generated to be sent in the {@link HelloVerifyRequest}. Store
	 * it, to verify the client's response.
	 */
	private Cookie cookie;

	// Constructors ///////////////////////////////////////////////////

	public ServerHandshaker(EndpointAddress endpointAddress, X509Certificate[] certificates, DTLSSession session) {
		super(endpointAddress, false, session);
		this.certificates = certificates;
		this.privateKey = loadPrivateKey("C:\\Users\\Jucker\\git\\Californium\\src\\ch\\ethz\\inf\\vs\\californium\\dtls\\ec3.pk8");
		try {
			this.md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public synchronized DTLSFlight processMessage(Record record) {
		if (lastFlight != null) {
			// we already sent the last flight, but the client did not receive
			// it, since we received its finished message again, so we
			// retransmit our last flight
			LOG.info("Received client's finished message again, retransmit the last flight.");
			return lastFlight;
		}

		DTLSFlight flight = null;

		if (!processMessageNext(record)) {
			return null;
		}

		switch (record.getType()) {
		case ALERT:
			record.getFragment();
			break;

		case CHANGE_CIPHER_SPEC:
			record.getFragment();
			setCurrentReadState();
			session.incrementReadEpoch();
			break;

		case HANDSHAKE:
			HandshakeMessage fragment = (HandshakeMessage) record.getFragment();
			switch (fragment.getMessageType()) {
			case CLIENT_HELLO:
				flight = receivedClientHello((ClientHello) fragment);
				break;

			case CERTIFICATE:
				clientCertificate = (CertificateMessage) fragment;
				// TODO verify client's certificate
				break;

			case CLIENT_KEY_EXCHANGE:
				byte[] premasterSecret;
				switch (keyExchange) {
				case PSK:
					premasterSecret = receivedClientKeyExchange((PSKClientKeyExchange) fragment);
					generateKeys(premasterSecret);
					break;

				case EC_DIFFIE_HELLMAN:
					premasterSecret = receivedClientKeyExchange((ECDHClientKeyExchange) fragment);
					generateKeys(premasterSecret);
					break;
					
				case NULL:
					clientKeyExchange = (NULLClientKeyExchange) fragment;
					// TODO what to do here?
					generateKeys(new byte[0]);
					break;
					
				default:
					LOG.severe("Unknown key exchange algorithm: " + keyExchange);
					break;
				}
				break;

			case CERTIFICATE_VERIFY:
				certificateVerify = (CertificateVerify) fragment;
				// TODO verify this
				break;

			case FINISHED:
				flight = receivedClientFinished((Finished) fragment);
				break;

			default:
				LOG.severe("Server received not supported handshake message:\n" + fragment.toString());
				break;
			}

			break;

		default:
			LOG.severe("Server received not supported record:\n" + record.toString());
			break;
		}
		if (flight == null) {
			Record nextMessage = null;
			// check queued message, if it is now their turn
			for (Record queuedMessage : queuedMessages) {
				if (processMessageNext(queuedMessage)) {
					// queuedMessages.remove(queuedMessage);
					nextMessage = queuedMessage;
				}
			}
			if (nextMessage != null) {
				flight = processMessage(nextMessage);
			}
		}
		LOG.info("DTLS Message processed.");
		System.out.println(record.toString());
		return flight;
	}

	/**
	 * Called, when the server received the client's {@link Finished} message.
	 * Generate a {@link DTLSFlight} containing the
	 * {@link ChangeCipherSpecMessage} and {@link Finished} message. This flight
	 * will not be retransmitted, unless we receive the same finish message in
	 * the future; then, we retransmit this flight.
	 * 
	 * @param message
	 *            the client's {@link Finished} message.
	 * @return the server's last {@link DTLSFlight}.
	 */
	private DTLSFlight receivedClientFinished(Finished message) {
		System.out.println("Received Client Finished.");
		if (lastFlight != null) {
			// we already sent this last flight, but the client did not receive
			// it, since we received its finished message again, so we
			// retransmit our last flight
			return lastFlight;
		}

		DTLSFlight flight = new DTLSFlight();

		clientFinished = message;

		// create handshake hash
		if (clientCertificate != null) { // optional
			md.update(clientCertificate.toByteArray());
		}
		
		System.out.println("Client key exchange bytes: " + Arrays.toString(clientKeyExchange.toByteArray()));
		md.update(clientKeyExchange.toByteArray()); // mandatory

		if (certificateVerify != null) { // optional
			md.update(certificateVerify.toByteArray());
		}

		MessageDigest mdWithClientFinished = null;
		try {
			/*
			 * the handshake_messages for the Finished message sent by the
			 * client will be different from that for the Finished message sent
			 * by the server, because the one that is sent second will include
			 * the prior one.
			 */
			mdWithClientFinished = (MessageDigest) md.clone();
			mdWithClientFinished.update(clientFinished.toByteArray());
		} catch (CloneNotSupportedException e) {
			LOG.severe("Clone not supported.");
			e.printStackTrace();
		}

		// Verify client's data
		byte[] handshakeHash = md.digest();
		if (!clientFinished.verifyData(getMasterSecret(), true, handshakeHash)) {
			AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
			flight.addMessage(wrapMessage(alert));
			flight.setRetransmissionNeeded(false);

			return flight;
		}

		// First, send change cipher spec
		ChangeCipherSpecMessage changeCipherSpecMessage = new ChangeCipherSpecMessage();
		flight.addMessage(wrapMessage(changeCipherSpecMessage));
		setCurrentWriteState();
		session.incrementWriteEpoch();

		// Second, send own finished message
		handshakeHash = mdWithClientFinished.digest();
		Finished finished = new Finished(getMasterSecret(), isClient, handshakeHash);
		setSequenceNumber(finished);
		flight.addMessage(wrapMessage(finished));

		state = HandshakeType.FINISHED.getCode();
		session.setActive(true);

		flight.setRetransmissionNeeded(false);
		// store, if we need to retransmit this flight, see rfc6347, 4.2.4.
		lastFlight = flight;
		return flight;

	}

	/**
	 * Called after the server receives a {@link ClientHello} handshake message.
	 * If the message has a {@link Cookie} set, verify it and continue with
	 * {@link ServerHello}, otherwise reply with a {@link HelloVerifyRequest}.
	 * If the session identifier is set, try resuming the previous session.
	 * 
	 * @param message
	 *            the client's {@link ClientHello} message.
	 * @return list of {@link DTLSMessage} that need to be sent after receiving
	 *         a {@link ClientHello}.
	 */
	private DTLSFlight receivedClientHello(ClientHello message) {
		DTLSFlight flight = new DTLSFlight();

		if (message.getCookie().length() > 0 && isValidCookie(message.getCookie())) {
			// client has set a cookie, so it is a response to
			// helloVerifyRequest

			clientHello = message;
			System.out.println("Client hello bytes: " + Arrays.toString(clientHello.toByteArray()));
			md.update(clientHello.toByteArray());

			/*
			 * First, send SERVER HELLO
			 */
			// TODO negotiate version
			ProtocolVersion serverVersion = new ProtocolVersion();

			// store client random
			clientRandom = message.getRandom();
			// server random
			serverRandom = new Random(new SecureRandom());

			SessionId sessionId = new SessionId();
			session.setSessionIdentifier(sessionId);

			// TODO negotiate cipher suite and compression method
			CipherSuite cipherSuite = CipherSuite.SSL_NULL_WITH_NULL_NULL;
			CompressionMethod compressionMethod = CompressionMethod.NULL;
			setCipherSuite(cipherSuite);

			ServerHello serverHello = new ServerHello(serverVersion, serverRandom, sessionId, cipherSuite, compressionMethod, null);
			setSequenceNumber(serverHello);
			flight.addMessage(wrapMessage(serverHello));
			
			System.out.println("Server hello bytes: " + Arrays.toString(serverHello.toByteArray()));
			md.update(serverHello.toByteArray());

			/*
			 * Second, send CERTIFICATE if necessary (not in PSK, see
			 * http://tools.ietf.org/html/rfc4279#section-2)
			 */
			switch (keyExchange) {
			case EC_DIFFIE_HELLMAN:
				CertificateMessage certificate = new CertificateMessage(certificates);
				setSequenceNumber(certificate);
				flight.addMessage(wrapMessage(certificate));
				md.update(certificate.toByteArray());
				break;

			default:
				// NULL and PSK do not require the Certificate message
				break;
			}

			/*
			 * Third, send SERVER KEY EXCHANGE
			 */
			ServerKeyExchange serverKeyExchange = null;
			switch (keyExchange) {
			case EC_DIFFIE_HELLMAN:
				ecdhe = new ECDHECryptography(privateKey);
				serverKeyExchange = new ECDHServerKeyExchange(ecdhe, privateKey, clientRandom, serverRandom);
				break;

			case PSK:
				serverKeyExchange = new PSKServerKeyExchange("TEST");
				break;

			default:
				// NULL does not require the server's key exchange message
				break;
			}
			
			if (serverKeyExchange != null) {
				setSequenceNumber(serverKeyExchange);
				flight.addMessage(wrapMessage(serverKeyExchange));
				md.update(serverKeyExchange.toByteArray());
			}

			/*
			 * Fourth, if required, send CERTIFICATE REQUEST for client, PSK
			 * does not require this message.
			 */
			if (clientAuthenticationRequired && keyExchange != KeyExchangeAlgorithm.PSK) {

				CertificateRequest certificateRequest = new CertificateRequest();

				// TODO make this interchangeable
				certificateRequest.addCertificateType(ClientCertificateType.ECDSA_FIXED_ECDH);
				certificateRequest.addSignatureAlgorithm(new SignatureAndHashAlgorithm(HashAlgorithm.MD5, SignatureAlgorithm.ECDSA));
				certificateRequest.addCertificateAuthority(new DistinguishedName(new byte[6]));

				setSequenceNumber(certificateRequest);
				flight.addMessage(wrapMessage(certificateRequest));
				md.update(certificateRequest.toByteArray());
			}

			/*
			 * Last, send server HELLO DONE
			 */
			ServerHelloDone serverHelloDone = new ServerHelloDone();
			setSequenceNumber(serverHelloDone);
			flight.addMessage(wrapMessage(serverHelloDone));
			System.out.println("Server hello done bytes: " + Arrays.toString(serverHelloDone.toByteArray()));
			md.update(serverHelloDone.toByteArray());

		} else {
			// either first time, or cookies did not match first client hello

			if (message.getSessionId().length() > 0) {
				// client has sent non-empty session id, try resuming it

				if (message.getSessionId() == session.getSessionIdentifier()) {
					// session with specified ID available, resume it

					clientHello = message;
					md.update(clientHello.toByteArray());
					/*
					 * First, send SERVER HELLO with session ID set
					 */
					// TODO set this according to session
					ProtocolVersion serverVersion = new ProtocolVersion();

					// store client random
					clientRandom = message.getRandom();
					// server random
					serverRandom = new Random(new SecureRandom());

					CipherSuite cipherSuite = session.getWriteState().getCipherSuite();
					CompressionMethod compressionMethod = session.getWriteState().getCompressionMethod();
					setCipherSuite(cipherSuite);

					ServerHello serverHello = new ServerHello(serverVersion, serverRandom, session.getSessionIdentifier(), cipherSuite, compressionMethod, null);
					setSequenceNumber(serverHello);
					flight.addMessage(wrapMessage(serverHello));
					md.update(serverHello.toByteArray());

					// TODO make new keys

					/*
					 * Second, send CHANGE CIPHER SPEC
					 */
					ChangeCipherSpecMessage changeCipherSpecMessage = new ChangeCipherSpecMessage();
					flight.addMessage(wrapMessage(changeCipherSpecMessage));
					setCurrentWriteState();
					session.incrementWriteEpoch();

					/*
					 * Third, send FINISHED message
					 */
					MessageDigest md2 = null;
					try {
						// the hash for the client's finished message will
						// contain also the server's finished message, therefore
						// needs to be added to message digest
						md2 = (MessageDigest) md.clone();
					} catch (CloneNotSupportedException e) {
						LOG.severe("Clone not supported.");
						e.printStackTrace();
					}
					byte[] handshakeHash = md.digest();
					Finished finished = new Finished(getMasterSecret(), isClient, handshakeHash);
					setSequenceNumber(finished);
					flight.addMessage(wrapMessage(finished));

					// the new handshake hash with the server's finished message
					// included
					md2.update(finished.toByteArray());
					handshakeHash = md2.digest();

				} else {
					// TODO session ID does not match, start fresh full
					// handshake
				}

			} else {
				// client did not try to resume
				cookie = generateCookie();
				HelloVerifyRequest helloVerifyRequest = new HelloVerifyRequest(new ProtocolVersion(), cookie);
				setSequenceNumber(helloVerifyRequest);
				flight.addMessage(wrapMessage(helloVerifyRequest));
			}
		}
		return flight;
	}

	private byte[] receivedClientKeyExchange(ECDHClientKeyExchange message) {
		clientKeyExchange = message;
		byte[] premasterSecret = ecdhe.getSecret(message.getEncodedPoint()).getEncoded();
		
		return premasterSecret;
	}
	
	private byte[] receivedClientKeyExchange(PSKClientKeyExchange message) {
		clientKeyExchange = message;
		
		// TODO use identity to get right preshared key
		message.getIdentity();
		
		byte[] psk = "preshared secret".getBytes();
		
		return generatePremasterSecretFromPSK(psk);
	}

	private Cookie generateCookie() {
		// TODO as suggested in http://tools.ietf.org/html/rfc6347#section-4.2.1
		// Cookie = HMAC(Secret, Client-IP, Client-Parameters)
		return new Cookie(new Random(new SecureRandom()).getRandomBytes());
	}

	private boolean isValidCookie(Cookie clientCookie) {
		return Arrays.equals(cookie.getCookie(), clientCookie.getCookie());
	}

	@Override
	public DTLSFlight getStartHandshakeMessage() {
		HelloRequest helloRequest = new HelloRequest();
		setSequenceNumber(helloRequest);

		DTLSFlight flight = new DTLSFlight();
		flight.addMessage(wrapMessage(helloRequest));
		return flight;
	}

	/**
	 * Loads the private key from a file encoded according to the PKCS #8
	 * standard.
	 * 
	 * @param filename
	 *            the filename where the private key resides.
	 * @return the private key.
	 */
	private PrivateKey loadPrivateKey(String filename) {
		PrivateKey privateKey = null;
		try {
			RandomAccessFile raf = new RandomAccessFile(filename, "r");
			byte[] encodedKey = new byte[(int) raf.length()];

			raf.readFully(encodedKey);
			raf.close();

			PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(encodedKey);
			/*
			 * See
			 * http://docs.oracle.com/javase/7/docs/technotes/guides/security
			 * /StandardNames.html#KeyFactory
			 */
			KeyFactory keyF = KeyFactory.getInstance("EC");
			privateKey = keyF.generatePrivate(kspec);

		} catch (Exception e) {
			LOG.severe("Could not load private key: " + filename);
			e.printStackTrace();
		}
		return privateKey;
	}

}

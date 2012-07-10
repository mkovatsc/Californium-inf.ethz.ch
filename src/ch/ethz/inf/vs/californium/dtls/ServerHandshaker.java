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
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	// Members ////////////////////////////////////////////////////////

	/** Is the client required to authenticate itself? */
	private boolean clientAuthenticationRequired;

	/** The server's certificate. */
	private X509Certificate[] certificates;

	/** The server's private key. */
	private PrivateKey privateKey;

	private List<CipherSuite> supportedCipherSuites;

	/*
	 * Store all the the message which can possibly be sent by the client. We
	 * need these to compute the handshake hash.
	 */
	/** The client's {@link ClientHello}. Mandatory. */
	protected ClientHello clientHello;
	/** The client's {@link CertificateMessage}. Optional. */
	protected CertificateMessage clientCertificate = null;
	/** The client's {@link ClientKeyExchange}. mandatory. */
	protected ClientKeyExchange clientKeyExchange;
	/** The client's {@link CertificateVerify}. Optional. */
	protected CertificateVerify certificateVerify = null;
	/** The client's {@link Finished} message. Mandatory. */
	protected Finished clientFinished;

	/**
	 * The cookie generated to be sent in the {@link HelloVerifyRequest}. Store
	 * it, to verify the client's response.
	 */
	private Cookie cookie;

	// Constructors ///////////////////////////////////////////////////

	/**
	 * 
	 * @param endpointAddress
	 *            the peer's address.
	 * @param certificates
	 *            the server's certificate chain.
	 * @param session
	 *            the {@link DTLSSession}.
	 */
	public ServerHandshaker(EndpointAddress endpointAddress, X509Certificate[] certificates, DTLSSession session) {
		super(endpointAddress, false, session);
		this.certificates = certificates;
		this.privateKey = loadPrivateKey("C:\\Users\\Jucker\\git\\Californium\\src\\ch\\ethz\\inf\\vs\\californium\\dtls\\ec3.pk8");

		this.supportedCipherSuites = new ArrayList<CipherSuite>();
		this.supportedCipherSuites.add(CipherSuite.SSL_NULL_WITH_NULL_NULL);
		this.supportedCipherSuites.add(CipherSuite.TLS_PSK_WITH_AES_128_CCM_8);
		this.supportedCipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8);
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
		if (lastFlight != null) {
			return null;
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
		// store, if we need to retransmit this flight, see
		// http://tools.ietf.org/html/rfc6347#section-4.2.4
		lastFlight = flight;
		return flight;

	}

	/**
	 * Called after the server receives a {@link ClientHello} handshake message.
	 * If the message has a {@link Cookie} set, verify it and continue with
	 * {@link ServerHello}, otherwise reply with a {@link HelloVerifyRequest}.
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
			ProtocolVersion serverVersion = negotiateProtocolVersion(clientHello.getClientVersion());

			// store client random
			clientRandom = message.getRandom();
			// server random
			serverRandom = new Random(new SecureRandom());

			SessionId sessionId = new SessionId();
			session.setSessionIdentifier(sessionId);

			CipherSuite cipherSuite = negotiateCipherSuite(clientHello.getCipherSuites());
			setCipherSuite(cipherSuite);

			CompressionMethod compressionMethod = CompressionMethod.NULL;
			setCompressionMethod(compressionMethod);

			ServerHello serverHello = new ServerHello(serverVersion, serverRandom, sessionId, cipherSuite, compressionMethod, null);
			setSequenceNumber(serverHello);
			flight.addMessage(wrapMessage(serverHello));
			md.update(serverHello.toByteArray());

			System.out.println("Server hello bytes: " + Arrays.toString(serverHello.toByteArray()));

			/*
			 * Second, send CERTIFICATE, if necessary
			 */
			CertificateMessage certificate = null;
			switch (keyExchange) {
			case EC_DIFFIE_HELLMAN:
				// TODO make this variable
				certificate = new CertificateMessage(certificates, true);

				break;

			default:
				// NULL and PSK do not require the Certificate message
				// See http://tools.ietf.org/html/rfc4279#section-2
				break;
			}
			if (certificate != null) {
				setSequenceNumber(certificate);
				flight.addMessage(wrapMessage(certificate));
				md.update(certificate.toByteArray());
			}

			/*
			 * Third, send SERVER KEY EXCHANGE, if necessary
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
				
				certificateRequest.addCertificateType(ClientCertificateType.ECDSA_FIXED_ECDH);
				certificateRequest.addSignatureAlgorithm(new SignatureAndHashAlgorithm(HashAlgorithm.MD5, SignatureAlgorithm.ECDSA));
				certificateRequest.addCertificateAuthority(new DistinguishedName(new byte[6]));

				setSequenceNumber(certificateRequest);
				flight.addMessage(wrapMessage(certificateRequest));
				md.update(certificateRequest.toByteArray());
			}

			/*
			 * Last, send SERVER HELLO DONE
			 */
			ServerHelloDone serverHelloDone = new ServerHelloDone();
			setSequenceNumber(serverHelloDone);
			flight.addMessage(wrapMessage(serverHelloDone));
			md.update(serverHelloDone.toByteArray());

			System.out.println("Server hello done bytes: " + Arrays.toString(serverHelloDone.toByteArray()));

		} else {
			// either first time, or cookies did not match
			cookie = generateCookie();
			HelloVerifyRequest helloVerifyRequest = new HelloVerifyRequest(new ProtocolVersion(), cookie);
			setSequenceNumber(helloVerifyRequest);
			flight.addMessage(wrapMessage(helloVerifyRequest));
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

	/**
	 * 
	 * @param clientCookie
	 *            the cookie in the client's hello message.
	 * @return <code>true</code> if the cookie matches, <code>false</code>
	 *         otherwise.
	 */
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
	 * Negotiates the version to be used. It will return the lower of that
	 * suggested by the client in the client hello and the highest supported by
	 * the server.
	 * 
	 * @param clientVersion
	 *            the suggested version by the client.
	 * @return the version to be used in the handshake.
	 */
	private ProtocolVersion negotiateProtocolVersion(ProtocolVersion clientVersion) {
		return new ProtocolVersion();
	}

	/**
	 * Selects one of the client's proposed cipher suites.
	 * 
	 * @param cipherSuites
	 *            the client's cipher suites.
	 * @return The single cipher suite selected by the server from the list.
	 */
	private CipherSuite negotiateCipherSuite(List<CipherSuite> cipherSuites) {
		// the client's list is sorted by preference
		for (CipherSuite cipherSuite : cipherSuites) {
			if (supportedCipherSuites.contains(cipherSuite)) {
				return cipherSuite;
			}
		}
		return CipherSuite.SSL_NULL_WITH_NULL_NULL;
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

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
import java.security.interfaces.ECPrivateKey;
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
	/** The client's {@link ClientHello} raw byte representation. Used for computing the handshake hash. */
	private byte[] clientHelloBytes;
	/** The client's {@link CertificateMessage}. Optional. */
	protected CertificateMessage clientCertificate = null;
	/** The client's {@link ClientKeyExchange}. mandatory. */
	protected ClientKeyExchange clientKeyExchange;
	/** The client's {@link CertificateVerify}. Optional. */
	protected CertificateVerify certificateVerify = null;
	/** The client's {@link Finished} message. Mandatory. */
	protected Finished clientFinished;

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
		// FIXME
		this.privateKey = loadPrivateKey("C:\\Users\\Jucker\\git\\Californium\\cf-server\\src\\main\\resources\\certs\\ec.pk8");
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
			return flight;
		}

		switch (record.getType()) {
		case ALERT:
			AlertMessage alert = (AlertMessage) record.getFragment();
			switch (alert.getDescription()) {
			case CLOSE_NOTIFY:
				flight = closeConnection();
				break;

			default:
				break;
			}
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
				clientHelloBytes = record.getFragmentBytes();
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
					premasterSecret = receivedClientKeyExchange((NULLClientKeyExchange) fragment);
					generateKeys(premasterSecret);
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

		/* 
		 * First, send ChangeCipherSpec
		 */
		ChangeCipherSpecMessage changeCipherSpecMessage = new ChangeCipherSpecMessage();
		flight.addMessage(wrapMessage(changeCipherSpecMessage));
		setCurrentWriteState();
		session.incrementWriteEpoch();

		/* 
		 * Second, send Finished message
		 */
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
	 * If the message has a {@link Cookie} set, verify it and prepare the next
	 * flight (mandatory messages depend on the cipher suite / key exchange
	 * algorithm). Mandatory messages are ServerHello and ServerHelloDone; see
	 * <a href="http://tools.ietf.org/html/rfc5246#section-7.3">Figure 1.
	 * Message flow for a full handshake</a> for details about the messages in
	 * the next flight.
	 * 
	 * @param message
	 *            the client's hello message.
	 * @return the server's next flight to be sent.
	 */
	private DTLSFlight receivedClientHello(ClientHello message) {
		DTLSFlight flight = new DTLSFlight();

		if (message.getCookie().length() > 0 && isValidCookie(message)) {
			// client has set a cookie, so it is a response to
			// helloVerifyRequest
			
			
			// store the message and update the handshake hash
			clientHello = message;
			md.update(clientHelloBytes);

			/*
			 * First, send ServerHello (mandatory)
			 */
			ProtocolVersion serverVersion = negotiateProtocolVersion(clientHello.getClientVersion());

			// store client and server random
			clientRandom = message.getRandom();
			serverRandom = new Random(new SecureRandom());

			SessionId sessionId = new SessionId();
			session.setSessionIdentifier(sessionId);

			CipherSuite cipherSuite = negotiateCipherSuite(clientHello.getCipherSuites());
			setCipherSuite(cipherSuite);
			
			// currently only NULL compression supported, no negotiation needed
			CompressionMethod compressionMethod = CompressionMethod.NULL;
			setCompressionMethod(compressionMethod);

			// TODO add extensions to ServerHello
			ServerHello serverHello = new ServerHello(serverVersion, serverRandom, sessionId, cipherSuite, compressionMethod, null);
			setSequenceNumber(serverHello);
			flight.addMessage(wrapMessage(serverHello));
			// update the handshake hash
			md.update(serverHello.toByteArray());


			/*
			 * Second, send Certificate (if required by key exchange algorithm)
			 */
			CertificateMessage certificate = null;
			switch (keyExchange) {
			case EC_DIFFIE_HELLMAN:
				certificate = new CertificateMessage(certificates, useRawPublicKey);
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
			 * Third, send ServerKeyExchange (if required by key exchange algorithm)
			 */
			ServerKeyExchange serverKeyExchange = null;
			switch (keyExchange) {
			case EC_DIFFIE_HELLMAN:
				ecdhe = new ECDHECryptography((ECPrivateKey) privateKey);
				serverKeyExchange = new ECDHServerKeyExchange(ecdhe, privateKey, clientRandom, serverRandom);
				break;

			case PSK:
				// serverKeyExchange = new PSKServerKeyExchange("TEST");
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
			 * Fourth, send CertificateRequest for client (if required)
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
			 * Last, send ServerHelloDone (mandatory)
			 */
			ServerHelloDone serverHelloDone = new ServerHelloDone();
			setSequenceNumber(serverHelloDone);
			flight.addMessage(wrapMessage(serverHelloDone));
			md.update(serverHelloDone.toByteArray());
			
		} else {
			// either first time, or cookies did not match
			HelloVerifyRequest helloVerifyRequest = new HelloVerifyRequest(new ProtocolVersion(), generateCookie(message));
			setSequenceNumber(helloVerifyRequest);
			flight.addMessage(wrapMessage(helloVerifyRequest));
		}
		return flight;
	}
	
	/**
	 * Generates the premaster secret by taking the client's public key and
	 * running the ECDHE key agreement.
	 * 
	 * @param message
	 *            the client's key exchange message.
	 * @return the premaster secret
	 */
	private byte[] receivedClientKeyExchange(ECDHClientKeyExchange message) {
		clientKeyExchange = message;
		byte[] premasterSecret = ecdhe.getSecret(message.getEncodedPoint()).getEncoded();

		return premasterSecret;
	}
	
	/**
	 * Retrieves the preshared key from the identity hint and then generates the
	 * premaster secret.
	 * 
	 * @param message
	 *            the client's key exchange message.
	 * @return the premaster secret
	 */
	private byte[] receivedClientKeyExchange(PSKClientKeyExchange message) {
		clientKeyExchange = message;

		// use the client's PSK identity to get right preshared key
		String identity = message.getIdentity();
		byte[] psk = sharedKeys.get(identity);

		return generatePremasterSecretFromPSK(psk);
	}
	
	/**
	 * Returns an empty premaster secret.
	 * 
	 * @param message
	 *            the client's key exchange message.
	 * @return the premaster secret
	 */
	private byte[] receivedClientKeyExchange(NULLClientKeyExchange message) {
		clientKeyExchange = message;

		// by current assumption we take an empty premaster secret
		// to compute the master secret and the resulting keys
		return new byte[] {};
	}
	
	/**
	 * Generates a cookie in such a way that they can be verified without
	 * retaining any per-client state on the server.
	 * <pre>Cookie = HMAC(Secret, Client-IP, Client-Parameters)</pre> as suggested <a
	 * href="http://tools.ietf.org/html/rfc6347#section-4.2.1">here</a>.
	 * 
	 * @return the cookie generated from the client's parameters.
	 */
	private Cookie generateCookie(ClientHello clientHello) {
		
		MessageDigest md;
		byte[] cookie = null;
		
		try {
			md = MessageDigest.getInstance("SHA-256");
			
			// Cookie = HMAC(Secret, Client-IP, Client-Parameters)
			byte[] secret = "generate cookie".getBytes();
			
			// Client-IP
			md.update(endpointAddress.toString().getBytes());
			
			// Client-Parameters
			md.update((byte) clientHello.getClientVersion().getMajor());
			md.update((byte) clientHello.getClientVersion().getMinor());
			md.update(clientHello.getRandom().getRandomBytes());
			md.update(clientHello.getSessionId().getSessionId());
			md.update(CipherSuite.listToByteArray(clientHello.getCipherSuites()));
			md.update(CompressionMethod.listToByteArray(clientHello.getCompressionMethods()));

			byte[] data = md.digest();

			cookie = Handshaker.doHMAC(md, secret, data);
		} catch (NoSuchAlgorithmException e) {
			LOG.info("Could not instantiate message digest algorithm.");
			e.printStackTrace();
		}
		if (cookie == null) {
			return new Cookie(new Random(new SecureRandom()).getRandomBytes());
		} else {
			return new Cookie(cookie);
		}
	}

	/**
	 * Checks whether the Cookie in the client's hello message matches the
	 * expected cookie generated from the client's parameters.
	 * 
	 * @param clientHello
	 *            the client's hello message containing the cookie.
	 * @return <code>true</code> if the cookie matches, <code>false</code>
	 *         otherwise.
	 */
	private boolean isValidCookie(ClientHello clientHello) {
		Cookie expected = generateCookie(clientHello);
		Cookie actual = clientHello.getCookie();
		boolean valid = Arrays.equals(expected.getCookie(), actual.getCookie());
		
		if (!valid) {
			LOG.info("Client's cookie did not match expected cookie:\nExpected: " + Arrays.toString(expected.getCookie()) + "\nActual: " + Arrays.toString(actual.getCookie()));
		}
		
		return valid;
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
	 * @return The single cipher suite selected by the server from the list
	 *         which will be used after handshake completion.
	 */
	private CipherSuite negotiateCipherSuite(List<CipherSuite> cipherSuites) {
		// the client's list is sorted by preference
		for (CipherSuite cipherSuite : cipherSuites) {
			if (supportedCipherSuites.contains(cipherSuite)) {
				return cipherSuite;
			}
		}
		// if none of the client's proposed cipher suites matches
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

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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;

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

	/** The server's public key from its certificate */
	private PublicKey serverPublicKey;

	/** The server's ephemeral public key, used for key agreement */
	private ECPublicKey ephemeralServerPublicKey;

	/** The client's hello handshake message. Store it, to add the cookie in the second flight. */
	protected ClientHello clientHello = null;

	/*
	 * Store all the the message which can possibly be sent by the server. We
	 * need these to compute the handshake hash.
	 */
	/** The server's {@link ServerHello}. Mandatory. */
	protected ServerHello serverHello;
	/** The server's {@link CertificateMessage}. Optional. */
	protected CertificateMessage serverCertificate = null;
	/** The server's {@link CertificateRequest}. Optional. */
	protected CertificateRequest certificateRequest = null;
	/** The server's {@link ServerKeyExchange}. Optional. */
	protected ServerKeyExchange serverKeyExchange = null;
	/** The server's {@link ServerHelloDone}. Mandatory. */
	protected ServerHelloDone serverHelloDone;

	/** The hash of all received handshake messages sent in the finished message. */
	protected byte[] handshakeHash = null;

	// Constructors ///////////////////////////////////////////////////

	/**
	 * 
	 * 
	 * @param endpointAddress
	 *            the endpoint address
	 * @param message
	 *            the message
	 * @param session
	 *            the session
	 */
	public ClientHandshaker(EndpointAddress endpointAddress, Message message, DTLSSession session) {
		super(endpointAddress, true, session);
		this.message = message;
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public synchronized DTLSFlight processMessage(Record record) {
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
			case HELLO_REQUEST:
				flight = receivedHelloRequest((HelloRequest) fragment);
				break;

			case HELLO_VERIFY_REQUEST:
				flight = receivedHelloVerifyRequest((HelloVerifyRequest) fragment);
				break;

			case SERVER_HELLO:
				receivedServerHello((ServerHello) fragment);
				break;

			case CERTIFICATE:
				receivedServerCertificate((CertificateMessage) fragment);
				break;

			case SERVER_KEY_EXCHANGE:

				switch (keyExchange) {
				case EC_DIFFIE_HELLMAN:
					flight = receivedServerKeyExchange((ECDHServerKeyExchange) fragment);
					break;

				case PSK:
					serverKeyExchange = (PSKServerKeyExchange) fragment;
					break;
					
				case NULL:
					LOG.info("Received unexpected ServerKeyExchange message in NULL key exchange mode.");
					break;

				default:
					LOG.severe("Not supported server key exchange algorithm: " + keyExchange);
					break;
				}
				break;

			case CERTIFICATE_REQUEST:
				// save for later, will be handled by server hello done
				certificateRequest = (CertificateRequest) fragment;
				break;

			case SERVER_HELLO_DONE:
				flight = receivedServerHelloDone((ServerHelloDone) fragment);
				break;

			case FINISHED:
				flight = receivedServerFinished((Finished) fragment);
				break;

			default:
				LOG.severe("Client received not supported handshake message:\n" + fragment.toString());
				break;
			}
			break;

		default:
			LOG.severe("Client received not supported record:\n" + record.toString());
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
	 * Called when the client received the server's finished message. If the
	 * data can be verified, encrypted application data can be sent.
	 * 
	 * @param message
	 *            the {@link Finished} message.
	 * @return the list
	 */
	private DTLSFlight receivedServerFinished(Finished message) {
		DTLSFlight flight = new DTLSFlight();

		if (!message.verifyData(getMasterSecret(), false, handshakeHash)) {

			AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
			flight.addMessage(wrapMessage(alert));
			flight.setRetransmissionNeeded(false);

			return flight;
		}

		state = HandshakeType.FINISHED.getCode();
		session.setActive(true);

		// received server's Finished message, now able to send encrypted
		// message
		ApplicationMessage applicationMessage = new ApplicationMessage(this.message.toByteArray());

		flight.addMessage(wrapMessage(applicationMessage));
		// application data is not retransmitted
		flight.setRetransmissionNeeded(false);

		return flight;
	}

	/**
	 * Used by the server to kickstart negotiations.
	 * 
	 * @param message
	 *            the hello request message
	 */
	private DTLSFlight receivedHelloRequest(HelloRequest message) {
		if (state < HandshakeType.HELLO_REQUEST.getCode()) {
			return getStartHandshakeMessage();
		} else {
			// already started with handshake, drop this message
			return null;
		}
	}

	/**
	 * A {@link HelloVerifyRequest} is sent by the server upon the arrival of
	 * the client's {@link ClientHello}. It is sent by the server to prevent
	 * flooding of a client. The client answers with the same
	 * {@link ClientHello} as before with the additional cookie.
	 * 
	 * @param message
	 *            the server's {@link HelloVerifyRequest}.
	 * @return {@link ClientHello} with server's {@link Cookie} set.
	 */
	private DTLSFlight receivedHelloVerifyRequest(HelloVerifyRequest message) {

		clientHello.setCookie(message.getCookie());
		setSequenceNumber(clientHello);

		DTLSFlight flight = new DTLSFlight();
		flight.addMessage(wrapMessage(clientHello));

		return flight;
	}

	/**
	 * Stores the negotiated security parameters.
	 * 
	 * @param message
	 *            the {@link ServerHello} message.
	 */
	private void receivedServerHello(ServerHello message) {
		if (serverHello != null && (message.getMessageSeq() == serverHello.getMessageSeq())) {
			// received duplicate version (retransmission), discard it
			return;
		}
		serverHello = message;

		// store the negotiated values
		usedProtocol = message.getServerVersion();
		serverRandom = message.getRandom();
		session.setSessionIdentifier(message.getSessionId());
		setCipherSuite(message.getCipherSuite());
		setCompressionMethod(message.getCompressionMethod());
	}

	/**
	 * Unless a anonymous cipher suite is used, the server always sends a
	 * {@link CertificateMessage}. The client verifies it and stores the
	 * server's public key.
	 * 
	 * @param message
	 *            the server's {@link CertificateMessage}.
	 */
	private void receivedServerCertificate(CertificateMessage message) {
		if (serverCertificate != null && (serverCertificate.getMessageSeq() == message.getMessageSeq())) {
			// discard duplicate message
			return;
		}

		serverCertificate = message;
		serverPublicKey = serverCertificate.getPublicKey();
	}

	/**
	 * The ServerKeyExchange message is sent by the server only when the server
	 * {@link CertificateMessage} (if sent) does not contain enough data to
	 * allow the client to exchange a premaster secret. Used when the key
	 * exchange is ECDH. The client tries to verify the server's signature and
	 * on success prepares the ECDH key agreement.
	 * 
	 * @param message
	 *            the server's {@link ServerKeyExchange} message.
	 * @return {@link AlertMessage} if the message can't be verified.
	 */
	private DTLSFlight receivedServerKeyExchange(ECDHServerKeyExchange message) {
		if (serverKeyExchange != null && (serverKeyExchange.getMessageSeq() == message.getMessageSeq())) {
			// discard duplicate message
			return null;
		}

		serverKeyExchange = message;
		if (message.verifySignature(serverPublicKey, clientRandom, serverRandom)) {
			ephemeralServerPublicKey = message.getPublicKey(((ECPublicKey) serverPublicKey).getParams());
			ecdhe = new ECDHECryptography(ephemeralServerPublicKey.getParams());
		} else {
			LOG.severe("The server's ECDHE key exchange message's signature could not be verified.");
			AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
			DTLSFlight flight = new DTLSFlight();
			flight.addMessage(wrapMessage(alert));
			flight.setRetransmissionNeeded(false);
			
			return flight;
		}
		return null;
	}

	/**
	 * The ServerHelloDone message is sent by the server to indicate the end of
	 * the ServerHello and associated messages. The client prepares all
	 * necessary messages (depending on server's previous flight) and returns
	 * the next flight.
	 * 
	 * @return the client's next flight to be sent.
	 */
	private DTLSFlight receivedServerHelloDone(ServerHelloDone message) {
		DTLSFlight flight = new DTLSFlight();
		if (serverHelloDone != null && (serverHelloDone.getMessageSeq() == message.getMessageSeq())) {
			// discard duplicate message
			return flight;
		}
		serverHelloDone = message;

		/*
		 * All possible handshake messages sent in this flight. Used to compute
		 * handshake hash.
		 */
		CertificateMessage clientCertificate = null;
		ClientKeyExchange clientKeyExchange = null;
		CertificateVerify certificateVerify = null;

		/*
		 * First, if required by server, send client certificate.
		 */
		if (certificateRequest != null) {
			// TODO load the client's certificate according to the allowed
			// parameters in the CertificateRequest
			clientCertificate = new CertificateMessage(null, false);
			setSequenceNumber(clientCertificate);

			flight.addMessage(wrapMessage(clientCertificate));
		}

		/*
		 * Second, send ClientKeyExchange as specified by the key exchange
		 * algorithm.
		 */
		byte[] premasterSecret;
		switch (keyExchange) {
		case EC_DIFFIE_HELLMAN:
			clientKeyExchange = new ECDHClientKeyExchange(ecdhe.getPublicKey());
			premasterSecret = ecdhe.getSecret(ephemeralServerPublicKey).getEncoded();

			generateKeys(premasterSecret);

			break;

		case PSK:
			// TODO get the identity according to the server
			String identity = "TEST";
			clientKeyExchange = new PSKClientKeyExchange(identity);
			byte[] psk = sharedKeys.get(identity);

			premasterSecret = generatePremasterSecretFromPSK(psk);
			generateKeys(premasterSecret);

			break;

		case NULL:
			clientKeyExchange = new NULLClientKeyExchange();

			// We assume, that the premaster secret is empty
			generateKeys(new byte[] {});
			break;

		default:
			LOG.severe("Unknown key exchange algorithm: " + keyExchange);
			break;
		}
		setSequenceNumber(clientKeyExchange);
		flight.addMessage(wrapMessage(clientKeyExchange));

		/*
		 * Third, send CertificateVerify message if necessary.
		 */
		if (certificateRequest != null) {
			certificateVerify = new CertificateVerify(null);
			setSequenceNumber(certificateVerify);
			flight.addMessage(wrapMessage(certificateVerify));
		}

		/*
		 * Fourth, send a change cipher spec.
		 */
		ChangeCipherSpecMessage changeCipherSpecMessage = new ChangeCipherSpecMessage();
		flight.addMessage(wrapMessage(changeCipherSpecMessage));
		setCurrentWriteState();
		session.incrementWriteEpoch();

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
				md.update(clientCertificate.toByteArray());
			}
			md.update(clientKeyExchange.toByteArray());

			if (certificateVerify != null) {
				md.update(certificateVerify.toByteArray());
			}

			MessageDigest mdWithClientFinished = null;
			try {
				mdWithClientFinished = (MessageDigest) md.clone();
			} catch (CloneNotSupportedException e) {
				LOG.severe("Clone not supported.");
				e.printStackTrace();
			}

			handshakeHash = md.digest();
			Finished finished = new Finished(getMasterSecret(), isClient, handshakeHash);
			setSequenceNumber(finished);

			// compute handshake hash with client's finished message also
			// included, used for server's finished message
			mdWithClientFinished.update(finished.toByteArray());
			handshakeHash = mdWithClientFinished.digest();

			flight.addMessage(wrapMessage(finished));

		} catch (NoSuchAlgorithmException e) {
			LOG.severe("No such Message Digest Algorithm available.");
			e.printStackTrace();
		}

		return flight;

	}

	@Override
	public DTLSFlight getStartHandshakeMessage() {
		ClientHello message = new ClientHello(maxProtocolVersion, new SecureRandom());

		// store client random for later calculations
		clientRandom = message.getRandom();

		// the mandatory to implement ciphersuites
		message.addCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8);
		message.addCipherSuite(CipherSuite.TLS_PSK_WITH_AES_128_CCM_8);
		message.addCompressionMethod(CompressionMethod.NULL);
		setSequenceNumber(message);

		// set current state
		state = message.getMessageType().getCode();

		// store for later calculations
		clientHello = message;
		DTLSFlight flight = new DTLSFlight();
		flight.addMessage(wrapMessage(message));

		return flight;
	}

}

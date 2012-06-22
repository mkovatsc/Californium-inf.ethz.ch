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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.dtls.CipherSuite.KeyExchangeAlgorithm;
import ch.ethz.inf.vs.californium.util.ByteArrayUtils;

public abstract class Handshaker {

	// Logging ////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(Handshaker.class.getName());

	// Static members /////////////////////////////////////////////////

	public final static String MASTER_SECRET_LABEL = "master secret";

	public final static String KEY_EXPANSION_LABEL = "key expansion";

	public final static String CLIENT_FINISHED_LABEL = "client finished";

	public final static String SERVER_FINISHED_LABEL = "server finished";

	public final static String TEST_LABEL = "test label";
	
	public final static String TEST_LABEL_2 = "PRF Testvector";

	// Members ////////////////////////////////////////////////////////

	/**
	 * Indicates, whether the handshake protocol is performed from the client's
	 * side or the server's.
	 */
	protected boolean isClient;

	protected int state = -1;

	protected EndpointAddress endpointAddress;

	protected ProtocolVersion usedProtocol;
	protected Random clientRandom;
	protected Random serverRandom;
	protected CipherSuite cipherSuite;
	protected CompressionMethod compressionMethod;

	protected KeyExchangeAlgorithm keyExchange;

	private byte[] masterSecret;

	private SecretKey clientWriteMACKey;
	private SecretKey serverWriteMACKey;

	private IvParameterSpec clientWriteIV;
	private IvParameterSpec serverWriteIV;

	private SecretKey clientWriteKey;
	private SecretKey serverWriteKey;

	protected DTLSSession session = null;

	/**
	 * The current sequence number (in the handshake message called message_seq)
	 * for this handshake.
	 */
	protected int sequenceNumber = 0;

	/** The next expected handshake message sequence number. */
	protected int nextReceiveSeq = 0;

	/** The CoAP {@link Message} that needs encryption. */
	protected Message message;

	/** Queue for messages, that can not yet be processed. */
	protected Collection<Record> queuedMessages;

	/**
	 * The last flight that is sent during this handshake, will not be
	 * retransmitted unless the peer retransmits its last flight.
	 */
	protected DTLSFlight lastFlight = null;

	// Constructor ////////////////////////////////////////////////////

	/**
	 * 
	 * @param peerAddress
	 * @param isClient
	 * @param session
	 */
	public Handshaker(EndpointAddress peerAddress, boolean isClient, DTLSSession session) {
		this.endpointAddress = peerAddress;
		this.isClient = isClient;
		this.session = session;
		queuedMessages = new HashSet<Record>();
	}

	// Abstract Methods ///////////////////////////////////////////////

	/**
	 * Processes the handshake message according to its {@link HandshakeType}
	 * and reacts according to the protocol specification.
	 * 
	 * @param message
	 *            the received {@link HandshakeMessage}.
	 * @return the list all handshake messages that need to be sent triggered by
	 *         this message.
	 */
	public abstract DTLSFlight processMessage(Record message);

	/**
	 * Gets the handshake flight which needs to be sent first to initiate
	 * handshake. This differs from client side to server side.
	 * 
	 * @return the handshake message to start off the handshake protocol.
	 */
	public abstract DTLSFlight getStartHandshakeMessage();

	// Methods ////////////////////////////////////////////////////////

	protected void generateKeys(byte[] premasterSecret) {
		masterSecret = generateMasterSecret(premasterSecret);

		/*
		 * See http://tools.ietf.org/html/rfc5246#section-6.3:
		 * 
		 * key_block = PRF(SecurityParameters.master_secret, "key expansion",
		 * SecurityParameters.server_random + SecurityParameters.client_random);
		 */

		byte[] data = doPRF(masterSecret, KEY_EXPANSION_LABEL, ByteArrayUtils.concatenate(serverRandom.getRandomBytes(), clientRandom.getRandomBytes()));

		/*
		 * Create keys as suggested in
		 * http://tools.ietf.org/html/rfc5246#section-6.3
		 * 
		 * client_write_MAC_key[SecurityParameters.mac_key_length]
		 * server_write_MAC_key[SecurityParameters.mac_key_length]
		 * client_write_key[SecurityParameters.enc_key_length]
		 * server_write_key[SecurityParameters.enc_key_length]
		 * client_write_IV[SecurityParameters.fixed_iv_length]
		 * server_write_IV[SecurityParameters.fixed_iv_length]
		 */

		// See http://www.ietf.org/mail-archive/web/tls/current/msg08445.html
		// for values (in octets!)
		int mac_key_length = 0;
		int enc_key_length = 16;
		int fixed_iv_length = 4;

		clientWriteMACKey = new SecretKeySpec(data, 0, mac_key_length, "Mac");
		serverWriteMACKey = new SecretKeySpec(data, mac_key_length, mac_key_length, "Mac");

		clientWriteKey = new SecretKeySpec(data, 2 * mac_key_length, enc_key_length, "AES");
		serverWriteKey = new SecretKeySpec(data, (2 * mac_key_length) + enc_key_length, enc_key_length, "AES");

		clientWriteIV = new IvParameterSpec(data, (2 * mac_key_length) + (2 * enc_key_length), fixed_iv_length);
		serverWriteIV = new IvParameterSpec(data, (2 * mac_key_length) + (2 * enc_key_length) + fixed_iv_length, fixed_iv_length);

	}

	private byte[] generateMasterSecret(byte[] premasterSecret) {

		/*
		 * See http://tools.ietf.org/html/rfc5246#section-8.1
		 * 
		 * master_secret = PRF(pre_master_secret, "master secret",
		 * ClientHello.random + ServerHello.random) [0..47]
		 */

		byte[] randomSeed = ByteArrayUtils.concatenate(clientRandom.getRandomBytes(), serverRandom.getRandomBytes());
		return doPRF(premasterSecret, MASTER_SECRET_LABEL, randomSeed);
	}

	/**
	 * See <a href="http://tools.ietf.org/html/rfc4279#section-2">RFC 4279</a>:
	 * The premaster secret is formed as follows: if the PSK is N octets long,
	 * concatenate a uint16 with the value N, N zero octets, a second uint16
	 * with the value N, and the PSK itself.
	 * 
	 * @param psk
	 *            the preshared key as byte array.
	 * @return the premaster secret.
	 */
	protected byte[] generatePremasterSecretFromPSK(byte[] psk) {
		/*
		 * What we are building is the following with length fields in between:
		 * struct { opaque other_secret<0..2^16-1>; opaque psk<0..2^16-1>; };
		 */
		int length = psk.length;

		byte[] lengthField = new byte[2];
		lengthField[0] = (byte) (length >> 8);
		lengthField[1] = (byte) (length);

		byte[] zero = ByteArrayUtils.padArray(new byte[0], (byte) 0x00, length);

		byte[] premasterSecret = ByteArrayUtils.concatenate(lengthField, ByteArrayUtils.concatenate(zero, ByteArrayUtils.concatenate(lengthField, psk)));

		return premasterSecret;
	}

	/**
	 * Does the Pseudorandom function as defined in <a
	 * href="http://tools.ietf.org/html/rfc5246#section-5">RFC 5246</a>.
	 * 
	 * @param secret
	 *            the secret
	 * @param label
	 *            the label
	 * @param seed
	 *            the seed
	 * @return the byte[]
	 */
	public static byte[] doPRF(byte[] secret, String label, byte[] seed) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");

			switch (label) {
			case MASTER_SECRET_LABEL:
				// The master secret is always 48 bytes long, see
				// http://tools.ietf.org/html/rfc5246#section-8.1
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 48);

			case KEY_EXPANSION_LABEL:
				// The most key material required is 128 bytes, see
				// http://tools.ietf.org/html/rfc5246#section-6.3
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 128);

			case CLIENT_FINISHED_LABEL:
			case SERVER_FINISHED_LABEL:
				// The verify data is always 12 bytes long, see
				// http://tools.ietf.org/html/rfc5246#section-7.4.9
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 12);

			case TEST_LABEL:
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 100);
				
			case TEST_LABEL_2:
				md = MessageDigest.getInstance("SHA-1");
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 104);

			default:
				LOG.severe("Unknwon label: " + label);
				return null;
			}
		} catch (NoSuchAlgorithmException e) {
			LOG.severe("Message digest algorithm not available.");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Performs the secret expansion as described in <a
	 * href="http://tools.ietf.org/html/rfc5246#section-5">RFC 5246</a>.
	 * 
	 * @param md
	 *            the cryptographic hash function.
	 * @param secret
	 *            the secret.
	 * @param data
	 *            the data.
	 * @param length
	 *            the length of the expansion in <tt>bytes</tt>.
	 * @return the expanded array with given length.
	 */
	protected static byte[] doExpansion(MessageDigest md, byte[] secret, byte[] data, int length) {
		/*
		 * P_hash(secret, seed) = HMAC_hash(secret, A(1) + seed) +
		 * HMAC_hash(secret, A(2) + seed) + HMAC_hash(secret, A(3) + seed) + ...
		 * 
		 * where + indicates concatenation.
		 * 
		 * A() is defined as:
		 * 
		 * A(0) = seed, A(i) = HMAC_hash(secret, A(i-1))
		 */
		double hashLength = 32;
		if (md.getAlgorithm().equals("SHA-1")) {
			hashLength = 20;
		}

		int iterations = (int) Math.ceil(length / hashLength);
		byte[] expansion = new byte[0];

		byte[] A = data;
		for (int i = 0; i < iterations; i++) {
			A = doHMAC(md, secret, A);
			expansion = ByteArrayUtils.concatenate(expansion, doHMAC(md, secret, ByteArrayUtils.concatenate(A, data)));
		}

		return ByteArrayUtils.truncate(expansion, length);
	}

	/**
	 * Performs the HMAC computation as described in <a
	 * href="http://tools.ietf.org/html/rfc2104#section-2">RFC 2104</a>.
	 * 
	 * @param md
	 *            the cryptographic hash function.
	 * @param secret
	 *            the secret key.
	 * @param data
	 *            the data.
	 * @return the hash after HMAC has been applied.
	 */
	public static byte[] doHMAC(MessageDigest md, byte[] secret, byte[] data) {
		// the block size of the hash function, always 64 bytes (for SHA-512 it
		// would be 128 bytes, but not needed right now, except for test
		// purpose)

		int B = 64;
		if (md.getAlgorithm().equals("SHA-512")) {
			B = 128;
		}

		// See http://tools.ietf.org/html/rfc2104#section-2
		// ipad = the byte 0x36 repeated B times
		byte[] ipad = new byte[B];
		Arrays.fill(ipad, (byte) 0x36);

		// opad = the byte 0x5C repeated B times
		byte[] opad = new byte[B];
		Arrays.fill(opad, (byte) 0x5C);

		/*
		 * (1) append zeros to the end of K to create a B byte string (e.g., if
		 * K is of length 20 bytes and B=64, then K will be appended with 44
		 * zero bytes 0x00)
		 */
		byte[] step1 = secret;
		if (secret.length < B) {
			// append zeros to the end of K to create a B byte string
			step1 = ByteArrayUtils.padArray(secret, (byte) 0x00, B);
		} else if (secret.length > B) {
			// Applications that use keys longer
			// than B bytes will first hash the key using H and then use the
			// resultant L byte string as the actual key to HMAC.
			md.update(secret);
			step1 = md.digest();
			md.reset();

			step1 = ByteArrayUtils.padArray(step1, (byte) 0x00, B);
		}

		/*
		 * (2) XOR (bitwise exclusive-OR) the B byte string computed in step (1)
		 * with ipad
		 */
		byte[] step2 = ByteArrayUtils.xorArrays(step1, ipad);

		/*
		 * (3) append the stream of data 'text' to the B byte string resulting
		 * from step (2)
		 */
		byte[] step3 = ByteArrayUtils.concatenate(step2, data);

		/*
		 * (4) apply H to the stream generated in step (3)
		 */
		md.update(step3);
		byte[] step4 = md.digest();
		md.reset();

		/*
		 * (5) XOR (bitwise exclusive-OR) the B byte string computed in step (1)
		 * with opad
		 */
		byte[] step5 = ByteArrayUtils.xorArrays(step1, opad);

		/*
		 * (6) append the H result from step (4) to the B byte string resulting
		 * from step (5)
		 */
		byte[] step6 = ByteArrayUtils.concatenate(step5, step4);

		/*
		 * (7) apply H to the stream generated in step (6) and output the result
		 */
		md.update(step6);
		byte[] step7 = md.digest();

		return step7;
	}

	protected void setCurrentReadState() {
		DTLSConnectionState connectionState;
		if (isClient) {
			connectionState = new DTLSConnectionState(cipherSuite, compressionMethod, serverWriteKey, serverWriteIV, serverWriteMACKey);
		} else {
			connectionState = new DTLSConnectionState(cipherSuite, compressionMethod, clientWriteKey, clientWriteIV, clientWriteMACKey);
		}
		session.setReadState(connectionState);
	}

	protected void setCurrentWriteState() {
		DTLSConnectionState connectionState;
		if (isClient) {
			connectionState = new DTLSConnectionState(cipherSuite, compressionMethod, clientWriteKey, clientWriteIV, clientWriteMACKey);
		} else {
			connectionState = new DTLSConnectionState(cipherSuite, compressionMethod, serverWriteKey, serverWriteIV, serverWriteMACKey);
		}
		session.setWriteState(connectionState);
	}

	/**
	 * Wraps the message into a record layer.
	 * 
	 * @param fragment
	 *            the {@link DTLSMessage} fragment.
	 * @return the fragment wrapped into a record layer.
	 */
	protected Record wrapMessage(DTLSMessage fragment) {

		ContentType type = null;
		if (fragment instanceof ApplicationMessage) {
			type = ContentType.APPLICATION_DATA;
		} else if (fragment instanceof AlertMessage) {
			type = ContentType.ALERT;
		} else if (fragment instanceof ChangeCipherSpecMessage) {
			type = ContentType.CHANGE_CIPHER_SPEC;
		} else if (fragment instanceof HandshakeMessage) {
			type = ContentType.HANDSHAKE;
		}

		return new Record(type, session.getWriteEpoch(), session.getSequenceNumber(), fragment, session);
	}

	/**
	 * Determines, using the epoch and sequence number, whether this record is
	 * the next one, which needs to be processed by the handshake protocol.
	 * 
	 * @param record
	 *            the current received message.
	 * @return <tt>true</tt> if the current message is the next to process,
	 *         <tt>false</tt> otherwise.
	 */
	protected boolean processMessageNext(Record record) {

		int epoch = record.getEpoch();
		if (epoch < session.getReadEpoch()) {
			// discard old message
			LOG.info("Discarded message due to older epoch.");
			return false;
		} else if (epoch == session.getReadEpoch()) {
			DTLSMessage fragment = record.getFragment();
			if (fragment instanceof AlertMessage) {
				return true; // Alerts must be processed immediately
			} else if (fragment instanceof ChangeCipherSpecMessage) {
				return true; // CCS must be processed immediately
			} else if (fragment instanceof HandshakeMessage) {
				int messageSeq = ((HandshakeMessage) fragment).getMessageSeq();

				if (messageSeq == nextReceiveSeq) {
					nextReceiveSeq++;
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			// newer epoch, queue message
			queuedMessages.add(record);
			return false;
		}
	}

	// Getters and Setters ////////////////////////////////////////////

	public CipherSuite getCipherSuite() {
		return cipherSuite;
	}

	/**
	 * Sets the negotiated {@link CipherSuite} and the corresponding
	 * {@link KeyExchangeAlgorithm}.
	 * 
	 * @param cipherSuite
	 *            the cipher suite.
	 */
	public void setCipherSuite(CipherSuite cipherSuite) {
		this.cipherSuite = cipherSuite;
		this.keyExchange = cipherSuite.getKeyExchange();
		this.session.setKeyExchange(keyExchange);
	}

	public byte[] getMasterSecret() {
		return masterSecret;
	}

	public SecretKey getClientWriteMACKey() {
		return clientWriteMACKey;
	}

	public SecretKey getServerWriteMACKey() {
		return serverWriteMACKey;
	}

	public IvParameterSpec getClientWriteIV() {
		return clientWriteIV;
	}

	public IvParameterSpec getServerWriteIV() {
		return serverWriteIV;
	}

	public SecretKey getClientWriteKey() {
		return clientWriteKey;
	}

	public SecretKey getServerWriteKey() {
		return serverWriteKey;
	}

	public DTLSSession getSession() {
		return session;
	}

	public void setSession(DTLSSession session) {
		this.session = session;
	}

	/**
	 * Add the smallest available message sequence to the handshake message.
	 * 
	 * @param message
	 *            the {@link HandshakeMessage}.
	 */
	public void setSequenceNumber(HandshakeMessage message) {
		message.setMessageSeq(sequenceNumber);
		sequenceNumber++;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public int getNextReceiveSeq() {
		return nextReceiveSeq;
	}

	public void incrementNextReceiveSeq(int nextReceiveSeq) {
		this.nextReceiveSeq++;
	}
}

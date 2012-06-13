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

public abstract class Handshaker {

	// Logging ////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(Handshaker.class.getName());

	// Static members /////////////////////////////////////////////////

	public final static String MASTER_SECRET_LABEL = "master secret";

	public final static String KEY_EXPANSION_LABEL = "key expansion";

	public final static String CLIENT_FINISHED_LABEL = "client finished";

	public final static String SERVER_FINISHED_LABEL = "server finished";

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

		byte[] data = doPRF(masterSecret, KEY_EXPANSION_LABEL, concatenate(serverRandom.getRandomBytes(), clientRandom.getRandomBytes()));

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
		clientWriteMACKey = new SecretKeySpec(data, 0, 8, "Mac");
		serverWriteMACKey = new SecretKeySpec(data, 8, 8, "Mac");

		clientWriteKey = new SecretKeySpec(data, 16, 16, "AES");
		serverWriteKey = new SecretKeySpec(data, 32, 16, "AES");

		// TODO check this values
		clientWriteIV = new IvParameterSpec(data, 48, 16);
		serverWriteIV = new IvParameterSpec(data, 64, 16);
		
		clientWriteIV = null;
		serverWriteIV = null;

	}

	private byte[] generateMasterSecret(byte[] premasterSecret) {

		/*
		 * See http://tools.ietf.org/html/rfc5246#section-8.1
		 * 
		 * master_secret = PRF(pre_master_secret, "master secret",
		 * ClientHello.random + ServerHello.random) [0..47]
		 */

		byte[] randomSeed = concatenate(clientRandom.getRandomBytes(), serverRandom.getRandomBytes());
		return doPRF(premasterSecret, MASTER_SECRET_LABEL, randomSeed);
	}

	/**
	 * Do
	 * 
	 * @param md
	 *            the md
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
				// The master secret is always 48 bytes lond, see
				// http://tools.ietf.org/html/rfc5246#section-8.1
				return doExpansion(md, secret, concatenate(label.getBytes(), seed), 48);

			case KEY_EXPANSION_LABEL:
				// The most key material required is 128 bytes, see
				// http://tools.ietf.org/html/rfc5246#section-6.3
				return doExpansion(md, secret, concatenate(label.getBytes(), seed), 128);

			case CLIENT_FINISHED_LABEL:
			case SERVER_FINISHED_LABEL:
				// The verify data is always 12 bytes long, see
				// http://tools.ietf.org/html/rfc5246#section-7.4.9
				return doExpansion(md, secret, concatenate(label.getBytes(), seed), 12);

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
	private static byte[] doExpansion(MessageDigest md, byte[] secret, byte[] data, int length) {
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

		int iterations = (int) Math.ceil(length / hashLength);
		byte[] expansion = new byte[0];

		byte[] A = data;
		for (int i = 0; i < iterations; i++) {
			A = doHMAC(md, secret, A);
			expansion = concatenate(expansion, doHMAC(md, secret, concatenate(A, data)));
		}

		return truncate(expansion, length);
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
	private static byte[] doHMAC(MessageDigest md, byte[] secret, byte[] data) {
		// the block size of the hash function, always 64 bytes
		int B = 64;

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
			step1 = paddArray(secret, (byte) 0x00, B);
		} else if (secret.length > B) {
			// Applications that use keys longer
			// than B bytes will first hash the key using H and then use the
			// resultant L byte string as the actual key to HMAC.
			md.update(secret);
			step1 = md.digest();
			md.reset();

			step1 = paddArray(step1, (byte) 0x00, B);
		}

		/*
		 * (2) XOR (bitwise exclusive-OR) the B byte string computed in step (1)
		 * with ipad
		 */
		byte[] step2 = new byte[B];
		for (int i = 0; i < B; i++) {
			step2[i] = (byte) (step1[i] ^ ipad[i]);
		}

		/*
		 * (3) append the stream of data 'text' to the B byte string resulting
		 * from step (2)
		 */
		byte[] step3 = concatenate(step2, data);

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
		byte[] step5 = new byte[B];
		for (int i = 0; i < B; i++) {
			step5[i] = (byte) (step1[i] ^ opad[i]);
		}

		/*
		 * (6) append the H result from step (4) to the B byte string resulting
		 * from step (5)
		 */
		byte[] step6 = concatenate(step5, step4);

		/*
		 * (7) apply H to the stream generated in step (6) and output the result
		 */
		md.update(step6);
		byte[] step7 = md.digest();

		return step7;
	}

	/**
	 * Adds a padding to the given array, such that a new array with the given
	 * length is generated.
	 * 
	 * @param array
	 *            the array to be padded.
	 * @param value
	 *            the padding value.
	 * @param newLength
	 *            the new length of the padded array.
	 * @return the array padded with the given value.
	 */
	private static byte[] paddArray(byte[] array, byte value, int newLength) {
		int length = array.length;
		int paddingLength = newLength - length;

		if (paddingLength < 1) {
			return array;
		} else {
			byte[] padding = new byte[paddingLength];
			Arrays.fill(padding, value);

			return concatenate(array, padding);
		}

	}

	private static byte[] truncate(byte[] array, int newLength) {
		if (array.length < newLength) {
			return array;
		} else {
			byte[] truncated = new byte[newLength];
			System.arraycopy(array, 0, truncated, 0, newLength);

			return truncated;
		}
	}

	/**
	 * Concatenates two byte arrays.
	 * 
	 * @param a
	 *            the first array.
	 * @param b
	 *            the second array.
	 * @return the concatenated array.
	 */
	private static byte[] concatenate(byte[] a, byte[] b) {
		int lengthA = a.length;
		int lengthB = b.length;

		byte[] concat = new byte[lengthA + lengthB];

		System.arraycopy(a, 0, concat, 0, lengthA);
		System.arraycopy(b, 0, concat, lengthA, lengthB);

		return concat;
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

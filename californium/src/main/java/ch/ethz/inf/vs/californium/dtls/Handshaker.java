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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;
import ch.ethz.inf.vs.californium.dtls.CertSendExtension.CertType;
import ch.ethz.inf.vs.californium.dtls.CipherSuite.KeyExchangeAlgorithm;
import ch.ethz.inf.vs.californium.util.ByteArrayUtils;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The base class for the handshake protocol logic. Contains all the
 * functionality and fields which is needed by all types of handshakers.
 * 
 * @author Stefan Jucker
 * 
 */
public abstract class Handshaker {

	// Logging ////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(Handshaker.class.getName());

	// Static members /////////////////////////////////////////////////

	private final static int MASTER_SECRET_LABEL = 1;

	private final static int KEY_EXPANSION_LABEL = 2;

	public final static int CLIENT_FINISHED_LABEL = 3;

	public final static int SERVER_FINISHED_LABEL = 4;

	public final static int TEST_LABEL = 5;

	public final static int TEST_LABEL_2 = 6;

	public final static int TEST_LABEL_3 = 7;
	
	public final static String KEY_STORE_PASSWORD = "endPass";
	
	private static final String TRUST_STORE_PASSWORD = "rootPass";

	/**
	 * A map storing shared keys. The shared key is associated with an PSK
	 * identity. See <a href="http://tools.ietf.org/html/rfc4279#section-2">RFC
	 * 4279</a> for details.
	 */
	protected static Map<String, byte[]> sharedKeys = new HashMap<String, byte[]>();

	static {
		sharedKeys.put("TEST", new byte[] { 0x73, 0x65, 0x63, 0x72, 0x65, 0x74, 0x50, 0x53, 0x4b });
		sharedKeys.put("Client_identity", new byte[] { 0x73, 0x65, 0x63, 0x72, 0x65, 0x74, 0x50, 0x53, 0x4b });
		sharedKeys.put("001", new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });
		sharedKeys.put("PSK_Identity", new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 });
	}

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
	private CipherSuite cipherSuite;
	private CompressionMethod compressionMethod;

	protected KeyExchangeAlgorithm keyExchange;

	/** The helper class to execute the ECDHE key agreement and key generation. */
	protected ECDHECryptography ecdhe;

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
	private int sequenceNumber = 0;

	/** The next expected handshake message sequence number. */
	private int nextReceiveSeq = 0;

	/** The CoAP {@link Message} that needs encryption. */
	protected Message message = null;

	/** Queue for messages, that can not yet be processed. */
	protected Collection<Record> queuedMessages;
	
	/** Store the fragmented messages until we are able to reassemble the handshake message. */
	protected Map<Integer, List<FragmentedHandshakeMessage>> fragmentedMessages = new HashMap<Integer, List<FragmentedHandshakeMessage>>();

	/**
	 * The message digest to compute the handshake hashes sent in the
	 * {@link Finished} messages.
	 */
	protected MessageDigest md;

	/** All the handshake messages sent before the CertificateVerify message. */
	protected byte[] handshakeMessages = new byte[] {};

	/**
	 * The last flight that is sent during this handshake, will not be
	 * retransmitted unless the peer retransmits its last flight.
	 */
	protected DTLSFlight lastFlight = null;

	/** The handshaker's private key. */
	protected PrivateKey privateKey;

	/** The handshaker's certificate chain. */
	protected Certificate[] certificates;

	// Constructor ////////////////////////////////////////////////////

	/**
	 * 
	 * @param peerAddress
	 *            the peer's address.
	 * @param isClient
	 *            indicating whether this instance represents a client or a
	 *            server.
	 * @param session
	 *            the session belonging to this handshake.
	 */
	public Handshaker(EndpointAddress peerAddress, boolean isClient, DTLSSession session) {
		this.endpointAddress = peerAddress;
		this.isClient = isClient;
		this.session = session;
		this.queuedMessages = new HashSet<Record>();
		loadKeyStore();
		try {
			this.md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			LOG.severe("Could not initialize the message digest algorithm.");
			e.printStackTrace();
		}
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
	public abstract DTLSFlight processMessage(Record message) throws HandshakeException;

	/**
	 * Gets the handshake flight which needs to be sent first to initiate
	 * handshake. This differs from client side to server side.
	 * 
	 * @return the handshake message to start off the handshake protocol.
	 */
	public abstract DTLSFlight getStartHandshakeMessage();

	// Methods ////////////////////////////////////////////////////////

	/**
	 * First, generates the master secret from the given premaster secret and
	 * then applying the key expansion on the master secret generates a large
	 * enough key block to generate the write, MAC and IV keys. See <a
	 * href="http://tools.ietf.org/html/rfc5246#section-6.3">RFC 5246</a> for
	 * further details about the keys.
	 * 
	 * @param premasterSecret
	 *            the shared premaster secret.
	 */
	protected void generateKeys(byte[] premasterSecret) {
		masterSecret = generateMasterSecret(premasterSecret);
		session.setMasterSecret(masterSecret);

		calculateKeys(masterSecret);
	}

	/**
	 * Calculates the encryption key, MAC key and IV from a given master secret.
	 * First, applies the key expansion to the master secret.
	 * 
	 * @param masterSecret
	 *            the master secret.
	 */
	private void calculateKeys(byte[] masterSecret) {
		/*
		 * See http://tools.ietf.org/html/rfc5246#section-6.3:
		 * key_block = PRF(SecurityParameters.master_secret, "key expansion", SecurityParameters.server_random + SecurityParameters.client_random);
		 */

		byte[] data = doPRF(masterSecret, KEY_EXPANSION_LABEL, ByteArrayUtils.concatenate(serverRandom.getRandomBytes(), clientRandom.getRandomBytes()));

		/*
		 * Create keys as suggested in
		 * http://tools.ietf.org/html/rfc5246#section-6.3:
		 * client_write_MAC_key[SecurityParameters.mac_key_length]
		 * server_write_MAC_key[SecurityParameters.mac_key_length]
		 * client_write_key[SecurityParameters.enc_key_length]
		 * server_write_key[SecurityParameters.enc_key_length]
		 * client_write_IV[SecurityParameters.fixed_iv_length]
		 * server_write_IV[SecurityParameters.fixed_iv_length]
		 */
		if (cipherSuite == null) {
			cipherSuite = session.getCipherSuite();
		}

		int macKeyLength = cipherSuite.getBulkCipher().getMacKeyLength();
		int encKeyLength = cipherSuite.getBulkCipher().getEncKeyLength();
		int fixedIvLength = cipherSuite.getBulkCipher().getFixedIvLength();

		clientWriteMACKey = new SecretKeySpec(data, 0, macKeyLength, "Mac");
		serverWriteMACKey = new SecretKeySpec(data, macKeyLength, macKeyLength, "Mac");

		clientWriteKey = new SecretKeySpec(data, 2 * macKeyLength, encKeyLength, "AES");
		serverWriteKey = new SecretKeySpec(data, (2 * macKeyLength) + encKeyLength, encKeyLength, "AES");

		clientWriteIV = new IvParameterSpec(data, (2 * macKeyLength) + (2 * encKeyLength), fixedIvLength);
		serverWriteIV = new IvParameterSpec(data, (2 * macKeyLength) + (2 * encKeyLength) + fixedIvLength, fixedIvLength);

	}

	/**
	 * Generates the master secret from a given shared premaster secret as
	 * described in <a href="http://tools.ietf.org/html/rfc5246#section-8.1">RFC
	 * 5246</a>.
	 * 
	 * <pre>
	 * master_secret = PRF(pre_master_secret, "master secret",
	 * 	ClientHello.random + ServerHello.random) [0..47]
	 * </pre>
	 * 
	 * @param premasterSecret
	 *            the shared premaster secret.
	 * @return the master secret.
	 */
	private byte[] generateMasterSecret(byte[] premasterSecret) {
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
	 * @param labelId
	 *            the label
	 * @param seed
	 *            the seed
	 * @return the byte[]
	 */
	public static byte[] doPRF(byte[] secret, int labelId, byte[] seed) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");

			String label;
			switch (labelId) {
			case MASTER_SECRET_LABEL:
				// The master secret is always 48 bytes long, see
				// http://tools.ietf.org/html/rfc5246#section-8.1
				label = "master secret";
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 48);

			case KEY_EXPANSION_LABEL:
				// The most key material required is 128 bytes, see
				// http://tools.ietf.org/html/rfc5246#section-6.3
				label = "key expansion";
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 128);

			case CLIENT_FINISHED_LABEL:
				// The verify data is always 12 bytes long, see
				// http://tools.ietf.org/html/rfc5246#section-7.4.9
				label = "client finished";
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 12);

			case SERVER_FINISHED_LABEL:
				// The verify data is always 12 bytes long, see
				// http://tools.ietf.org/html/rfc5246#section-7.4.9
				label = "server finished";
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 12);

			case TEST_LABEL:
				// http://www.ietf.org/mail-archive/web/tls/current/msg03416.html
				label = "test label";
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 100);

			case TEST_LABEL_2:
				// http://www.ietf.org/mail-archive/web/tls/current/msg03416.html
				label = "test label";
				md = MessageDigest.getInstance("SHA-512");
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 196);

			case TEST_LABEL_3:
				// http://www.ietf.org/mail-archive/web/tls/current/msg03416.html
				label = "test label";
				md = MessageDigest.getInstance("SHA-384");
				return doExpansion(md, secret, ByteArrayUtils.concatenate(label.getBytes(), seed), 148);

			default:
				LOG.severe("Unknwon label: " + labelId);
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
		 * where + indicates concatenation. A() is defined as: A(0) = seed, A(i)
		 * = HMAC_hash(secret, A(i-1))
		 */
		double hashLength = 32;
		if (md.getAlgorithm().equals("SHA-1")) {
			hashLength = 20;
		} else if (md.getAlgorithm().equals("SHA-384")) {
			hashLength = 48;
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
		if (md.getAlgorithm().equals("SHA-512") || md.getAlgorithm().equals("SHA-384")) {
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
	 * Wraps the message into (potentially multiple) record layers. Sets the
	 * epoch, sequence number and handles fragmentation for handshake messages.
	 * 
	 * @param fragment
	 *            the {@link DTLSMessage} fragment.
	 * @return the fragment wrapped into (multiple) record layers.
	 */
	protected List<Record> wrapMessage(DTLSMessage fragment) {
		
		List<Record> records = new ArrayList<Record>();

		ContentType type = null;
		if (fragment instanceof ApplicationMessage) {
			type = ContentType.APPLICATION_DATA;
		} else if (fragment instanceof AlertMessage) {
			type = ContentType.ALERT;
		} else if (fragment instanceof ChangeCipherSpecMessage) {
			type = ContentType.CHANGE_CIPHER_SPEC;
		} else if (fragment instanceof HandshakeMessage) {
			type = ContentType.HANDSHAKE;
			HandshakeMessage handshakeMessage = (HandshakeMessage) fragment;
			setSequenceNumber(handshakeMessage);
			
			byte[] messageBytes = handshakeMessage.fragmentToByteArray();
			
			int maxFragmentLength = Properties.std.getInt("MAX_FRAGMENT_LENGTH");
			if (messageBytes.length > maxFragmentLength) {
				/*
				 * The sender then creates N handshake messages, all with the
				 * same message_seq value as the original handshake message.
				 */
				int messageSeq = handshakeMessage.getMessageSeq();

				int numFragments = (messageBytes.length / maxFragmentLength) + 1;
				
				int offset = 0;
				for (int i = 0; i < numFragments; i++) {
					int fragmentLength = maxFragmentLength;
					if (offset + fragmentLength > messageBytes.length) {
						// the last fragment is normally shorter than the maximal size
						fragmentLength = messageBytes.length - offset;
					}
					byte[] fragmentBytes = new byte[fragmentLength];
					System.arraycopy(messageBytes, offset, fragmentBytes, 0, fragmentLength);
					
					FragmentedHandshakeMessage fragmentedMessage =
							new FragmentedHandshakeMessage(fragmentBytes, handshakeMessage.getMessageType(), offset, messageBytes.length);
					
					// all fragments have the same message_seq
					fragmentedMessage.setMessageSeq(messageSeq);
					offset += fragmentBytes.length;
					
					records.add(new Record(type, session.getWriteEpoch(), session.getSequenceNumber(), fragmentedMessage, session));
				}
			}
		}
		
		if (records.isEmpty()) { // no fragmentation needed
			records.add(new Record(type, session.getWriteEpoch(), session.getSequenceNumber(), fragment, session));
		}
		
		return records;
	}

	/**
	 * Determines, using the epoch and sequence number, whether this record is
	 * the next one which needs to be processed by the handshake protocol.
	 * 
	 * @param record
	 *            the current received message.
	 * @return <tt>true</tt> if the current message is the next to process,
	 *         <tt>false</tt> otherwise.
	 * @throws HandshakeException 
	 */
	protected boolean processMessageNext(Record record) throws HandshakeException {

		int epoch = record.getEpoch();
		if (epoch < session.getReadEpoch()) {
			// discard old message
			LOG.info("Discarded message from " + endpointAddress.toString() + " due to older epoch.");
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
					if (!(fragment instanceof FragmentedHandshakeMessage)) {
						// each fragment has the same message_seq, therefore
						// don't increment yet
						incrementNextReceiveSeq();
					}
					return true;
				} else if (messageSeq > nextReceiveSeq) {
					LOG.info("Queued newer message from same epoch, message_seq: " + messageSeq + ", next_receive_seq: " + nextReceiveSeq);
					queuedMessages.add(record);
					return false;
				} else {
					LOG.info("Discarded message due to older message_seq: " + messageSeq + ", next_receive_seq: " + nextReceiveSeq);
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

	/**
	 * Closes the current connection and returns the notify_close Alert message
	 * wrapped in flight.
	 * 
	 * @return the close_notify message to indicate closing of the connection.
	 */
	protected DTLSFlight closeConnection() {
		DTLSFlight flight = new DTLSFlight();

		// TODO what to do here?
		session.setActive(false);
		DTLSMessage closeNotify = new AlertMessage(AlertLevel.WARNING, AlertDescription.CLOSE_NOTIFY);

		flight.addMessage(wrapMessage(closeNotify));
		flight.setRetransmissionNeeded(false);

		return flight;
	}
	
	/**
	 * Loads the given keyStore (location specified in Californium.properties).
	 * The keyStore must contain the private key and the corresponding
	 * certificate (chain). The keyStore alias is expected to be "end".
	 */
	protected void loadKeyStore() {
		try {
			KeyStore keyStore = KeyStore.getInstance("JKS");
			InputStream in = new FileInputStream(Properties.std.getProperty("KEY_STORE_LOCATION".replace("/", File.pathSeparator)));
			keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());

			certificates = keyStore.getCertificateChain("end");
			privateKey = (PrivateKey) keyStore.getKey("end", KEY_STORE_PASSWORD.toCharArray());
		} catch (Exception e) {
			LOG.severe("Could not load the keystore.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads the trusted certificates.
	 * 
	 * @return the trusted certificates.
	 */
	protected Certificate[] loadTrustedCertificates() {
		Certificate[] trustedCertificates = new Certificate[] {};

		try {
			KeyStore trustStore = KeyStore.getInstance("JKS");
			InputStream in = new FileInputStream(Properties.std.getProperty("TRUST_STORE_LOCATION".replace("/", File.pathSeparator)));
			trustStore.load(in, TRUST_STORE_PASSWORD.toCharArray());
			
			trustedCertificates = trustStore.getCertificateChain("root");
		} catch (Exception e) {
			LOG.severe("Could not load the trusted certificates.");
			e.printStackTrace();
		}

		return trustedCertificates;
	}

	
	/**
	 * Checks whether the peer supports receiving RawPublicKey certificates.
	 * 
	 * @param extension
	 *            the peer's {@link CertReceiveExtension}.
	 * @return <code>true</code> if the peer supports RawPublicKey
	 *         certificates, <code>false</code> otherwise.
	 */
	protected boolean sendRawPublicKey(CertReceiveExtension extension) {
		for (CertType certType : extension.getCertTypes()) {
			if (certType == CertType.RAW_PUBLIC_KEY) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Called when a fragmented handshake message is received. Checks if all
	 * fragments already here to reassemble the handshake message and if so,
	 * returns the whole handshake message.
	 * 
	 * @param fragment
	 *            the fragmented handshake message.
	 * @return the reassembled handshake message (if all fragements available),
	 *         <code>null</code> otherwise.
	 * @throws HandshakeException
	 */
	protected HandshakeMessage handleFragmentation(FragmentedHandshakeMessage fragment) throws HandshakeException {
		HandshakeMessage reassembledMessage = null;
		
		int messageSeq = fragment.getMessageSeq();
		if (fragmentedMessages.get(messageSeq) == null) {
			fragmentedMessages.put(messageSeq, new ArrayList<FragmentedHandshakeMessage>());
		}
		// store fragment together with other fragments of same message_seq
		fragmentedMessages.get(messageSeq).add(fragment);
		
		reassembledMessage = reassembleFragments(messageSeq, fragment.getMessageLength(), fragment.getMessageType(), session);
		if (reassembledMessage != null) {
			// message could be reassembled, therefore increase the next_receive_seq
			incrementNextReceiveSeq();
			fragmentedMessages.remove(messageSeq);
		}
		
		return reassembledMessage;
	}
	
	/**
	 * Tries to reassemble the handshake message with the available fragments.
	 * 
	 * @param messageSeq
	 *            the fragment's message_seq
	 * @param totalLength
	 *            the expected total length of the reassembled fragment
	 * @param type
	 *            the type of the handshake message
	 * @param session
	 *            the {@link DTLSSession}
	 * @return the reassembled handshake message (if all fragements available),
	 *         <code>null</code> otherwise.
	 * @throws HandshakeException
	 */
	protected HandshakeMessage reassembleFragments(int messageSeq, int totalLength, HandshakeType type, DTLSSession session) throws HandshakeException {
		List<FragmentedHandshakeMessage> fragments = fragmentedMessages.get(messageSeq);
		HandshakeMessage message = null;

		// sort according to fragment offset
		Collections.sort(fragments, new Comparator<FragmentedHandshakeMessage>() {

			@Override
			public int compare(FragmentedHandshakeMessage o1, FragmentedHandshakeMessage o2) {
				if (o1.getFragmentOffset() == o2.getFragmentOffset()) {
					return 0;
				} else if (o1.getFragmentOffset() < o2.getFragmentOffset()) {
					return -1;
				} else {
					return 1;
				}
			}
		});

		byte[] reassembly = new byte[] {};
		int offset = 0;
		for (FragmentedHandshakeMessage fragmentedHandshakeMessage : fragments) {
			
			int fragmentOffset = fragmentedHandshakeMessage.getFragmentOffset();
			int fragmentLength = fragmentedHandshakeMessage.getFragmentLength();
			
			if (fragmentOffset == offset) { // eliminate duplicates
				// case: no overlap
				reassembly = ByteArrayUtils.concatenate(reassembly, fragmentedHandshakeMessage.fragmentToByteArray());
				offset = reassembly.length;
			} else if (fragmentOffset < offset && (fragmentOffset + fragmentLength) > offset) {
				// case: overlap fragment
				
				// determine the offset where the fragment adds new information for the reassembly
				int newOffset = offset - fragmentOffset;
				int newLength = fragmentLength - newOffset;
				byte[] newBytes = new byte[newLength];
				// take only the new bytes and add them
				System.arraycopy(fragmentedHandshakeMessage.fragmentToByteArray(), newOffset, newBytes, 0, newLength);	
				reassembly = ByteArrayUtils.concatenate(reassembly, newBytes);
				
				offset = reassembly.length;
			}
		}
		
		if (reassembly.length == totalLength) {
			// the reassembled fragment has the expected length
			FragmentedHandshakeMessage wholeMessage = new FragmentedHandshakeMessage(type, totalLength, messageSeq, 0, reassembly);
			reassembly = wholeMessage.toByteArray();
			
			KeyExchangeAlgorithm keyExchangeAlgorithm = KeyExchangeAlgorithm.NULL;
			boolean receiveRawPublicKey = false;
			if (session != null) {
				keyExchangeAlgorithm = session.getKeyExchange();
				receiveRawPublicKey = session.receiveRawPublicKey();
			}
			message = HandshakeMessage.fromByteArray(reassembly, keyExchangeAlgorithm, receiveRawPublicKey);
		}
		
		return message;
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
		this.session.setCipherSuite(cipherSuite);
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

	public void incrementNextReceiveSeq() {
		this.nextReceiveSeq++;
	}

	public CompressionMethod getCompressionMethod() {
		return compressionMethod;
	}

	public void setCompressionMethod(CompressionMethod compressionMethod) {
		this.compressionMethod = compressionMethod;
		this.session.setCompressionMethod(compressionMethod);
	}
}

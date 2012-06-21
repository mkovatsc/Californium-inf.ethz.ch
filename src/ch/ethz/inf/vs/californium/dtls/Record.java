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

import java.math.BigInteger;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.dtls.CipherSuite.KeyExchangeAlgorithm;
import ch.ethz.inf.vs.californium.layers.DTLSLayer;
import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

public class Record {

	// Logging ////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(Record.class.getName());

	// CoAP-specific constants/////////////////////////////////////////

	private static final int CONTENT_TYPE_BITS = 8;

	private static final int VERSION_BITS = 8; // for major and minor each

	private static final int EPOCH_BITS = 16;

	private static final int SEQUENCE_NUMBER_BYTES = 6;

	private static final int LENGHT_BITS = 16;

	// Members ////////////////////////////////////////////////////////

	/** The higher-level protocol used to process the enclosed fragment */
	private ContentType type = null;

	/**
	 * The version of the protocol being employed. DTLS version 1.2 uses { 254,
	 * 253 }
	 */
	private ProtocolVersion version = new ProtocolVersion(254, 253);

	/** A counter value that is incremented on every cipher state change */
	private int epoch = -1;

	/** The sequence number for this record */
	private long sequenceNumber;

	/** The length (in bytes) of the following {@link DTLSMessage}. */
	private int length = 0;

	/**
	 * The application data. This data is transparent and treated as an
	 * independent block to be dealt with by the higher-level protocol specified
	 * by the type field.
	 */
	private DTLSMessage fragment = null;

	/** The raw byte representation of the fragment. */
	private byte[] fragmentBytes = null;

	/** The DTLS session. */
	private DTLSSession session;

	// Constructors ///////////////////////////////////////////////////

	/**
	 * Called when reconstructing the record from a byte array. The fragment
	 * will remain in its binary representation up to the {@link DTLSLayer}.
	 * 
	 * @param type
	 * @param version
	 * @param epoch
	 * @param sequenceNumber
	 * @param length
	 * @param fragmentBytes
	 */
	public Record(ContentType type, ProtocolVersion version, int epoch, long sequenceNumber, int length, byte[] fragmentBytes) {
		this.type = type;
		this.version = version;
		this.epoch = epoch;
		this.sequenceNumber = sequenceNumber;
		this.length = length;
		this.fragmentBytes = fragmentBytes;
	}

	/**
	 * Called when creating a record after receiving a {@link Message}.
	 * 
	 * @param type
	 *            the type
	 * @param epoch
	 *            the epoch
	 * @param sequenceNumber
	 *            the sequence number
	 * @param fragment
	 *            the fragment
	 * @param session
	 *            the session
	 */

	public Record(ContentType type, int epoch, int sequenceNumber, DTLSMessage fragment, DTLSSession session) {
		this.type = type;
		this.epoch = epoch;
		this.sequenceNumber = sequenceNumber;
		this.length = fragment.getLength();
		this.session = session;
		setFragment(fragment);
	}

	// Serialization //////////////////////////////////////////////////

	/**
	 * Encodes the DTLS Record into its raw binary structure as defined in the
	 * DTLS v.1.2 specification.
	 * 
	 * @return the encoded byte array
	 */
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();

		writer.write(type.getCode(), CONTENT_TYPE_BITS);

		writer.write(version.getMajor(), VERSION_BITS);
		writer.write(version.getMinor(), VERSION_BITS);

		writer.write(epoch, EPOCH_BITS);

		// write uint48 sequence number (since int is only 32 bits, we take a
		// long)
		// see http://tools.ietf.org/html/rfc6347#section-4.1
		// TODO sequenceNumber = 281474976710655L; does not work, we need
		// unsigned bytes, implement this in DatagramWriter
		byte[] sequenceNumberBytes = new byte[SEQUENCE_NUMBER_BYTES];
		sequenceNumberBytes[0] = (byte) (sequenceNumber >> 40);
		sequenceNumberBytes[1] = (byte) (sequenceNumber >> 32);
		sequenceNumberBytes[2] = (byte) (sequenceNumber >> 24);
		sequenceNumberBytes[3] = (byte) (sequenceNumber >> 16);
		sequenceNumberBytes[4] = (byte) (sequenceNumber >> 8);
		sequenceNumberBytes[5] = (byte) (sequenceNumber);
		writer.writeBytes(sequenceNumberBytes);

		length = fragmentBytes.length;
		writer.write(length, LENGHT_BITS);

		writer.writeBytes(fragmentBytes);

		return writer.toByteArray();
	}

	/**
	 * Decodes the DTLS Record from its raw binary representation.
	 * 
	 * @param byteArray
	 *            the byte array
	 * @return the decoded record
	 */
	public static Record fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		ContentType contentType = ContentType.getTypeByValue(reader.read(CONTENT_TYPE_BITS));

		int major = reader.read(VERSION_BITS);
		int minor = reader.read(VERSION_BITS);
		ProtocolVersion version = new ProtocolVersion(major, minor);

		int epoch = reader.read(EPOCH_BITS);

		// TODO read uint48 sequence number
		byte[] sequenceNumberBytes = new byte[SEQUENCE_NUMBER_BYTES];
		sequenceNumberBytes = reader.readBytes(SEQUENCE_NUMBER_BYTES);
		BigInteger bigInteger = new BigInteger(sequenceNumberBytes);
		long sequenceNumber = bigInteger.longValue();

		int length = reader.read(LENGHT_BITS);

		// delay decryption/interpretation of fragment
		byte[] fragmentBytes = reader.readBytes(length);

		return new Record(contentType, version, epoch, sequenceNumber, length, fragmentBytes);
	}

	// Cryptography /////////////////////////////////////////////////////////

	/**
	 * Encrypts the fragment, if a ciphersuite is available that supports
	 * encryption.
	 * 
	 * @param byteArray
	 * @return
	 */
	private byte[] encryptFragment(byte[] byteArray) {
		if (session == null) {
			return byteArray;
		}

		byte[] encryptedFragment = byteArray;

		CipherSuite cipherSuite = session.getWriteState().getCipherSuite();
		if (cipherSuite != CipherSuite.SSL_NULL_WITH_NULL_NULL) {
			try {
				/*
				 * See http://tools.ietf.org/html/rfc5246#section-6.2.3.3 for
				 * explanation of additional data or
				 * http://tools.ietf.org/html/rfc5116#section-2.1
				 */
				byte[] iv = session.getWriteState().getIv().getIV();
				byte[] nonce = generateNonce(iv);
				byte[] key = session.getWriteState().getEncryptionKey().getEncoded();
				byte[] additionalData = generateAdditionalData(getLength());

				encryptedFragment = CCMBlockCipher.encrypt(key, nonce, additionalData, byteArray, 8);
				
				if (encryptedFragment == null) {
					// TODO alert
				}

			} catch (Exception e) {
				LOG.severe("Could not encrypt DTLS application data!");
				e.printStackTrace();
			}
		}

		return encryptedFragment;
	}

	/**
	 * Decrypts the byte array according to the current connection state. So,
	 * potentially no decryption takes place.
	 * 
	 * @param byteArray
	 *            the potentially encrypted fragment.
	 * @return the decrypted fragment.
	 */
	private byte[] decryptFragment(byte[] byteArray) {
		if (session == null) {
			return byteArray;
		}

		byte[] fragment = byteArray;

		CipherSuite cipherSuite = session.getReadState().getCipherSuite();
		if (cipherSuite != CipherSuite.SSL_NULL_WITH_NULL_NULL) {
			try {
				/*
				 * See http://tools.ietf.org/html/rfc5246#section-6.2.3.3 for
				 * explanation of additional data or
				 * http://tools.ietf.org/html/rfc5116#section-2.2
				 */
				byte[] iv = session.getReadState().getIv().getIV();
				byte[] nonce = generateNonce(iv);
				byte[] key = session.getReadState().getEncryptionKey().getEncoded();
				// TODO is the decrypted always 8 bytes shorter than the cipher?
				byte[] additionalData = generateAdditionalData(getLength() - 8);

				fragment = CCMBlockCipher.decrypt(key, nonce, additionalData, byteArray, 8);
				if (fragment == null) {
					// TODO alert
				}
			} catch (Exception e) {
				LOG.severe("Could not decrypt DTLS application data!");
				e.printStackTrace();
			}
		}

		return fragment;
	}

	/**
	 * http://tools.ietf.org/html/draft-mcgrew-tls-aes-ccm-ecc-03#section-2:
	 * 
	 * <pre>
	 * struct {
	 *   case client:
	 *     uint32 client_write_IV;  // low order 32-bits
	 *   case server:
	 *     uint32 server_write_IV;  // low order 32-bits
	 *  uint64 seq_num;
	 * } CCMNonce.
	 * </pre>
	 * 
	 * @param iv
	 * @return
	 */
	private byte[] generateNonce(byte[] iv) {
		byte[] seqNum = new byte[8];
		seqNum[0] = (byte) (epoch >> 8);
		seqNum[1] = (byte) (epoch);
		seqNum[2] = (byte) (sequenceNumber >> 40);
		seqNum[3] = (byte) (sequenceNumber >> 32);
		seqNum[4] = (byte) (sequenceNumber >> 24);
		seqNum[5] = (byte) (sequenceNumber >> 16);
		seqNum[6] = (byte) (sequenceNumber >> 8);
		seqNum[7] = (byte) (sequenceNumber);

		byte[] nonce = new byte[12];
		System.arraycopy(iv, 0, nonce, 0, 4);
		System.arraycopy(seqNum, 0, nonce, 4, 8);

		return nonce;
	}

	/**
	 * See <a href="http://tools.ietf.org/html/rfc5246#section-6.2.3.3">RFC
	 * 5246</a>: additional_data = seq_num + TLSCompressed.type +
	 * TLSCompressed.version + TLSCompressed.length; where "+" denotes
	 * concatenation.
	 * 
	 * @return
	 */
	private byte[] generateAdditionalData(int length) {
		DatagramWriter writer = new DatagramWriter();

		// write uint48 sequence number (since int is only 32 bits, we take a
		// long)
		// see http://tools.ietf.org/html/rfc6347#section-4.1
		// TODO sequenceNumber = 281474976710655L; does not work, we need
		// unsigned bytes, implement this in DatagramWriter
		byte[] sequenceNumberBytes = new byte[SEQUENCE_NUMBER_BYTES];
		sequenceNumberBytes[0] = (byte) (sequenceNumber >> 40);
		sequenceNumberBytes[1] = (byte) (sequenceNumber >> 32);
		sequenceNumberBytes[2] = (byte) (sequenceNumber >> 24);
		sequenceNumberBytes[3] = (byte) (sequenceNumber >> 16);
		sequenceNumberBytes[4] = (byte) (sequenceNumber >> 8);
		sequenceNumberBytes[5] = (byte) (sequenceNumber);
		writer.writeBytes(sequenceNumberBytes);

		writer.write(type.getCode(), CONTENT_TYPE_BITS);

		writer.write(version.getMajor(), VERSION_BITS);
		writer.write(version.getMinor(), VERSION_BITS);
		
		writer.write(length, LENGHT_BITS);

		return writer.toByteArray();
	}

	// Getters and Setters ////////////////////////////////////////////

	public ContentType getType() {
		return type;
	}

	public void setType(ContentType type) {
		this.type = type;
	}

	public ProtocolVersion getVersion() {
		return version;
	}

	public void setVersion(ProtocolVersion version) {
		this.version = version;
	}

	public int getEpoch() {
		return epoch;
	}

	public void setEpoch(int epoch) {
		this.epoch = epoch;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public DTLSSession getSession() {
		return session;
	}

	public void setSession(DTLSSession session) {
		this.session = session;
	}

	/**
	 * So far, the fragment is in its raw binary format. Decrypt (if necessary)
	 * under current read state and serialize it.
	 * 
	 * @return the fragment
	 */
	public DTLSMessage getFragment() {
		if (fragment == null) {
			// decide, which type of fragment need decryption
			switch (type) {
			case ALERT:
				byte[] decryptedMessage = decryptFragment(fragmentBytes);
				fragment = AlertMessage.fromByteArray(decryptedMessage);
				break;

			case APPLICATION_DATA:
				decryptedMessage = decryptFragment(fragmentBytes);
				fragment = ApplicationMessage.fromByteArray(decryptedMessage);
				break;

			case CHANGE_CIPHER_SPEC:
				fragment = ChangeCipherSpecMessage.fromByteArray(fragmentBytes);
				break;

			case HANDSHAKE:
				decryptedMessage = decryptFragment(fragmentBytes);

				// TODO check this
				KeyExchangeAlgorithm keyExchangeAlgorithm = null;
				if (session != null) {
					keyExchangeAlgorithm = session.getKeyExchange();
				}
				fragment = HandshakeMessage.fromByteArray(decryptedMessage, keyExchangeAlgorithm);
				break;

			default:
				LOG.severe("Unknown content type: " + type);
				break;
			}
		}

		return fragment;
	}

	/**
	 * Sets the DTLS fragment. At the same time, it creates the corresponding
	 * raw binary representation and encrypts it if necessary (depending on
	 * current connection state).
	 * 
	 * @param fragment
	 *            the DTLS fragment.
	 */
	public void setFragment(DTLSMessage fragment) {

		if (fragmentBytes == null) {
			// serialize fragment and if necessary encrypt byte array

			byte[] byteArray = fragment.toByteArray();

			switch (type) {
			case ALERT:
			case APPLICATION_DATA:
			case HANDSHAKE:
				byteArray = encryptFragment(byteArray);
				break;

			case CHANGE_CIPHER_SPEC:
				break;

			default:
				LOG.severe("Unknown content type: " + type.toString());
				break;
			}
			this.fragmentBytes = byteArray;

		}
		this.fragment = fragment;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("==[ DTLS Message  ]============================================\n");
		sb.append("Content Type: " + type.toString() + "\n");
		sb.append("Version: " + version.getMajor() + ", " + version.getMinor() + "\n");
		sb.append("Epoch: " + epoch + "\n");
		sb.append("Sequence Number: " + sequenceNumber + "\n");
		sb.append("Length: " + length + "\n");
		sb.append(fragment.toString());
		sb.append("===============================================================");

		return sb.toString();
	}

}
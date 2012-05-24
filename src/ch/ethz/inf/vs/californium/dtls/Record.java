package ch.ethz.inf.vs.californium.dtls;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import ch.ethz.inf.vs.californium.coap.Message;
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
	 * @param epoch
	 * @param sequenceNumber
	 * @param fragment
	 * @param handshaker
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

		writer.write(type.getId(), CONTENT_TYPE_BITS);

		writer.write(version.getMajor(), VERSION_BITS);
		writer.write(version.getMinor(), VERSION_BITS);

		writer.write(epoch, EPOCH_BITS);
		
		// TODO write uint48 sequence number
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
				Cipher cipher = Cipher.getInstance(cipherSuite.getBulkCipher().toString());
				cipher.init(Cipher.ENCRYPT_MODE, session.getWriteState().getEncryptionKey(), session.getWriteState().getIv(), new SecureRandom());

				encryptedFragment = cipher.doFinal(byteArray);
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
				Cipher cipher = Cipher.getInstance(cipherSuite.getBulkCipher().toString());
				cipher.init(Cipher.DECRYPT_MODE, session.getReadState().getEncryptionKey(), session.getReadState().getIv(), new SecureRandom());

				fragment = cipher.doFinal(byteArray);
			} catch (Exception e) {
				LOG.severe("Could not decrypt DTLS application data!");
				e.printStackTrace();
			}
		}

		return fragment;
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
	 * and serialize it.
	 * 
	 * @param handshaker
	 * @return
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
				fragment = HandshakeMessage.fromByteArray(decryptedMessage);
				break;

			default:
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
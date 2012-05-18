package ch.ethz.inf.vs.californium.dtls;

import java.security.SecureRandom;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

public class Record {

	// Logging ////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(Record.class.getName());

	// CoAP-specific constants/////////////////////////////////////////

	private static final int CONTENT_TYPE_BITS = 8;

	private static final int VERSION_BITS = 8; // for major and minor each

	private static final int EPOCH_BITS = 16;

	private static final int SEQUENCE_NUMBER_BITS = 48;

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
	private int sequenceNumber = -1;

	/** The length (in bytes) of the following {@link DTLSMessage}. */
	private int length = 0;

	/**
	 * The application data. This data is transparent and treated as an
	 * independent block to be dealt with by the higher-level protocol specified
	 * by the type field.
	 */
	private DTLSMessage fragment = null;

	/** The raw byte representation of the fragment. */
	private byte[] fragmentBytes;

	// Constructors ///////////////////////////////////////////////////

	public Record(ContentType type, ProtocolVersion version, int epoch, int sequenceNumber, int length, DTLSMessage fragment) {
		this.type = type;
		this.version = version;
		this.epoch = epoch;
		this.sequenceNumber = sequenceNumber;
		this.length = length;
		this.fragment = fragment;
	}

	public Record(ContentType type, ProtocolVersion version, int epoch, int sequenceNumber, int length, byte[] fragmentBytes) {
		this.type = type;
		this.version = version;
		this.epoch = epoch;
		this.sequenceNumber = sequenceNumber;
		this.length = length;
		this.fragmentBytes = fragmentBytes;
	}

	public Record(ContentType type, int epoch, DTLSMessage fragment) {
		this.type = type;
		this.epoch = 0;
		this.length = fragment.getLength();
		this.fragment = fragment;
	}

	// Serialization //////////////////////////////////////////////////

	/**
	 * Encodes the DTLS Record into its raw binary structure as defined in the
	 * DTLS v.1.2 specification.
	 * 
	 * @return the encoded byte array
	 */
	public byte[] toByteArray(Handshaker handshaker) {
		DatagramWriter writer = new DatagramWriter();

		writer.write(type.getId(), CONTENT_TYPE_BITS);

		writer.write(version.getMajor(), VERSION_BITS);
		writer.write(version.getMinor(), VERSION_BITS);

		writer.write(epoch, EPOCH_BITS);

		writer.write(sequenceNumber, SEQUENCE_NUMBER_BITS);

		boolean needsEncryption = false;
		// decide if fragment needs to be encrypted
		switch (type) {
		case APPLICATION_DATA:
		case ALERT:
			// application data and alert are always encrypted with the current
			// connection state
			needsEncryption = true;
			break;
		case HANDSHAKE:
			HandshakeMessage handshakeMessage = (HandshakeMessage) fragment;
			if (handshakeMessage.getMessageType() == HandshakeType.FINISHED) {
				// Finished message is the only one, that needs encryption
				// needsEncryption = true;
			}
			break;

		default:
			break;
		}

		byte[] fragmentBytes = null;
		if (needsEncryption) {
			fragmentBytes = encryptFragment(fragment.toByteArray(), handshaker);
			length = fragmentBytes.length; // length changes due to encryption
		} else {
			fragmentBytes = fragment.toByteArray();
		}

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
		int sequenceNumber = reader.read(SEQUENCE_NUMBER_BITS);
		int length = reader.read(LENGHT_BITS);

		byte[] fragmentBytes = reader.readBytes(length);

		return new Record(contentType, version, epoch, sequenceNumber, length, fragmentBytes);
	}

	// Cryptography /////////////////////////////////////////////////////////

	private byte[] encryptFragment(byte[] fragment, Handshaker handshaker) {
		byte[] encryptedFragment = null;
		try {
			Cipher cipher = Cipher.getInstance("AES");
			if (handshaker instanceof ClientHandshaker) {
				cipher.init(Cipher.ENCRYPT_MODE, handshaker.getServerWriteKey(), handshaker.getServerWriteIV(), new SecureRandom());
			} else {
				cipher.init(Cipher.ENCRYPT_MODE, handshaker.getClientWriteKey(), handshaker.getClientWriteIV(), new SecureRandom());
			}

			encryptedFragment = cipher.doFinal(fragment);

		} catch (Exception e) {
			LOG.severe("Could not encrypt DTLS application data!");
			e.printStackTrace();
		}

		return encryptedFragment;
	}

	private static byte[] decryptFragment(byte[] encryptedFragment, Handshaker handshaker) {
		byte[] fragment = null;
		try {
			Cipher cipher = Cipher.getInstance("AES");
			if (handshaker instanceof ServerHandshaker) {
				// we are acting as a server, decrypt message with
				// server's keys
				cipher.init(Cipher.DECRYPT_MODE, handshaker.getServerWriteKey(), handshaker.getServerWriteIV(), new SecureRandom());
			} else {
				// we are acting as a client, decrypt message with
				// client's keys
				cipher.init(Cipher.DECRYPT_MODE, handshaker.getClientWriteKey(), handshaker.getClientWriteIV(), new SecureRandom());
			}

			fragment = cipher.doFinal(encryptedFragment);
		} catch (Exception e) {
			LOG.severe("Could not decrypt DTLS application data!");
			e.printStackTrace();
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

	public int getSequenceNumber() {
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

	/**
	 * So far, the fragment is in its raw binary format. Decrypt (if necessary)
	 * and serialize it.
	 * 
	 * @param handshaker
	 * @return
	 */
	public DTLSMessage getFragment(Handshaker handshaker) {
		if (fragment == null) {
			// decide, which type of fragment need decryption
			switch (type) {
			case ALERT:
				byte[] decryptedMessage = decryptFragment(fragmentBytes, handshaker);
				fragment = AlertMessage.fromByteArray(decryptedMessage);
				break;

			case APPLICATION_DATA:
				decryptedMessage = decryptFragment(fragmentBytes, handshaker);
				fragment = ApplicationMessage.fromByteArray(decryptedMessage);
				break;

			case CHANGE_CIPHER_SPEC:
				fragment = ChangeCipherSpecMessage.fromByteArray(fragmentBytes);
				break;

			case HANDSHAKE:
				// TODO Finished message is Decrypted!
				fragment = HandshakeMessage.fromByteArray(fragmentBytes);
				break;

			default:
				break;
			}
		}

		return fragment;
	}

	public void setFragment(DTLSMessage fragment) {
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
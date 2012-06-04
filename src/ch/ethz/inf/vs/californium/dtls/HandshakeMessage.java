package ch.ethz.inf.vs.californium.dtls;

import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

public abstract class HandshakeMessage implements DTLSMessage {

	// Logging /////////////////////////////////////////////////////////

	protected static final Logger LOG = Logger.getLogger(HandshakeMessage.class.getName());

	// CoAP-specific constants /////////////////////////////////////////

	private static final int MESSAGE_TYPE_BITS = 8;

	private static final int MESSAGE_LENGTH_BITS = 24;

	private static final int MESSAGE_SEQ_BITS = 16;

	private static final int FRAGMENT_OFFSET_BITS = 24;

	private static final int FRAGMENT_LENGTH_BITS = 24;

	// Members //////////////////////////////////////////////////////////

	private int messageSeq;
	
	/**
	 * The number of bytes contained in previous fragments.
	 */
	private int fragmentOffset;

	/**
	 * The length of this fragment. An unfragmented message is a degenerate case
	 * with fragment_offset=0 and fragment_length=length.
	 */
	private int fragmentLength;

	// Constructors /////////////////////////////////////////////////////

	public HandshakeMessage() {

		this.messageSeq = 0;

		// TODO fragmentation
		this.fragmentOffset = 0;
		this.fragmentLength = 0;
	}
	
	// Methods ////////////////////////////////////////////////////////

	/**
	 * Returns the type of the handshake message. See {@link HandshakeType}.
	 * 
	 * @return the {@link HandshakeType}.
	 */
	public abstract HandshakeType getMessageType();

	/**
	 * Must be implemented by each subclass. The length is given in bytes and
	 * only includes the length of the subclass' specific fields (not the
	 * handshake message header).
	 * 
	 * @return the length of the message <strong>in bytes</strong>.
	 */
	public abstract int getMessageLength();

	@Override
	public int getLength() {
		// fixed: message type (1 byte) + message length (3 bytes) + message seq
		// (2 bytes) + fragment offset (3 bytes) + fragment length (3 bytes) =
		// 12 bytes
		return 12 + getMessageLength();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\tHandshake Protocol\n");
		sb.append("\tType: " + getMessageType().toString() + "\n");
		sb.append("\tMessage Sequence: " + messageSeq + " \n");
		sb.append("\tFragment Offset: " + fragmentOffset + "\n");
		sb.append("\tFragment Length: " + fragmentLength + "\n");
		sb.append("\tLength: " + getMessageLength() + "\n");

		return sb.toString();
	}
	
	// Serialization //////////////////////////////////////////////////

	/**
	 * Returns the raw binary representation of the handshake header. The
	 * subclasses are responsible for the specific rest of the fragment.
	 * 
	 * @return the byte representation of the handshake message.
	 */
	public byte[] toByteArray() {
		// create datagram writer to encode message data
		DatagramWriter writer = new DatagramWriter();

		// write fixed-size handshake message header
		writer.write(getMessageType().getCode(), MESSAGE_TYPE_BITS);
		writer.write(getMessageLength(), MESSAGE_LENGTH_BITS);

		writer.write(messageSeq, MESSAGE_SEQ_BITS);

		writer.write(fragmentOffset, FRAGMENT_OFFSET_BITS);
		writer.write(fragmentLength, FRAGMENT_LENGTH_BITS);

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);
		HandshakeType type = HandshakeType.getTypeByCode(reader.read(MESSAGE_TYPE_BITS));

		int length = reader.read(MESSAGE_LENGTH_BITS);

		int messageSeq = reader.read(MESSAGE_SEQ_BITS);

		int fragmentOffset = reader.read(FRAGMENT_OFFSET_BITS);
		int fragmentLength = reader.read(FRAGMENT_LENGTH_BITS);

		HandshakeMessage body = null;

		byte[] bytesLeft = reader.readBytes(length);
		switch (type) {
		case HELLO_REQUEST:
			body = new HelloRequest();
			break;

		case CLIENT_HELLO:
			body = ClientHello.fromByteArray(bytesLeft);
			break;

		case SERVER_HELLO:
			body = ServerHello.fromByteArray(bytesLeft);
			break;

		case HELLO_VERIFY_REQUEST:
			body = HelloVerifyRequest.fromByteArray(bytesLeft);
			break;

		case CERTIFICATE:
			body = CertificateMessage.fromByteArray(bytesLeft);
			break;

		case SERVER_KEY_EXCHANGE:
			// TODO make this variable
			body = ECDHServerKeyExchange.fromByteArray(bytesLeft);
			break;

		case CERTIFICATE_REQUEST:
			body = CertificateRequest.fromByteArray(bytesLeft);
			break;

		case SERVER_HELLO_DONE:
			body = new ServerHelloDone();
			break;

		case CERTIFICATE_VERIFY:
			body = CertificateVerify.fromByteArray(bytesLeft);
			break;

		case CLIENT_KEY_EXCHANGE:
			// TODO make this variable
			body = ECDHClientKeyExchange.fromByteArray(bytesLeft);
			break;

		case FINISHED:
			body = Finished.fromByteArray(bytesLeft);
			break;

		default:
			break;
		}

		body.setFragmentLength(fragmentLength);
		body.setFragmentOffset(fragmentOffset);
		body.setMessageSeq(messageSeq);

		return body;
	}

	// Getters and Setters ////////////////////////////////////////////

	public int getMessageSeq() {
		return messageSeq;
	}

	public void incrementMessageSeq() {
		messageSeq++;
	}

	public int getFragmentOffset() {
		return fragmentOffset;
	}

	public int getFragmentLength() {
		return fragmentLength;
	}

	public void setFragmentLength(int length) {
		this.fragmentLength = length;
	}

	public void setMessageSeq(int messageSeq) {
		this.messageSeq = messageSeq;
	}

	public void setFragmentOffset(int fragmentOffset) {
		this.fragmentOffset = fragmentOffset;
	}

}

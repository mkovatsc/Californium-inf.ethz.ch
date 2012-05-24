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

	private int fragmentOffset;

	private int fragmentLength;

	// Constructors /////////////////////////////////////////////////////

	public HandshakeMessage() {

		this.messageSeq = 0;

		// TODO fragmentation
		this.fragmentOffset = 0;
		this.fragmentLength = 0;
	}

	/**
	 * Returns the type of the handshake message.
	 * 
	 * @return the handshake type.
	 */
	public abstract HandshakeType getMessageType();

	public abstract int getMessageLength();

	@Override
	public int getLength() {
		// message type (1) + message length (3) + message seq (2) + fragment
		// offset (3) + fragment length (3) = 12
		return 12 + getMessageLength();
	}

	/**
	 * Returns the raw binary representation of the handshake header. The
	 * subclasses are responsible for the rest of the fragment.
	 * 
	 * @return the byte[] the byte representation of the handshake message.
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

		case CERTIFICATE_REQUEST:
			// TODO
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

}

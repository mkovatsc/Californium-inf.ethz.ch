package ch.inf.vs.californium.coap;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import ch.inf.vs.californium.coap.CoAP.Type;

// TODO: Auto-generated Javadoc
/**
 * The Class Message.
 */
public class Message {

	/** The Constant NONE in case no MID has been set. */
	public static final int NONE = -1;
	
	 // For debugging; TODO: remove
 	private static AtomicInteger counter = new AtomicInteger();
	public final int debugID = counter.getAndIncrement();
	
	/** The type. One of {CON, NON, ACK or RST}. */
	private CoAP.Type type;

	/** The 16-bit Message Identification. */
	private int mid = NONE; // Message ID
	
	/** The token, a 0-8 byte array. */
	private byte[] token;
	
	/** The set of options of this message. */
	private OptionSet options;
	
	/** The payload of this message. */
	private byte[] payload;
	
	/** The payload as string. */
	private String payloadString; // lazy variable
	
	/** The destination address of this message. */
	private InetAddress destination;
	
	/** The source address of this message. */
	private InetAddress source;
	
	/** The destination port of this message. */
	private int destinationPort;
	
	/** The source port of this message. */
	private int sourcePort;
	
	/** Indicates if the message has been acknowledged. */
	private boolean acknowledged;
	
	/** Indicates if the message has been rejected. */
	private boolean rejected;
	
	/** Indicates if the message is a duplicate. */
	private boolean duplicate;
	
	/** The serialized message as byte array. */
	private byte[] bytes;
	
	/** Indicates if a layer has decided to intercept and stop this message. */
	private boolean ignored; // For debugging
	
	/** Indicates if this message has been delivered to the server. */
	private boolean delivered; // For debugging
	
	/** TODO */
	private long timestamp;
	
	/**
	 * Instantiates a new message with the given type. The type must be one of CON, NON, ACK or RST.
	 *
	 * @param type the type
	 */
	public Message(Type type) {
		this.type = type;
	}
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public Type getType() {
		return type;
	}
	
	/**
	 * Sets the type.
	 *
	 * @param type the new type
	 */
	public void setType(CoAP.Type type) {
		if (type == null)
			throw new NullPointerException();
		this.type = type;
	}
	
	/**
	 * Checks if this message is confirmable.
	 *
	 * @return true, if is confirmable
	 */
	public boolean isConfirmable() {
		return getType()==Type.CON;
	}
	
	/**
	 * Makes this message a confirmable message with type CON.
	 *
	 * @param con the new confirmable
	 */
	public void setConfirmable(boolean con) {
		setType(con?Type.CON:Type.NON);
	}

	/**
	 * Gets the 16-bit message identification.
	 *
	 * @return the mid
	 */
	public int getMid() {
		return mid;
	}

	/**
	 * Sets the 16-bit message identification.
	 *
	 * @param mid the new mid
	 */
	public void setMid(int mid) {
		if (mid >= 1<<16 || mid < 0)
			throw new IllegalArgumentException("The MID must be a 16-bit number between 0 and "+((1<<16)-1)+" inclusive but was "+mid);
		this.mid = mid;
	}
	
	/**
	 * Gets the 0-8 byte token.
	 *
	 * @return the token
	 */
	public byte[] getToken() {
		return token;
	}

	/**
	 * Sets the 0-8 byte token.
	 *
	 * @param token the new token
	 */
	public void setToken(byte[] token) {
		if (token != null && token.length > 8)
			throw new IllegalArgumentException("Token length must be between 0 and 8 inclusive");
		this.token = token;
	}
	
	/**
	 * Gets the set of options. If no set has been defined yet, it creates a new
	 * one. EmptyMessages should not have any options.
	 * 
	 * @return the options
	 */
	public OptionSet getOptions() {
		if (options == null)
			options = new OptionSet();
		return options;
	}
	
	/**
	 * Sets the set of options. This function makes a defensive copy of the
	 * specified set of options.
	 * 
	 * @param options the new options
	 */
	public void setOptions(OptionSet options) {
		this.options = options;
	}
	
	/**
	 * Gets the payload.
	 *
	 * @return the payload
	 */
	public byte[] getPayload() {
		return payload;
	}
	
	/**
	 * Gets the payload in the form of a string. Returns null if no payload is
	 * defined.
	 * 
	 * @return the payload as string
	 */
	public String getPayloadString() {
		if (payload==null)
			return null;
		this.payloadString = new String(payload);
		return payloadString;
	}
	
	/**
	 * Gets the size (amount of bytes) of the payload.
	 *
	 * @return the payload size
	 */
	public int getPayloadSize() {
		return payload == null ? 0 : payload.length;
	}
	
	public void setPayload(String payload) {
		setPayload(payload.getBytes());
	}
	
	/**
	 * Sets the payload.
	 *
	 * @param payload the new payload
	 */
	public void setPayload(byte[] payload) {
		this.payload = payload;
		this.payloadString = null;
	}

	/**
	 * Gets the destination address.
	 *
	 * @return the destination
	 */
	public InetAddress getDestination() {
		return destination;
	}

	/**
	 * Sets the destination address.
	 *
	 * @param destination the new destination
	 */
	public void setDestination(InetAddress destination) {
		this.destination = destination;
	}

	/**
	 * Gets the source address.
	 *
	 * @return the source
	 */
	public InetAddress getSource() {
		return source;
	}

	/**
	 * Sets the source address.
	 *
	 * @param source the new source
	 */
	public void setSource(InetAddress source) {
		this.source = source;
	}

	/**
	 * Gets the destination port.
	 *
	 * @return the destination port
	 */
	public int getDestinationPort() {
		return destinationPort;
	}

	/**
	 * Sets the destination port.
	 *
	 * @param destinationPort the new destination port
	 */
	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}

	/**
	 * Gets the source port.
	 *
	 * @return the source port
	 */
	public int getSourcePort() {
		return sourcePort;
	}

	/**
	 * Sets the source port.
	 *
	 * @param sourcePort the new source port
	 */
	public void setSourcePort(int sourcePort) {
		this.sourcePort = sourcePort;
	}
	
	// For debugging
	/**
	 * Marks this message as ignored or not.
	 *
	 * @param b the new ignored
	 */
	public void setIgnored(boolean b) {
		this.ignored = b;
	}
	
	// For debugging
	/**
	 * Marks this message as delivered or not.
	 *
	 * @param b the new delivered
	 */
	public void setDelivered(boolean b) {
		this.delivered = b;
	}
	
	/**
	 * Checks if this message has been delivered or ignored.
	 *
	 * @return true, if successful
	 */
	public boolean hasBeenHandled() {
		return delivered || ignored;
	}

	/**
	 * Checks if is this message has been acknowledged.
	 *
	 * @return true, if is acknowledged
	 */
	public boolean isAcknowledged() {
		return acknowledged;
	}

	/**
	 * Marks this message as acknowledged.
	 *
	 * @param acknowledged if acknowledged
	 */
	public void setAcknowledged(boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

	/**
	 * Checks if this message has been rejected.
	 *
	 * @return true, if is rejected
	 */
	public boolean isRejected() {
		return rejected;
	}

	/**
	 * Marks this message as rejected
	 *
	 * @param rejected if rejected
	 */
	public void setRejected(boolean rejected) {
		this.rejected = rejected;
	}

	/**
	 * Checks if this message is a duplicate.
	 *
	 * @return true, if is a duplicate
	 */
	public boolean isDuplicate() {
		return duplicate;
	}

	/**
	 * Marks this message as a duplicate
	 *
	 * @param duplicate if a duplicate
	 */
	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}

	/**
	 * Gets the serialized message as byte array or null if not serialized yet.
	 *
	 * @return the bytes of the serialized message or null
	 */
	public byte[] getBytes() {
		return bytes;
	}

	/**
	 * Sets the bytes of the serialized message.
	 *
	 * @param bytes the serialized bytes
	 */
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}

package ch.inf.vs.californium.coap;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import ch.inf.vs.californium.coap.CoAP.Type;

public class Message {

	public static final int NONE = -1;
	
	 // For debugging; TODO: remove
	private static AtomicInteger counter = new AtomicInteger();
	public final int debugID = counter.getAndIncrement();
	
	private CoAP.Type type;

	private int mid = NONE; // Message ID
	private byte[] token;
	
	private OptionSet options;
	private byte[] payload;
	private String payloadString; // lazy variable
	
	private InetAddress destination;
	private InetAddress source;
	private int destinationPort;
	private int sourcePort;
	
	private boolean acknowledged;
	private boolean rejected;
	private boolean duplicate;
	
	// encoded as bytes
	private byte[] bytes;
	
	private boolean ignored; // For debugging
	private boolean delivered; // For debugging
	
//	public Message() {
//		this(null);
//		this.type = Type.NCON;
//	}
	
	public Message(Type type) {
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
	
	public void setType(CoAP.Type type) {
		if (type == null)
			throw new NullPointerException();
		this.type = type;
	}
	
	public boolean isConfirmable() {
		return getType()==Type.CON;
	}
	
	public void setConfirmable(boolean con) {
		setType(con?Type.CON:Type.NCON);
	}

	public int getMid() {
		return mid;
	}

	public void setMid(int mid) {
		this.mid = mid;
	}
	
	public byte[] getToken() {
		return token;
	}

	public void setToken(byte[] token) {
		this.token = token;
	}
	
	public OptionSet getOptions() {
		if (options == null)
			options = new OptionSet();
		return options;
	}
	
	public void setOptions(OptionSet options) {
		this.options = options;
	}
	
	public byte[] getPayload() {
		return payload;
	}
	
	public String getPayloadString() {
		if (payload==null)
			return "null";
		this.payloadString = new String(payload);
		return payloadString;
	}
	
	public int getPayloadSize() {
		return payload == null ? 0 : payload.length;
	}
	
	public void setPayload(byte[] payload) {
		this.payload = payload;
		this.payloadString = null;
	}

	public InetAddress getDestination() {
		return destination;
	}

	public void setDestination(InetAddress destination) {
		this.destination = destination;
	}

	public InetAddress getSource() {
		return source;
	}

	public void setSource(InetAddress source) {
		this.source = source;
	}

	public int getDestinationPort() {
		return destinationPort;
	}

	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}

	public int getSourcePort() {
		return sourcePort;
	}

	public void setSourcePort(int sourcePort) {
		this.sourcePort = sourcePort;
	}
	
	// For debugging
	public void setIgnored(boolean b) {
		this.ignored = b;
	}
	
	// For debugging
	public void setDelivered(boolean b) {
		this.delivered = b;
	}
	
	public boolean hasBeenHandled() {
		return delivered || ignored;
	}

	public boolean isAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

	public boolean isRejected() {
		return rejected;
	}

	public void setRejected(boolean rejected) {
		this.rejected = rejected;
	}

	public boolean isDuplicate() {
		return duplicate;
	}

	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
}

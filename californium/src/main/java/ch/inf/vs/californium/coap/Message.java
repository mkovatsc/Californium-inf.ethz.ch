package ch.inf.vs.californium.coap;

import ch.inf.vs.californium.coap.CoAP.Type;

public class Message {

	private CoAP.Type type;

	private int mid; // Message ID
	private byte[] token;
	
	private OptionSet options;
	private byte[] payload;
	private String payloadString; // lazy variable
	
	public Message() {
		this(null);
	}
	
	public Message(Type type) {
		this.type = type;
		this.options = new OptionSet();
	}
	
	public Type getType() {
		return type;
	}
	
	public void setType(CoAP.Type type) {
		this.type = type;
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
		return options;
	}
	
	public void setOptions(OptionSet options) {
		this.options = options;
	}
	
	public byte[] getPayload() {
		return payload;
	}
	
	public String getPayloadString() {
		if (payloadString==null)
			payloadString = String.valueOf(payload);
		return payloadString;
	}
	
	public int getPayloadSize() {
		if (payload==null) return 0;
		else return payload.length;
	}
	
	public void setPayload(byte[] payload) {
		this.payload = payload;
		this.payloadString = null;
	}
}

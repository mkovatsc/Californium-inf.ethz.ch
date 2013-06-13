package ch.inf.vs.californium.coap;

import java.util.Arrays;

import ch.inf.vs.californium.coap.CoAP.Type;

/**
 * EmptyMessage represents an empty CoAP message. An empty message has either
 * the {@link Type} ACK or RST.
 */
public class EmptyMessage extends Message {

	/**
	 * Instantiates a new empty message.
	 *
	 * @param type the message type (ACK or RST)
	 */
	public EmptyMessage(Type type) {
		super(type);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String mid = getMid()==Message.NONE?"none":String.valueOf(getMid());
		return getType()+": MID="+getMid()+", Token="+Arrays.toString(getToken())+", "+getOptions()+", Payload=\""+getPayloadString()+"\"";
	}

	/**
	 * Create a new acknowledgment for the specified message.
	 *
	 * @param message the message to acknowledge
	 * @return the acknowledgment
	 */
	public static EmptyMessage newACK(Message message) {
		EmptyMessage ack = new EmptyMessage(Type.ACK);
		ack.setMid(message.getMid());
		ack.setToken(new byte[0]);
		ack.setDestination(message.getSource());
		ack.setDestinationPort(message.getSourcePort());
		return ack;
	}
	
	/**
	 * Create a new reset message for the specified message.
	 *
	 * @param message the message to reject
	 * @return the reset
	 */
	public static EmptyMessage newRST(Message message) {
		EmptyMessage rst = new EmptyMessage(Type.RST);
		rst.setMid(message.getMid());
		rst.setToken(new byte[0]);
		rst.setDestination(message.getSource());
		rst.setDestinationPort(message.getSourcePort());
		return rst;
	}
	
}

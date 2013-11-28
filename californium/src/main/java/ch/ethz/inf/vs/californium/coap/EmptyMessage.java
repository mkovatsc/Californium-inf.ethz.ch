package ch.ethz.inf.vs.californium.coap;

import java.util.Arrays;

import ch.ethz.inf.vs.californium.coap.CoAP.Type;

/**
 * EmptyMessage represents an empty CoAP message. An empty message has either
 * the message {@link Type} ACK or RST.
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
		String appendix = "";
		// crude way to check nothing extra is set in an empty message
		if (!hasEmptyToken()
				|| getOptions().asSortedList().size()>0
				|| getPayloadSize()>0) {
			String payload = getPayloadString();
			if (payload == null) {
				payload = "no payload";
			} else {
				int len = payload.length();
				if (payload.indexOf("\n")!=-1) payload = payload.substring(0, payload.indexOf("\n"));
				if (payload.length() > 24) payload = payload.substring(0,20);
				payload = "\""+payload+"\"";
				if (payload.length() != len+2) payload += ".. " + payload.length() + " bytes";
			}
			appendix = " NON-EMPTY: Token="+Arrays.toString(getToken())+", "+getOptions()+", "+payload;
		}
		return String.format("%s        MID=%5d%s", getType(), getMID(), appendix);
	}

	/**
	 * Create a new acknowledgment for the specified message.
	 *
	 * @param message the message to acknowledge
	 * @return the acknowledgment
	 */
	public static EmptyMessage newACK(Message message) {
		EmptyMessage ack = new EmptyMessage(Type.ACK);
		ack.setMID(message.getMID());
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
		rst.setMID(message.getMID());
		rst.setToken(new byte[0]);
		rst.setDestination(message.getSource());
		rst.setDestinationPort(message.getSourcePort());
		return rst;
	}
	
}

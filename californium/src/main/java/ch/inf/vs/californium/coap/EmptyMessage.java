package ch.inf.vs.californium.coap;

import java.util.Arrays;

import ch.inf.vs.californium.coap.CoAP.Type;

public class EmptyMessage extends Message {

	public EmptyMessage(Type type) {
		super(type);
	}
	
	@Override
	public String toString() {
		return getType()+": MID="+getMid()+", Token="+Arrays.toString(getToken())+", "+getOptions()+", Payload=\""+getPayloadString()+"\"";
	}

	public static EmptyMessage newACK(Message message) {
		EmptyMessage ack = new EmptyMessage(Type.ACK);
		ack.setMid(message.getMid());
		ack.setToken(new byte[0]);
		ack.setDestination(message.getSource());
		ack.setDestinationPort(message.getSourcePort());
		return ack;
	}
	
	public static EmptyMessage newRST(Message message) {
		EmptyMessage rst = new EmptyMessage(Type.RST);
		rst.setMid(message.getMid());
		rst.setToken(new byte[0]);
		rst.setDestination(message.getSource());
		rst.setDestinationPort(message.getSourcePort());
		return rst;
	}
	
}

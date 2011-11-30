package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.Message;


/*
 * This class describes the functionality of a layer that drops messages
 * with a given probability in order to test retransmissions between
 * MessageLayer and UDPLayer etc.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */

public class AdverseLayer extends UpperLayer {
	
	public AdverseLayer(double txPacketLossProbability, double rxPacketLossProbability) {
		this.txPacketLossProbability = txPacketLossProbability;
		this.rxPacketLossProbability = rxPacketLossProbability;
	}
	
	public AdverseLayer() {
		this(0.01, 0.00);
	}

	@Override
	protected void doSendMessage(Message msg) throws IOException {
		if (Math.random() >= txPacketLossProbability) {
			sendMessageOverLowerLayer(msg);
		} else {
			System.err.printf("[%s] Outgoing message dropped: %s\n",
				getClass().getName(), msg.key());
		}
	}
	
	@Override
	protected void doReceiveMessage(Message msg) {
		if (Math.random() >= rxPacketLossProbability) {
			deliverMessage(msg);
		} else {
			System.err.printf("[%s] Incoming message dropped: %s\n",
				getClass().getName(), msg.key());
		}
	}

	private double txPacketLossProbability;
	private double rxPacketLossProbability;
	
}

/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
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

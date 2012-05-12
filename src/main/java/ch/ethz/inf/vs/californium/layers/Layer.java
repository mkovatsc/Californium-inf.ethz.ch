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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.MessageReceiver;

/**
 * An abstract Layer class that enforced a uniform interface for building a
 * layered communications stack.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public abstract class Layer implements MessageReceiver {

// Logging /////////////////////////////////////////////////////////////////////
	
	protected static final Logger LOG = Logger.getLogger(Layer.class.getName());

// Members /////////////////////////////////////////////////////////////////////

	private List<MessageReceiver> receivers;
	protected int numMessagesSent;
	protected int numMessagesReceived;

// Methods /////////////////////////////////////////////////////////////////////
	
	public void sendMessage(Message msg) throws IOException {

		if (msg != null) {
			doSendMessage(msg);
			++numMessagesSent;
		}
	}

	@Override
	public void receiveMessage(Message msg) {

		if (msg != null) {
			++numMessagesReceived;
			doReceiveMessage(msg);
		}
	}

	protected abstract void doSendMessage(Message msg) throws IOException;

	protected abstract void doReceiveMessage(Message msg);

	protected void deliverMessage(Message msg) {

		// pass message to registered receivers
		if (receivers != null) {
			for (MessageReceiver receiver : receivers) {
				receiver.receiveMessage(msg);
			}
		}
	}

	public void registerReceiver(MessageReceiver receiver) {

		// check for valid receiver
		if (receiver != null && receiver != this) {

			// lazy creation of receiver list
			if (receivers == null) {
				receivers = new ArrayList<MessageReceiver>();
			}

			// add receiver to list
			receivers.add(receiver);
		}
	}

	public void unregisterReceiver(MessageReceiver receiver) {

		// remove receiver from list
		if (receivers != null) {
			receivers.remove(receiver);
		}
	}

	public int getNumMessagesSent() {
		return numMessagesSent;
	}

	public int getNumMessagesReceived() {
		return numMessagesReceived;
	}
}

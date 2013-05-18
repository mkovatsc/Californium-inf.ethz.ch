/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * Copyright (c) 2013, Hauke Mehrtens <hauke@hauke-m.de>
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

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.MessageHandler;

/**
 * The Class Message provides the object representation of a generic message.
 * This class is just a wrapper around a byte[] and provides some extra getters
 * and setter to store some meta informations needed for the low lever transport
 * layers.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, Francesco Corazza and Matthias
 *         Kovatsch
 * @author <a href="mailto:hauke@hauke-m.de">Hauke Mehrtens</a>
 */
public class GenericMessage implements Message {
	private long timestamp = -1;
	private EndpointAddress peerAddress = null;

	private boolean requiresBlockwise = false;

	private int retransmissioned = 0;
	private byte[] msg;

	public GenericMessage(byte[] msg) {
		this.msg = msg;
	}

	public GenericMessage() {

	}

	/**
	 * Returns the timestamp associated with this message.
	 * 
	 * @return The timestamp of the message, in milliseconds
	 */
	@Override
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets the timestamp associated with this message. private EndpointAddress
	 * peerAddress = null;
	 * 
	 * @param timestamp
	 *            the new timestamp, in milliseconds
	 */
	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * This method is overridden by subclasses according to the Visitor Pattern.
	 * 
	 * @param handler
	 *            the handler for this message
	 */
	@Override
	public void handleBy(MessageHandler handler) {
		// do nothing
	}

	@Override
	public void setPeerAddress(EndpointAddress a) {
		peerAddress = a;
	}

	@Override
	public EndpointAddress getPeerAddress() {
		return peerAddress;
	}

	public boolean requiresBlockwise() {
		return requiresBlockwise;
	}

	@Override
	public void requiresBlockwise(boolean value) {
		requiresBlockwise = value;
	}

	@Override
	public byte[] toByteArray() {
		return msg;
	}

	@Override
	public int getRetransmissioned() {
		return retransmissioned;
	}

	public void setRetransmissioned(int retransmissioned) {
		this.retransmissioned = retransmissioned;
	}

	@Override
	public String key() {
		return null;
	}

	@Override
	public void prettyPrint() {

	}
}

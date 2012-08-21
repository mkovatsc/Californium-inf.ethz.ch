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
package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;

/**
 * DTLS messages are grouped into a series of message flights. One flight
 * contains of at least one message and needs to be retransmitted until the
 * peer's next flight has arrived in its total. A flight does not only exist of
 * {@link HandshakeMessage}, but also of {@link AlertMessage} and
 * {@link ChangeCipherSpecMessage}. See <a
 * href="http://tools.ietf.org/html/rfc6347#section-4.2.4">RFC 6347</a> for
 * details.
 * 
 * @author Stefan Jucker
 * 
 */
public class DTLSFlight {

	/**
	 * The DTLS messages that belong to this flight and need to be sent, when
	 * the timeout expires.
	 */
	private List<Record> messages;

	/** The peer's address. */
	private EndpointAddress peerAddress;

	/**
	 * The current DTLS session with the peer. Needed to set the record sequence
	 * number correctly when retransmitted.
	 */
	private DTLSSession session;

	/** The number of retransmissions. */
	private int tries;

	/** The current timeout (in milliseconds). */
	private int timeout;

	/**
	 * Indicates, whether this flight needs retransmission (not every flight
	 * needs retransmission, e.g. Alert).
	 */
	private boolean retransmissionNeeded = true;

	/** The retransmission task. Needed when to cancel the retransmission. */
	private TimerTask retransmitTask;

	/**
	 * Initializes an empty, fresh flight. The timeout is set to 0, it will be
	 * set later by the standard duration.
	 */
	public DTLSFlight() {
		this.messages = new ArrayList<Record>();
		this.tries = 0;
		this.timeout = 0;
	}
	
	public void addMessage(List<Record> message) {
		messages.addAll(message);
	}

	public void addMessage(Record message) {
		messages.add(message);
	}

	public List<Record> getMessages() {
		return messages;
	}

	public EndpointAddress getPeerAddress() {
		return peerAddress;
	}

	public void setPeerAddress(EndpointAddress peerAddress) {
		this.peerAddress = peerAddress;
	}

	public DTLSSession getSession() {
		return session;
	}

	public void setSession(DTLSSession session) {
		this.session = session;
	}

	public int getTries() {
		return tries;
	}

	public void incrementTries() {
		this.tries++;
	}

	public void setTries(int tries) {
		this.tries = tries;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Called, when the flight needs to be retransmitted. Increment the timeout,
	 * here we double it.
	 */
	public void incrementTimeout() {
		this.timeout *= 2;
	}

	public boolean isRetransmissionNeeded() {
		return retransmissionNeeded;
	}

	public void setRetransmissionNeeded(boolean needsRetransmission) {
		this.retransmissionNeeded = needsRetransmission;
	}

	public TimerTask getRetransmitTask() {
		return retransmitTask;
	}

	public void setRetransmitTask(TimerTask retransmitTask) {
		this.retransmitTask = retransmitTask;
	}

}

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

import java.security.MessageDigest;
import java.security.SecureRandom;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;

/**
 * The resuming server handshaker executes an abbreviated handshake when
 * receiving a ClientHello with a set session identifier. It checks whether such
 * a session still exists and if so, generates the new keys from the previously
 * established master secret. The message flow is depicted in <a
 * href="http://tools.ietf.org/html/rfc5246#section-7.3">Figure 2</a>.
 * 
 * @author Stefan Jucker
 * 
 */
public class ResumingServerHandshaker extends ServerHandshaker {
	
	// Members ////////////////////////////////////////////////////////
	
	/** The handshake hash used in the Finished messages. */
	private byte[] handshakeHash;
	
	// Constructor ////////////////////////////////////////////////////

	public ResumingServerHandshaker(EndpointAddress endpointAddress, DTLSSession session) {
		super(endpointAddress, session);
		setSessionToResume(session);
	}
	
	// Methods ////////////////////////////////////////////////////////
	
	/**
	 * Resets the state of a session, such that it can be used to resume it.
	 * 
	 * @param session
	 *            the session to be resumed.
	 */
	private void setSessionToResume(DTLSSession session) {
		session.setActive(false);
		session.setWriteEpoch(0);
		session.setReadEpoch(0);
	}
	
	@Override
	public synchronized DTLSFlight processMessage(Record record) throws HandshakeException {
		DTLSFlight flight = null;

		if (!processMessageNext(record)) {
			return null;
		}

		switch (record.getType()) {
		case ALERT:
			record.getFragment();
			break;

		case CHANGE_CIPHER_SPEC:
			record.getFragment();
			setCurrentReadState();
			session.incrementReadEpoch();
			break;

		case HANDSHAKE:
			HandshakeMessage fragment = (HandshakeMessage) record.getFragment();
			switch (fragment.getMessageType()) {
			case CLIENT_HELLO:
				flight = receivedClientHello((ClientHello) fragment);
				break;

			case FINISHED:
				receivedClientFinished((Finished) fragment);
				break;

			default:
				AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.UNEXPECTED_MESSAGE);
				throw new HandshakeException("Server received unexpected resuming handshake message:\n" + fragment.toString(), alert);
			}

			break;

		default:
			AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
			throw new HandshakeException("Server received not supported record:\n" + record.toString(), alert);
		}
		if (flight == null) {
			Record nextMessage = null;
			// check queued message, if it is now their turn
			for (Record queuedMessage : queuedMessages) {
				if (processMessageNext(queuedMessage)) {
					// queuedMessages.remove(queuedMessage);
					nextMessage = queuedMessage;
				}
			}
			if (nextMessage != null) {
				flight = processMessage(nextMessage);
			}
		}
		LOG.info("DTLS Message processed (" + endpointAddress.toString() + "):\n" + record.toString());
		return flight;
	}
	
	/**
	 * The server generates new keys from the old master secret and sends
	 * ChangeCipherSpec and Finished message. The ClientHello contains a fresh
	 * random value which will be needed to generate the new keys.
	 * 
	 * @param message
	 *            the client's hello message.
	 * @return the server's last flight.
	 */
	private DTLSFlight receivedClientHello(ClientHello message) {

		DTLSFlight flight = new DTLSFlight();
		clientHello = message;
		
		md.update(clientHello.toByteArray());

		clientRandom = clientHello.getRandom();
		serverRandom = new Random(new SecureRandom());

		ServerHello serverHello = new ServerHello(clientHello.getClientVersion(), serverRandom, session.getSessionIdentifier(), session.getCipherSuite(), session.getCompressionMethod(), null);
		flight.addMessage(wrapMessage(serverHello));
		md.update(serverHello.toByteArray());

		generateKeys(session.getMasterSecret());

		ChangeCipherSpecMessage changeCipherSpecMessage = new ChangeCipherSpecMessage();
		flight.addMessage(wrapMessage(changeCipherSpecMessage));
		setCurrentWriteState();
		session.incrementWriteEpoch();

		MessageDigest mdWithServerFinished = null;
		try {
			mdWithServerFinished = (MessageDigest) md.clone();
		} catch (Exception e) {
			LOG.severe("Clone not supported.");
			e.printStackTrace();
		}

		handshakeHash = md.digest();
		Finished finished = new Finished(getMasterSecret(), isClient, handshakeHash);
		flight.addMessage(wrapMessage(finished));

		mdWithServerFinished.update(finished.toByteArray());
		handshakeHash = mdWithServerFinished.digest();
			
		return flight;
	}
	
	/**
	 * Verifies the client's Finished message. If valid, encrypted application
	 * data can be sent, otherwise an Alert must be sent.
	 * 
	 * @param message
	 *            the client's Finished message.
	 * @throws HandshakeException
	 *             if the client's Finished message can not be verified.
	 */
	private void receivedClientFinished(Finished message) throws HandshakeException {

		clientFinished = message;

		message.verifyData(getMasterSecret(), false, handshakeHash);
	}

}

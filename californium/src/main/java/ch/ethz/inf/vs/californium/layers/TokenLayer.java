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
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * This class takes care of unique tokens for each sequence of request/response
 * exchanges.
 * Additionally, the TokenLayer takes care of an overall timeout for each
 * request/response exchange.
 * 
 * @author Matthias Kovatsch
 */
public class TokenLayer extends UpperLayer {

// Members /////////////////////////////////////////////////////////////////////
	
	private Map<String, RequestResponseSequence> exchanges = new HashMap<String, RequestResponseSequence>();

	/** A timer for scheduling overall request timeouts. */
	private Timer timer = new Timer(true);
	
	/** The time to wait for requests to complete, in milliseconds. */
	private int sequenceTimeout;
	
// Nested Classes //////////////////////////////////////////////////////////////
	
	/*
	 * Entity class to keep state of transfers
	 */
	private static class RequestResponseSequence {
		public String key;
		public Request request;
		public TimerTask timeoutTask;
	}
	
	/*
	 * Utility class to provide transaction timeouts
	 */
	private class TimeoutTask extends TimerTask {
		
		private RequestResponseSequence sequence;

		public TimeoutTask(RequestResponseSequence sequence) {
			this.sequence = sequence;
		}
		
		@Override
		public void run() {
			transferTimedOut(sequence);
		}
	}
	
	// Constructors ////////////////////////////////////////////////////////////
	
	public TokenLayer(int sequenceTimeout) {
		// member initialization
		this.sequenceTimeout = sequenceTimeout;
	}
	
	public TokenLayer() {
		this(Properties.std.getInt("DEFAULT_OVERALL_TIMEOUT"));
	}

	// I/O implementation //////////////////////////////////////////////////////
	
	@Override
	protected void doSendMessage(Message msg) throws IOException { 
		
		// set token option if required
		if (msg.requiresToken()) {
			msg.setToken( TokenManager.getInstance().acquireToken(true) );
		}
		
		// use overall timeout for clients (e.g., server crash after separate response ACK)
		if (msg instanceof Request) {
			LOG.info(String.format("Requesting response for %s: %s",  ((Request) msg).getUriPath(), msg.sequenceKey()));
			addExchange((Request) msg);
		} else if (msg.getCode()==CodeRegistry.EMPTY_MESSAGE) {
			LOG.info(String.format("Accepting request: %s", msg.key()));
		} else {
			LOG.info(String.format("Responding request: %s", msg.sequenceKey()));
		}
		
		sendMessageOverLowerLayer(msg);
	}	
	
	@Override
	protected void doReceiveMessage(Message msg) {

		if (msg instanceof Response) {

			Response response = (Response) msg;
			
			RequestResponseSequence sequence = getExchange(msg.sequenceKey());

			// check for missing token
			if (sequence == null && response.getToken().length==0) {
				
				LOG.warning(String.format("Remote endpoint failed to echo token: %s", msg.key()));
				
				// TODO try to recover from peerAddress
				
				// let timeout handle the problem
				return;
			}
			
			if (sequence != null) {
				
				// cancel timeout
				sequence.timeoutTask.cancel();
				
				// TODO separate observe registry
				if (msg.getFirstOption(OptionNumberRegistry.OBSERVE)==null) {
					removeExchange(msg.sequenceKey());
				}

				LOG.info(String.format("Incoming response from %s: %s // RTT: %fms", ((Response) msg).getRequest().getUriPath(), msg.sequenceKey(), ((Response) msg).getRTT()));
				
				deliverMessage(msg);
				
			} else {
			
				LOG.warning(String.format("Dropping unexpected response: %s", response.sequenceKey()));
			}
			
		} else if (msg instanceof Request) {
			
			LOG.info(String.format("Incoming request: %s", msg.sequenceKey()));
			
			deliverMessage(msg);
		}
	}
	
	private synchronized RequestResponseSequence addExchange(Request request) {
		
		// be aware when manually setting tokens, as request/response will be replace
		removeExchange(request.sequenceKey());
		
		// create new Transaction
		RequestResponseSequence sequence = new RequestResponseSequence();
		sequence.key = request.sequenceKey();
		sequence.request = request;
		sequence.timeoutTask = new TimeoutTask(sequence);
		
		// associate token with Transaction
		exchanges.put(sequence.key, sequence);
		
		timer.schedule(sequence.timeoutTask, sequenceTimeout);

		LOG.fine(String.format("Stored new exchange: %s", sequence.key));
		
		return sequence;
	}
	
	private RequestResponseSequence getExchange(String key) {
		return exchanges.get(key);
	}
	
	private synchronized void removeExchange(String key) {
		
		RequestResponseSequence exchange = exchanges.remove(key);
		
		if (exchange!=null) {
			
			exchange.timeoutTask.cancel();
			
			TokenManager.getInstance().releaseToken(exchange.request.getToken());
	
			LOG.finer(String.format("Cleared exchange: %s", exchange.key));
		}
	}
	
	private void transferTimedOut(RequestResponseSequence exchange) {
		
		// cancel transaction
		removeExchange(exchange.key);
		
		LOG.warning(String.format("Request/Response exchange timed out: %s", exchange.request.sequenceKey()));
		
		// call event handler
		exchange.request.handleTimeout();
	}
	
	public String getStats() {
		StringBuilder stats = new StringBuilder();
		
		stats.append("Request-Response exchanges: ");
		stats.append(exchanges.size());
		stats.append('\n');
		stats.append("Messages sent:     ");
		stats.append(numMessagesSent);
		stats.append('\n');
		stats.append("Messages received: ");
		stats.append(numMessagesReceived);
		
		return stats.toString();
	}
}

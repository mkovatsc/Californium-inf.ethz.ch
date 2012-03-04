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
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.util.Log;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * This class matches the request/response pairs, also over multiple
 * transactions if required (e.g., separate responses). The central channel
 * identifier is the token option.
 * <p>
 * Additionally, the MatchingLayer takes care of an overall timeout for each
 * request/response exchange.
 * 
 * @author Matthias Kovatsch
 */
public class MatchingLayer extends UpperLayer {

// Members /////////////////////////////////////////////////////////////////////
	
	private Map<String, RequestResponseExchange> exchanges = new HashMap<String, RequestResponseExchange>();

	/** A timer for scheduling overall request timeouts. */
	private Timer timer = new Timer(true);
	
	/** The time to wait for requests to complete, in milliseconds. */
	private int exchangeTimeout;
	
// Nested Classes //////////////////////////////////////////////////////////////
	
	/*
	 * Entity class to keep state of transfers
	 */
	private static class RequestResponseExchange {
		public String key;
		public Request request;
		public TimerTask timeoutTask;
	}
	
	/*
	 * Utility class to provide transaction timeouts
	 */
	private class TimeoutTask extends TimerTask {

		public TimeoutTask(RequestResponseExchange transfer) {
			this.transfer = transfer;
		}
		
		@Override
		public void run() {
			transferTimedOut(transfer);
		}
		
		private RequestResponseExchange transfer;
	}
	
	// Constructors ////////////////////////////////////////////////////////////
	
	public MatchingLayer(int exchangeTimeout) {
		// member initialization
		this.exchangeTimeout = exchangeTimeout;
	}
	
	public MatchingLayer() {
		this(Properties.std.getInt("DEFAULT_OVERALL_TIMEOUT"));
	}

	// I/O implementation //////////////////////////////////////////////////////
	
	@Override
	protected void doSendMessage(Message msg) throws IOException { 
		
		// set token option if required
		if (msg.requiresToken()) {
			msg.setToken( TokenManager.getInstance().acquireToken() );
		}
		
		// use overall timeout for clients (e.g., server crash after separate response ACK)
		if (msg instanceof Request) {
			addExchange((Request) msg);
		} else if (msg.getCode()!=CodeRegistry.EMPTY_MESSAGE) {
			Log.info(this, "Responding to exchange: %s", msg.exchangeKey());
		} else {
			Log.info(this, "Sending empty response: %s", msg.key());
		}
		
		sendMessageOverLowerLayer(msg);
	}	
	
	@Override
	protected void doReceiveMessage(Message msg) {

		if (msg instanceof Response) {

			Response response = (Response) msg;
			
			RequestResponseExchange exchange = getExchange(msg.exchangeKey());

			// check for missing token
			if (exchange == null && response.getToken() == null) {
				
				Log.warning(this, "Remote endpoint failed to echo token: %s", msg.key());
				
				// TODO try to recover from peerAddress
				
				// let timeout handle the problem
				return;
			}
			
			if (exchange != null) {
				
				// attach request to response
				response.setRequest(exchange.request);
				
				// cancel timeout
				exchange.timeoutTask.cancel();
				
				// TODO separate observe registry
				if (msg.getFirstOption(OptionNumberRegistry.OBSERVE)==null) {
					removeExchange(msg.exchangeKey());
				}
				
				deliverMessage(msg);
				
			} else {

//
// TODO check observing relationships for last used MID
// TODO otherwise send RST
//
			
				Log.warning(this, "Dropping unexpected response: %s", response.exchangeKey());
			}
			
		} else if (msg instanceof Request) {
			
			Log.info(this, "New exchange received: %s", msg.exchangeKey());
			
			deliverMessage(msg);
		}
	}
	
	private RequestResponseExchange addExchange(Request request) {
		
		// create new Transaction
		RequestResponseExchange exchange = new RequestResponseExchange();
		exchange.key = request.exchangeKey();
		exchange.request = request;
		exchange.timeoutTask = new TimeoutTask(exchange);
		
		// associate token with Transaction
		exchanges.put(exchange.key, exchange);
		
		timer.schedule(exchange.timeoutTask, exchangeTimeout);

		Log.info(this, "Stored new exchange: %s", exchange.key);
		
		return exchange;
	}
	
	private RequestResponseExchange getExchange(String key) {
		return exchanges.get(key);
	}
	
	private void removeExchange(String key) {
		
		RequestResponseExchange exchange = exchanges.remove(key);
		
		exchange.timeoutTask.cancel();
		exchange.timeoutTask = null;
		
		TokenManager.getInstance().releaseToken(exchange.request.getToken());

		Log.info(this, "Removed exchange: %s", exchange.key);
	}
	
	private void transferTimedOut(RequestResponseExchange exchange) {
		
		// cancel transaction
		removeExchange(exchange.key);
		
		Log.warning(this, "Request/Response exchange timed out: %s", exchange.request.exchangeKey());
		
		// call event handler
		exchange.request.handleTimeout();
	}
}

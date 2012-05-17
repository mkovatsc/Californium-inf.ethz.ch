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
package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;


public class RequestTest {

	class RespondTask extends TimerTask {

		RespondTask(Request request, Response response) {
			this.request = request;
			this.response = response;
		}

		@Override
		public void run() {
			request.respond(response);
			request.sendResponse();
		}

		Request request;
		Response response;

	}
	
	Response handledResponse;
	Timer timer = new Timer();

	@Test
	public void testRespond() {

		// Client Side /////////////////////////////////////////////////////////

		// create new request with own response handler
		Request request = new GETRequest() {
			@Override
			protected void handleResponse(Response response) {
				// change state of outer object
				handledResponse = response;
			}
		};

		/* (...) send the request to server */

		// Server Side /////////////////////////////////////////////////////////

		/* (...) receive request from client */

		// create new response
		Response response = new Response();

		// respond to the request
		request.respond(response);
		request.sendResponse();

		// Validation /////////////////////////////////////////////////////////

		// check if response was handled correctly
		assertSame(response, handledResponse);

	}

	@Test
	public void testReceiveResponse() throws InterruptedException {

		// Client Side /////////////////////////////////////////////////////////

		Request request = new GETRequest();

		// enable response queue in order to perform receiveResponse() calls
		request.enableResponseQueue(true);

		/* (...) send the request to server */

		// Server Side /////////////////////////////////////////////////////////

		/* (...) receive request from client */

		// create new response
		Response response = new Response();

		// schedule delayed response (e.g. take some time for computation etc.)
		timer.schedule(new RespondTask(request, response), 500);

		// Client Side /////////////////////////////////////////////////////////

		// block until response received
		Response receivedResponse = request.receiveResponse();

		// Validation /////////////////////////////////////////////////////////

		// check if response was received correctly
		assertSame(response, receivedResponse);
	}

	@Test
	public void testTokenManager() {

		Set<byte[]> acquiredTokens = new HashSet<byte[]>();
		
		final byte[] emptyToken = new byte[0];
		
		acquiredTokens.add(emptyToken);
		
		System.out.println("Contains: " + acquiredTokens.contains(emptyToken) );
		
		acquiredTokens.remove(emptyToken);
		
		System.out.println("Contains: " + acquiredTokens.contains(emptyToken) );
	}

}

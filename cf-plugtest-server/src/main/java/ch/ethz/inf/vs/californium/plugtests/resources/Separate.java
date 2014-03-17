/*******************************************************************************
 * Copyright (c) 2014, Institute for Pervasive Computing, ETH Zurich.
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
package ch.ethz.inf.vs.californium.plugtests.resources;

import static ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode.*;
import static ch.ethz.inf.vs.californium.coap.MediaTypeRegistry.*;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;


/**
 * This resource implements a test of specification for the
 * ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class Separate extends ResourceBase {

	public Separate() {
		super("separate");
		getAttributes().setTitle("Resource which cannot be served immediately and which cannot be acknowledged in a piggy-backed way");
	}

	@Override
	public void handleGET(CoapExchange exchange) {

		// promise the client that this request will be acted upon by sending an Acknowledgement
		exchange.accept();

		// do the time-consuming computation
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		// get request to read out details
		Request request = exchange.advanced().getRequest();

		String payload = String.format("Type: %d (%s)\nCode: %d (%s)\nMID: %d\n",
									 request.getType().value,
									 request.getType(),
									 request.getCode().value,
									 request.getCode(),
									 request.getMID()
									);

		// complete the request
		exchange.respond(CONTENT, payload, TEXT_PLAIN);
	}
}

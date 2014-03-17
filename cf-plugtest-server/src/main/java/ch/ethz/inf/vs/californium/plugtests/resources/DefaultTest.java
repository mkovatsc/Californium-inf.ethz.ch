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
 * This resource implements a test of specification for the ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class DefaultTest extends ResourceBase {

	public DefaultTest() {
		super("test");
		getAttributes().setTitle("Default test resource");
	}

	@Override
	public void handleGET(CoapExchange exchange) {

		// Check: Type, Code

		StringBuilder payload = new StringBuilder();

		Request request = exchange.advanced().getRequest();
		payload.append(String.format("Type: %d (%s)\nCode: %d (%s)\nMID: %d", 
				request.getType().value, 
				request.getType(), 
				request.getCode().value, 
				request.getCode(), 
				request.getMID()));

		if (request.getToken().length > 0) {
			payload.append("\nToken: ");
			StringBuffer tok = new StringBuffer(request.getToken()==null?"null":"");
			if (request.getToken()!=null) for(byte b:request.getToken()) tok.append(String.format("%02x", b&0xff));
			payload.append(tok);
		}

		if (payload.length() > 64) {
			payload.delete(62, payload.length());
			payload.append('»');
		}
		
		// complete the request
		exchange.setMaxAge(30);
		exchange.respond(CONTENT, payload.toString(), TEXT_PLAIN);
	}

	@Override
	public void handlePOST(CoapExchange exchange) {

		// Check: Type, Code, has Content-Type

		exchange.setLocationPath("/location1/location2/location3");
		exchange.respond(CREATED);
	}

	@Override
	public void handlePUT(CoapExchange exchange) {

		// Check: Type, Code, has Content-Type
		
		if (exchange.getRequestOptions().hasIfNoneMatch()) {
			exchange.respond(PRECONDITION_FAILED);
		} else {
			exchange.respond(CHANGED);
		}
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		// complete the request
		exchange.respond(DELETED);
	}
}

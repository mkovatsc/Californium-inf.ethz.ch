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
package ch.ethz.inf.vs.californium.examples.plugtest;

import java.util.Arrays;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;

/**
 * This resource implements a test of specification for the ETSI IoT CoAP Plugtests, Paris, France, 24 - 25 March 2012.
 * 
 * @author Matthias Kovatsch
 */
public class DefaultTest extends LocalResource {

	private byte[] etagStep3 = new byte[] { 0x00, 0x01 };
	private byte[] etagStep6 = new byte[] { 0x00, 0x02 };
	private byte[] etag;
	
	private boolean ifNoneMatchOkay = true;

	public DefaultTest() {
		super("test");
		setTitle("Default test resource");
	}

	@Override
	public void performGET(GETRequest request) {

		// Check: Type, Code

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);

		StringBuilder payload = new StringBuilder();

		payload.append(String.format("Type: %d (%s)\nCode: %d (%s)\nMID: %d", request.getType().ordinal(), request.typeString(), request.getCode(), CodeRegistry.toString(request.getCode()), request.getMID()));

		if (request.getToken().length > 0) {
			payload.append("\nToken: ");
			payload.append(request.getTokenString());
		}

		if (payload.length() > 64) {
			payload.delete(62, payload.length());
			payload.append('�');
		}

		// set payload
		response.setPayload(payload.toString());
		response.setContentType(MediaTypeRegistry.TEXT_PLAIN);

		Option option = request.getFirstOption(OptionNumberRegistry.ETAG);
		if (option == null) {
			etag = etagStep3;
			response.setOption(new Option(etag, OptionNumberRegistry.ETAG));
		} else {
			if (Arrays.equals(etag, option.getRawValue())) {
				response.setCode(CodeRegistry.RESP_VALID);
				response.setOption(new Option(etagStep3, OptionNumberRegistry.ETAG));
				etag = etagStep6;
			} else {
				response.setOption(new Option(etag, OptionNumberRegistry.ETAG));
			}
		}
		response.setMaxAge(30);

		// complete the request
		request.respond(response);
	}

	@Override
	public void performPOST(POSTRequest request) {

		// Check: Type, Code, has Content-Type

		// create new response
		Response response = new Response(CodeRegistry.RESP_CREATED);

		response.setLocationPath("/location1/location2/location3");

		// complete the request
		request.respond(response);
	}

	@Override
	public void performPUT(PUTRequest request) {

		// Check: Type, Code, has Content-Type

		// create new response
		Response response = new Response(CodeRegistry.RESP_CHANGED);

		Option ifMatch = request.getFirstOption(OptionNumberRegistry.IF_MATCH);
		Option ifNoneMatch = request.getFirstOption(OptionNumberRegistry.IF_NONE_MATCH);
		
		if (ifMatch != null) {
			if (Arrays.equals(ifMatch.getRawValue(), etagStep3)) {
				response.setOption(new Option(etagStep6, OptionNumberRegistry.ETAG));
				etag = etagStep3;
			} else if (Arrays.equals(ifMatch.getRawValue(), etagStep6)) {
				response.setCode(CodeRegistry.RESP_PRECONDITION_FAILED);
			}
		} else if (ifNoneMatch != null) {
			if (ifNoneMatchOkay) {
				response.setCode(CodeRegistry.RESP_CREATED);
				ifNoneMatchOkay = false;
			} else {
				response.setCode(CodeRegistry.RESP_PRECONDITION_FAILED);
				ifNoneMatchOkay = true;
			}
		}

		// complete the request
		request.respond(response);
	}

	@Override
	public void performDELETE(DELETERequest request) {

		// Check: Type, Code, has Content-Type
		
		ifNoneMatchOkay = true;

		// create new response
		Response response = new Response(CodeRegistry.RESP_DELETED);

		// complete the request
		request.respond(response);
	}
}

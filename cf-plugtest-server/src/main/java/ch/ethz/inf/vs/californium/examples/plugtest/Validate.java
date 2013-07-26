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
import java.util.List;

import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.MediaTypeRegistry;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.ResourceBase;

/**
 * This resource implements a test of specification for the ETSI IoT CoAP Plugtests, Paris, France, 24 - 25 March 2012.
 * 
 * @author Matthias Kovatsch
 */
public class Validate extends ResourceBase {

	private byte[] etag;
	
	private boolean ifNoneMatchOkay = true;

	public Validate() {
		super("validate");
		getAttributes().setTitle("Resource which varies");
		
		etag = new byte[3];
		etag[0] = 0x00;
		etag[1] = 0x00;
		etag[2] = 0x00;
	}

	@Override
	public void processGET(Exchange exchange) {
		Request request = exchange.getRequest();
		// create response
		Response response = new Response(ResponseCode.CONTENT);

		StringBuilder payload = new StringBuilder();

		payload.append(
				String.format(
						"Type: %d (%s)\nCode: %d (%s)\nMID: %d", 
						request.getType().value, 
						request.getType(), 
						request.getCode().value, 
						request.getCode(),
						request.getMID()));

		if (request.getToken().length > 0) {
			payload.append("\nToken: ");
			payload.append(request.getTokenString());
		}

		if (payload.length() > 64) {
			payload.delete(62, payload.length());
			payload.append('»');
		}

		// set payload
		response.setPayload(payload.toString());
		response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);

		List<byte[]> etags = request.getOptions().getETags();
		if (etags.isEmpty()) {
			response.getOptions().addETag(etag.clone());
		} else {
			if (Arrays.equals(etag, etags.get(0))) {
				response = new Response(ResponseCode.VALID);
				// payload and Content-Format is removed by the framework
				response.getOptions().addETag(etag.clone());
				etag[0] = 0x00;
				etag[1] = (byte) (0x100 * Math.random());
				etag[2] = (byte) (0x100 * Math.random());
			} else {
				response.getOptions().addETag(etag.clone());
			}
		}
		response.getOptions().setMaxAge(30);

		// complete the request
		exchange.respond(response);
	}

	@Override
	public void processPOST(Exchange exchange) {
		// Check: Type, Code, has Content-Type

		// create new response
		Response response = new Response(ResponseCode.CREATED);

		response.getOptions().setLocationPath("/location1/location2/location3");

		// complete the request
		exchange.respond(response);
	}

	@Override
	public void processPUT(Exchange exchange) {
		Request request = exchange.getRequest();
		// Check: Type, Code, has Content-Type

		// create new response
		Response response = new Response(ResponseCode.CHANGED);

//		Option ifMatch = request.getFirstOption(OptionNumberRegistry.IF_MATCH);
//		Option ifNoneMatch = request.getFirstOption(OptionNumberRegistry.IF_NONE_MATCH);
		
		byte[] ifMatch = null;
		if (request.getOptions().getIfMatchCount() > 0)
			ifMatch = request.getOptions().getIfMatchs().get(0);
		boolean ifNoneMatch = request.getOptions().hasIfNoneMatch();
		
		if (ifMatch != null) {
			if (Arrays.equals(ifMatch, etag)) {
				etag[0] = 0x00;
				etag[1] = (byte) (0x100 * Math.random());
				etag[2] = (byte) (0x100 * Math.random());
//				response.setOption(new Option(etag, OptionNumberRegistry.ETAG));
				response.getOptions().addETag(etag.clone());
			} else {
//				response.setCode(CodeRegistry.RESP_PRECONDITION_FAILED);
				response = new Response(ResponseCode.PRECONDITION_FAILED);
			} 
		} else if (ifNoneMatch) {
			if (ifNoneMatchOkay) {
//				response.setCode(CodeRegistry.RESP_CREATED);
				response = new Response(ResponseCode.CREATED);
				etag[0] = 0x00;
				etag[1] = (byte) (0x100 * Math.random());
				etag[2] = (byte) (0x100 * Math.random());
				ifNoneMatchOkay = false;
			} else {
//				response.setCode(CodeRegistry.RESP_PRECONDITION_FAILED);
				response = new Response(ResponseCode.PRECONDITION_FAILED);
				ifNoneMatchOkay = true;
			}
		} else {
			etag[0] = 0x00;
			etag[1] = (byte) (0x100 * Math.random());
			etag[2] = (byte) (0x100 * Math.random());
//			response.setOption(new Option(etag, OptionNumberRegistry.ETAG));
			response.getOptions().addETag(etag.clone());
		}

		// complete the request
		exchange.respond(response);
	}

	@Override
	public void processDELETE(Exchange exchange) {

		// Check: Type, Code, has Content-Type
		
		ifNoneMatchOkay = true;
		etag[0] = 0x00;
		etag[1] = 0x00;
		etag[2] = 0x00;

		// create new response
		Response response = new Response(ResponseCode.DELETED);

		// complete the request
		exchange.respond(response);
	}
}

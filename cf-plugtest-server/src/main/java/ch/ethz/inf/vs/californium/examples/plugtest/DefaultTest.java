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

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the ETSI IoT CoAP Plugtests, Paris, France, 24 - 25 March 2012.
 * 
 * @author Matthias Kovatsch
 */
public class DefaultTest extends ResourceBase {

	private byte[] etagStep3 = new byte[] { 0x00, 0x01 };
	private byte[] etagStep6 = new byte[] { 0x00, 0x02 };
	private byte[] etag;
	
	private boolean ifNoneMatchOkay = true;

	public DefaultTest() {
		super("test");
		getAttributes().setTitle("Default test resource");
	}

	@Override
	public void handleGET(Exchange exchange) {

		// Check: Type, Code

		// create response
		Response response = new Response(ResponseCode.CONTENT);

		StringBuilder payload = new StringBuilder();

		Request request = exchange.getRequest();
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

		// set payload
		response.setPayload(payload.toString());
		response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);

//		List<Option> etags = request.getOptions(OptionNumberRegistry.ETAG);
		List<byte[]> etags = request.getOptions().getETags();
		if (etags.isEmpty()) {
			etag = etagStep3;
//			response.setOption(new Option(etag, OptionNumberRegistry.ETAG));
			response.getOptions().addETag(etag);
		} else {
			if (Arrays.equals(etag, etags.get(0))) {
//				response.setCode(CodeRegistry.RESP_VALID);
				response = new Response(ResponseCode.VALID);
				// payload and Content-Format is removed by the framework
//				response.setOption(new Option(etagStep3, OptionNumberRegistry.ETAG));
				response.getOptions().addETag(etagStep3);
				etag = etagStep6;
			} else {
//				response.setOption(new Option(etag, OptionNumberRegistry.ETAG));
				response.getOptions().addETag(etag);
			}
		}
		response.getOptions().setMaxAge(30);

		// complete the request
		exchange.respond(response);
	}

	@Override
	public void handlePOST(Exchange exchange) {

		// Check: Type, Code, has Content-Type

		// create new response
		Response response = new Response(ResponseCode.CREATED);

		response.getOptions().setLocationPath("/location1/location2/location3");

		// complete the request
		exchange.respond(response);
	}

	@Override
	public void handlePUT(Exchange exchange) {
		Request request = exchange.getRequest();

		// Check: Type, Code, has Content-Type

		// create new response
		Response response = new Response(ResponseCode.CHANGED);

		List<byte[]> ifmatchs = request.getOptions().getIfMatchs();
		boolean ifNonMatch = request.getOptions().hasIfNoneMatch();
//		Option ifMatch = request.getFirstOption(OptionNumberRegistry.IF_MATCH);
//		Option ifNoneMatch = request.getFirstOption(OptionNumberRegistry.IF_NONE_MATCH);
		
//		if (ifMatch != null) {
		if (!ifmatchs.isEmpty()) {
			if (Arrays.equals(ifmatchs.get(0), etagStep3)) {
//				response.setOption(new Option(etagStep6, OptionNumberRegistry.ETAG));
				response.getOptions().addETag(etagStep6);
				etag = etagStep3;
			} else if (Arrays.equals(ifmatchs.get(0), etagStep6)) {
				
//				response.setCode(CodeRegistry.RESP_PRECONDITION_FAILED);
				response = new Response(ResponseCode.PRECONDITION_FAILED);
			}
//		} else if (ifNoneMatch != null) {
		} else if (ifNonMatch) {
			if (ifNoneMatchOkay) {
//				response.setCode(CodeRegistry.RESP_CREATED);
				response = new Response(ResponseCode.CREATED);
				ifNoneMatchOkay = false;
			} else {
//				response.setCode(CodeRegistry.RESP_PRECONDITION_FAILED);
				response = new Response(ResponseCode.PRECONDITION_FAILED);
				ifNoneMatchOkay = true;
			}
		}

		// complete the request
		exchange.respond(response);
	}

	@Override
	public void handleDELETE(Exchange exchange) {

		// Check: Type, Code, has Content-Type
		
		ifNoneMatchOkay = true;

		// create new response
		Response response = new Response(ResponseCode.DELETED);

		// complete the request
		exchange.respond(response);
	}
}

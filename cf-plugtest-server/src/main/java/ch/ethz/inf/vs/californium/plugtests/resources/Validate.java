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

import java.nio.ByteBuffer;

import static ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode.*;
import static ch.ethz.inf.vs.californium.coap.MediaTypeRegistry.*;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class Validate extends ResourceBase {

	private byte[] data = null;
	private int dataCf = TEXT_PLAIN;
	private byte[] etag = {0,0,0,0};

	public Validate() {
		super("validate");
		getAttributes().setTitle("Resource which varies");
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		
		// get request to read out details
		Request request = exchange.advanced().getRequest();

		// successively create response
		Response response;
		
		if (exchange.getRequestOptions().containsETag(etag)) {
			
			response = new Response(VALID);
			response.getOptions().addETag(etag.clone());
			
			// automatically change now
			storeData(null, UNDEFINED);
		} else {
			response = new Response(CONTENT);

			if (data==null) {
				etag = ByteBuffer.allocate(2).putShort( (short) (Math.random()*0x10000) ).array();
				
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
					payload.delete(63, payload.length());
					payload.append('»');
				}
				response.setPayload(payload.toString());
				response.getOptions().setContentFormat(TEXT_PLAIN);
			} else {
				response.setPayload(data);
				response.getOptions().setContentFormat(dataCf);
			}
			response.getOptions().addETag(etag.clone());
		}
		exchange.respond(response);
	}

	@Override
	public void handlePUT(CoapExchange exchange) {
		
		if (exchange.getRequestOptions().isIfMatch(etag)) {
			if (exchange.getRequestOptions().hasContentFormat()) {
				storeData(exchange.getRequestPayload(), exchange.getRequestOptions().getContentFormat());
				exchange.setETag(etag.clone());
				exchange.respond(CHANGED);
			} else {
				exchange.respond(BAD_REQUEST, "Content-Format not set");
			}
		} else if (exchange.getRequestOptions().hasIfNoneMatch() && data==null) {
			storeData(exchange.getRequestPayload(), exchange.getRequestOptions().getContentFormat());
			exchange.respond(CREATED);
		} else {
			exchange.respond(PRECONDITION_FAILED);
			// automatically change now
			storeData(null, UNDEFINED);
		}
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		storeData(null, UNDEFINED);
		exchange.respond(DELETED);
	}

	// Internal ////////////////////////////////////////////////////////////////
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private synchronized void storeData(byte[] payload, int cf) {
		
		if (payload!=null) {
			data = payload;
			dataCf = cf;
			
			etag = ByteBuffer.allocate(4).putInt( data.hashCode() ).array();
	
			// set payload and content type
			getAttributes().clearContentType();
			getAttributes().addContentType(dataCf);
			getAttributes().setMaximumSizeEstimate(data.length);
		} else {
			data = null;
			etag = ByteBuffer.allocate(2).putShort( (short) (Math.random()*0x10000) ).array();
		}
		
		// signal that resource state changed
		changed();
	}
}

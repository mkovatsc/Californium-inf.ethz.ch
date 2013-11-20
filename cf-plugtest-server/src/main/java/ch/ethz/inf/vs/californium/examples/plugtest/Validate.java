/*******************************************************************************
 * Copyright (c) 2013, Institute for Pervasive Computing, ETH Zurich.
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

import java.nio.ByteBuffer;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class Validate extends ResourceBase {

	private byte[] data = null;
	private int dataCt = MediaTypeRegistry.TEXT_PLAIN;
	private byte[] etag = {0,0,0,0};

	public Validate() {
		super("validate");
		getAttributes().setTitle("Resource which varies");
	}

	@Override
	public void handleGET(Exchange exchange) {

		Request request = exchange.getRequest();
		Response response;
		
		if (request.getOptions().containsETag(etag)) {
			response = new Response(ResponseCode.VALID);
			response.getOptions().addETag(etag.clone());
			
			// automatically change now
			storeData(null);
		} else {
			response = new Response(ResponseCode.CONTENT);

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
					payload.delete(62, payload.length());
					payload.append('»');
				}
				response.setPayload(payload.toString());
				response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
			} else {
				response.setPayload(data);
				response.getOptions().setContentFormat(dataCt);
			}
			response.getOptions().addETag(etag.clone());
		}
		exchange.respond(response);
	}

	@Override
	public void handlePUT(Exchange exchange) {
		Request request = exchange.getRequest();
		Response response;
		
		if (request.getOptions().getIfMatch(etag)) {
			if (exchange.getRequest().getOptions().hasContentFormat()) {
				storeData(exchange.getRequest());

				response = new Response(ResponseCode.CHANGED);
				response.getOptions().addETag(etag.clone());
				exchange.respond(response);
			} else {
				exchange.respond(ResponseCode.BAD_REQUEST, "Content-Format not set");
			}
		} else if (request.getOptions().hasIfNoneMatch() && data==null) {
			storeData(exchange.getRequest());
			
			response = new Response(ResponseCode.CREATED);
			exchange.respond(response);
		} else {
			exchange.respond(ResponseCode.PRECONDITION_FAILED);
			storeData(null);
		}
	}

	@Override
	public void handleDELETE(Exchange exchange) {
		storeData(null);
		exchange.respond(ResponseCode.DELETED);
	}

	// Internal ////////////////////////////////////////////////////////////////
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private synchronized void storeData(Request request) {
		
		if (request!=null) {
			data = request.getPayload();
			dataCt = request.getOptions().getContentFormat();
			
			etag = ByteBuffer.allocate(4).putInt( data.hashCode() ).array();
	
			// set payload and content type
			getAttributes().clearContentType();
			getAttributes().addContentType(dataCt);
			getAttributes().setMaximumSizeEstimate(data.length);
		} else {
			data = null;
			etag = ByteBuffer.allocate(2).putShort( (short) (Math.random()*0x10000) ).array();
		}
		
		// signal that resource state changed
		changed();
	}
}

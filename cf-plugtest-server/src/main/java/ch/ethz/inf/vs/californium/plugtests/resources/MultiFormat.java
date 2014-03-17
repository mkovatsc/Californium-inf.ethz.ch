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
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class MultiFormat extends ResourceBase {

	public MultiFormat() {
		super("multi-format");
		getAttributes().setTitle("Resource that exists in different content formats (text/plain utf8 and application/xml)");
		getAttributes().addContentType(0);
		getAttributes().addContentType(41);
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		
		// get request to read out details
		Request request = exchange.advanced().getRequest();
		
		// successively create response
		Response response = new Response(CONTENT);

		String format = "";
		switch (exchange.getRequestOptions().getAccept()) {
			case UNDEFINED:
			case TEXT_PLAIN:
				response.getOptions().setContentFormat(TEXT_PLAIN);
				format = "Status type: \"%s\"\nCode: \"%s\"\nMID: \"%s\"\nAccept: \"%s\"";
				break;
	
			case APPLICATION_XML:
				response.getOptions().setContentFormat(APPLICATION_XML);
				format = "<msg type=\"%s\" code=\"%s\" mid=%s accept=\"%s\"/>"; // should fit 64 bytes
				break;
	
			default:
				response = new Response(NOT_ACCEPTABLE);
				format = "text/plain or application/xml only";
				break;
		}
		
		response.setPayload( 
				String.format(format, 
						request.getType(), 
						request.getCode(), 
						request.getMID(),
						MediaTypeRegistry.toString(request.getOptions().getAccept())) 
				);

		exchange.respond(response);
	}

}

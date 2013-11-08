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

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the
 * ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class LargeUpdate extends ResourceBase {

// Members ////////////////////////////////////////////////////////////////

	private byte[] data = null;
	private int dataCt = MediaTypeRegistry.TEXT_PLAIN;

// Constructors ////////////////////////////////////////////////////////////
	
	/*
	 * Default constructor.
	 */
	public LargeUpdate() {
		this("large-update");
		
		StringBuilder builder = new StringBuilder();
		builder.append("/-------------------------------------------------------------\\\n");
		builder.append("|                 RESOURCE BLOCK NO. 1 OF 5                   |\n");
		builder.append("|               [each line contains 64 bytes]                 |\n");
		builder.append("\\-------------------------------------------------------------/\n");
		builder.append("/-------------------------------------------------------------\\\n");
		builder.append("|                 RESOURCE BLOCK NO. 2 OF 5                   |\n");
		builder.append("|               [each line contains 64 bytes]                 |\n");
		builder.append("\\-------------------------------------------------------------/\n");
		builder.append("/-------------------------------------------------------------\\\n");
		builder.append("|                 RESOURCE BLOCK NO. 3 OF 5                   |\n");
		builder.append("|               [each line contains 64 bytes]                 |\n");
		builder.append("\\-------------------------------------------------------------/\n");
		builder.append("/-------------------------------------------------------------\\\n");
		builder.append("|                 RESOURCE BLOCK NO. 4 OF 5                   |\n");
		builder.append("|               [each line contains 64 bytes]                 |\n");
		builder.append("\\-------------------------------------------------------------/\n");
		builder.append("/-------------------------------------------------------------\\\n");
		builder.append("|                 RESOURCE BLOCK NO. 5 OF 5                   |\n");
		builder.append("|               [each line contains 64 bytes]                 |\n");
		builder.append("\\-------------------------------------------------------------/\n");
		
		data = builder.toString().getBytes();
	}
	
	/*
	 * Constructs a new storage resource with the given resourceIdentifier.
	 */
	public LargeUpdate(String resourceIdentifier) {
		super(resourceIdentifier);
		getAttributes().setTitle("Large resource that can be updated using PUT method");
		getAttributes().addResourceType("block");
		getAttributes().setMaximumSizeEstimate(1280);
	}

	// REST Operations /////////////////////////////////////////////////////////
	
	@Override
	public void handleGET(Exchange exchange) {

		if (exchange.getRequest().getOptions().hasAccept()
				&& exchange.getRequest().getOptions().getAccept() != dataCt) {
			exchange.respond(ResponseCode.NOT_ACCEPTABLE, MediaTypeRegistry.toString(dataCt) + " only");
		} else {
			// create response
			Response response = new Response(ResponseCode.CONTENT);
			// load data into payload
			response.setPayload(data);
			// set content type
			response.getOptions().setContentFormat(dataCt);
			// complete the request
			exchange.respond(response);
		}
	}
	
	@Override
	public void handlePUT(Exchange exchange) {
		
		if (exchange.getRequest().getOptions().hasContentFormat()) {
			storeData(exchange.getRequest());
			exchange.respond(ResponseCode.CHANGED);
		} else {
			exchange.respond(ResponseCode.BAD_REQUEST, "Content-Format not set");
		}
	}

	// Internal ////////////////////////////////////////////////////////////////
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private synchronized void storeData(Request request) {

		// set payload and content type
		data = request.getPayload();
		dataCt = request.getOptions().getContentFormat();
		getAttributes().clearContentType();
		getAttributes().addContentType(dataCt);
		getAttributes().setMaximumSizeEstimate(data.length);

		// signal that resource state changed
		changed();
	}
}

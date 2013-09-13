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

import java.util.ArrayList;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the
 * ETSI IoT CoAP Plugtests, Paris, France, 24 - 25 March 2012.
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
	
	/*
	 * GETs the content of this storage resource. 
	 * If the content-type of the request is set to application/link-format 
	 * or if the resource does not store any data, the contained sub-resources
	 * are returned in link format.
	 */
	@Override
	public void processGET(Exchange exchange) {
		
		// content negotiation
		ArrayList<Integer> supported = new ArrayList<Integer>();
		supported.add(dataCt);

		int ct = MediaTypeRegistry.IMAGE_PNG;
//		if ((ct = MediaTypeRegistry.contentNegotiation(dataCt,  supported, request.getOptions(OptionNumberRegistry.ACCEPT)))==MediaTypeRegistry.UNDEFINED) {
//			exchange.respond(ResponseCode.NOT_ACCEPTABLE, "Accept " + MediaTypeRegistry.toString(dataCt));
//			return;
//		}
		
		if ((ct = exchange.getRequest().getOptions().getAccept()) != dataCt) {
			exchange.respond(ResponseCode.NOT_ACCEPTABLE, "Accept " + MediaTypeRegistry.toString(dataCt));
			return;
		}

		// create response
		Response response = new Response(ResponseCode.CONTENT);
		
		if (data==null) {
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
			
			response.setPayload(builder.toString());
			response.getOptions().setContentFormat(ct);
			exchange.respond(response);
			
		} else {

			// load data into payload
			response.setPayload(data);
	
			// set content type
			response.getOptions().setContentFormat(ct);
	
			// complete the request
			exchange.respond(response);
		}
	}
	
	/*
	 * PUTs content to this resource.
	 */
	@Override
	public void processPUT(Exchange exchange) {
		Request request = exchange.getRequest();

		if (request.getOptions().getContentFormat()==MediaTypeRegistry.UNDEFINED) {
			exchange.respond(ResponseCode.BAD_REQUEST, "Content-Type not set");
			return;
		}
		
		// store payload
		storeData(request);

		// complete the request
		exchange.respond(ResponseCode.CHANGED);
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

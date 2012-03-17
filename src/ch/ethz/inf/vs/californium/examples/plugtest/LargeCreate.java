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

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;

/**
 * This resource implements a test of specification for the
 * ETSI IoT CoAP Plugtests, Paris, France, 24 - 25 March 2012.
 * 
 * @author Matthias Kovatsch
 */
public class LargeCreate extends LocalResource {

// Members ////////////////////////////////////////////////////////////////

	private byte[] data = null;
	private int dataCt = -1;

// Constructors ////////////////////////////////////////////////////////////
	
	/*
	 * Default constructor.
	 */
	public LargeCreate() {
		this("large-create");
	}
	
	/*
	 * Constructs a new storage resource with the given resourceIdentifier.
	 */
	public LargeCreate(String resourceIdentifier) {
		super(resourceIdentifier, false);
		setTitle("Large resource that can be created using POST method");
		setResourceType("block");
	}

	// REST Operations /////////////////////////////////////////////////////////
	
	@Override
	public void performGET(GETRequest request) {

		Response response = null;
		
		if (data==null) {
			
			response = new Response(CodeRegistry.RESP_CONTENT);
			response.setPayload("Nothing POSTed yet", MediaTypeRegistry.TEXT_PLAIN);
			
		} else {
			
			// content negotiation
			ArrayList<Integer> supported = new ArrayList<Integer>();
			supported.add(dataCt);

			int ct = dataCt;
			if ((ct = MediaTypeRegistry.contentNegotiation(dataCt,  supported, request.getOptions(OptionNumberRegistry.ACCEPT)))==MediaTypeRegistry.UNDEFINED) {
				request.respond(CodeRegistry.RESP_NOT_ACCEPTABLE, "Accept " + MediaTypeRegistry.toString(dataCt));
				return;
			}
			
			response = new Response(CodeRegistry.RESP_CONTENT);

			// load data into payload
			response.setPayload(data);
	
			// set content type
			response.setContentType(ct);
	
		}
		
		// complete the request
		request.respond(response);
	}
	
	/*
	 * POST content to create this resource.
	 */
	@Override
	public void performPOST(POSTRequest request) {

		if (request.getContentType()==MediaTypeRegistry.UNDEFINED) {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Content-Type not set");
			return;
		}
		
		// store payload
		storeData(request);

		// create new response
		Response response = new Response(CodeRegistry.RESP_CREATED);

		// inform client about the location of the new resource
		response.setLocationPath("/nirvana");

		// complete the request
		request.respond(response);
	}
	
	/*
	 * DELETE the data and act as resouce was deleted.
	 */
	@Override
	public void performDELETE(DELETERequest request) {

		// delete
		data = null;

		// complete the request
		request.respond(new Response(CodeRegistry.RESP_DELETED));
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
		dataCt = request.getContentType();
		clearAttribute(LinkFormat.CONTENT_TYPE);
		setContentTypeCode(dataCt);

		// signal that resource state changed
		changed();
	}
}

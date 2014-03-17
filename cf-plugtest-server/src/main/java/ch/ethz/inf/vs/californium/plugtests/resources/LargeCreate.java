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
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the
 * ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class LargeCreate extends ResourceBase {

// Members /////////////////////////////////////////////////////////////////
	
	private int counter = 0;

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
		super(resourceIdentifier);
		getAttributes().setTitle("Large resource that can be created using POST method");
		getAttributes().addResourceType("block");
	}

	// REST Operations /////////////////////////////////////////////////////////

	/*
	 * GET Link Format list of created sub-resources.
	 */
	@Override
	public void handleGET(CoapExchange exchange) {
		String subtree = LinkFormat.serializeTree(this);
		exchange.respond(CONTENT, subtree, APPLICATION_LINK_FORMAT);
	}
	
	/*
	 * POST content to create a sub-resource.
	 */
	@Override
	public void handlePOST(CoapExchange exchange) {
		
		if (exchange.getRequestOptions().hasContentFormat()) {
			exchange.setLocationPath( storeData(exchange.getRequestPayload(), exchange.getRequestOptions().getContentFormat()) );
			exchange.respond(CREATED);
		} else {
			exchange.respond(BAD_REQUEST, "Content-Format not set");
		}
	}

	// Internal ////////////////////////////////////////////////////////////////
	
	private class StorageResource extends ResourceBase {
		
		byte[] data = null;
		int dataCt = UNDEFINED;
		
		public StorageResource(String name, byte[] post, int ct) {
			super(name);
			
			this.data = post;
			this.dataCt = ct;
			
			getAttributes().addContentType(dataCt);
			getAttributes().setMaximumSizeEstimate(data.length);
		}
		
		@Override
		public void handleGET(CoapExchange exchange) {

			if (exchange.getRequestOptions().hasAccept() && exchange.getRequestOptions().getAccept() != dataCt) {
				exchange.respond(NOT_ACCEPTABLE, MediaTypeRegistry.toString(dataCt) + " only");
			} else {
				exchange.respond(CONTENT, data, dataCt);
			}
		}

		@Override
		public void handleDELETE(CoapExchange exchange) {
			this.delete();
		}
	}
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private synchronized String storeData(byte[] payload, int cf) {
		
		String name = new Integer(++counter).toString();

		// set payload and content type
		StorageResource sub = new StorageResource(name, payload, cf);
		
		add(sub);
		
		return sub.getURI();
	}
}

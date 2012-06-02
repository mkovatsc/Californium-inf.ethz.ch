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
package ch.ethz.inf.vs.californium.examples.resources;

import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;

/*
 * This class implements a 'storage' resource for demonstration purposes.
 * 
 * Defines a resource that stores POSTed data and that creates new
 * sub-resources on PUT request where the Uri-Path doesn't yet point to an
 * existing resource.
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class StorageResource extends LocalResource {

	// Constructors ////////////////////////////////////////////////////////////
	
	/*
	 * Default constructor.
	 */
	public StorageResource() {
		this("storage");
	}
	
	/*
	 * Constructs a new storage resource with the given resourceIdentifier.
	 */
	public StorageResource(String resourceIdentifier) {
		super(resourceIdentifier);
		setTitle("PUT your data here or POST new resources!");
		setResourceType("Storage");
		isObservable(true);
	}

	// REST Operations /////////////////////////////////////////////////////////
	
	/*
	 * GETs the content of this storage resource. 
	 * If the content-type of the request is set to application/link-format 
	 * or if the resource does not store any data, the contained sub-resources
	 * are returned in link format.
	 */
	@Override
	public void performGET(GETRequest request) {

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);

		// check if link format requested
		if (request.getContentType()==MediaTypeRegistry.APPLICATION_LINK_FORMAT || data == null) {

			// respond with list of sub-resources in link format
			response.setPayload(LinkFormat.serialize(this, request.getOptions(OptionNumberRegistry.URI_QUERY), true), MediaTypeRegistry.APPLICATION_LINK_FORMAT);

		} else {

			// load data into payload
			response.setPayload(data);

			// set content type
			if (getContentTypeCode().size()>0) {
				response.setContentType(getContentTypeCode().get(0));
			}
		}

		// complete the request
		request.respond(response);
	}
	
	/*
	 * PUTs content to this resource.
	 */
	@Override
	public void performPUT(PUTRequest request) {

		// store payload
		storeData(request);

		// complete the request
		request.respond(CodeRegistry.RESP_CHANGED);
	}

	/*
	 * POSTs a new sub-resource to this resource.
	 * The name of the new sub-resource is retrieved from the request
	 * payload.
	 */
	@Override
	public void performPOST(POSTRequest request) {

		// get request payload as a string
		String payload = request.getPayloadString();
		
		// check if valid Uri-Path specified
		if (payload != null && !payload.isEmpty()) {

			createSubResource(request, payload);

		} else {

			// complete the request
			request.respond(CodeRegistry.RESP_BAD_REQUEST,
				"Payload must contain Uri-Path for new sub-resource.");
		}
	}

	/*
	 * Creates a new sub-resource with the given identifier in this resource.
	 * Added checks for resource creation.
	 */
	@Override
	public void createSubResource(Request request, String newIdentifier) {
		
		if (request instanceof PUTRequest) {
			request.respond(CodeRegistry.RESP_FORBIDDEN, "PUT restricted to exiting resources");
			return;
		}
		
		// omit leading and trailing slashes
		if (newIdentifier.startsWith("/")) {
			newIdentifier = newIdentifier.substring(1);
		}
		if (newIdentifier.endsWith("/")) {
			newIdentifier = newIdentifier.substring(0, newIdentifier.length()-1);
		}
		
		// truncate from special chars onwards 
		if (newIdentifier.indexOf("/")!=-1) {
			newIdentifier = newIdentifier.substring(0,newIdentifier.indexOf("/"));
		}
		if (newIdentifier.indexOf("?")!=-1) {
			newIdentifier = newIdentifier.substring(0,newIdentifier.indexOf("?"));
		}
		if (newIdentifier.indexOf("\r")!=-1) {
			newIdentifier = newIdentifier.substring(0,newIdentifier.indexOf("\r"));
		}
		if (newIdentifier.indexOf("\n")!=-1) {
			newIdentifier = newIdentifier.substring(0,newIdentifier.indexOf("\n"));
		}
		
		// special restriction
		if (newIdentifier.length()>32) {
			request.respond(CodeRegistry.RESP_FORBIDDEN, "Resource segments limited to 32 chars");
			return;
		}
		
		// rt by query
		String newRtAttribute = null;
		for (Option query : request.getOptions(OptionNumberRegistry.URI_QUERY)) {
			String keyValue[] = query.getStringValue().split("=");
			
			if (keyValue[0].equals("rt") && keyValue.length==2) {
				newRtAttribute = keyValue[1];
				continue;
			}
		}

		// create new sub-resource
		if (getResource(newIdentifier)==null) {
			
			StorageResource resource = new StorageResource(newIdentifier);
			if (newRtAttribute!=null) {
				resource.setResourceType(newRtAttribute);
			}
			
			add(resource);
	
			// store payload
			resource.storeData(request);
	
			// create new response
			Response response = new Response(CodeRegistry.RESP_CREATED);
	
			// inform client about the location of the new resource
			response.setLocationPath(resource.getPath());
	
			// complete the request
			request.respond(response);
			
		} else {
			// defensive programming if someone incorrectly calls createSubResource()
			request.respond(CodeRegistry.RESP_INTERNAL_SERVER_ERROR, "Trying to create existing resource");
			Logger.getAnonymousLogger().severe(String.format("Cannot create sub resource: %s/[%s] already exists", this.getPath(), newIdentifier));
		}
	}
	
	/*
	 * DELETEs this storage resource, if it is not root.
	 */
	@Override
	public void performDELETE(DELETERequest request) {

		// disallow to remove the root "storage" resource
		if (parent instanceof StorageResource) {

			// remove this resource
			remove();

			request.respond(CodeRegistry.RESP_DELETED);
		} else {
			request.respond(CodeRegistry.RESP_FORBIDDEN,
				"Root storage resource cannot be deleted");
		}
	}

	// Internal ////////////////////////////////////////////////////////////////
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private void storeData(Request request) {

		// set payload and content type
		data = request.getPayload();
		clearAttribute(LinkFormat.CONTENT_TYPE);
		setContentTypeCode(request.getContentType());

		// signal that resource state changed
		changed();
	}

	private byte[] data; 
}

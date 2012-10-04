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
package ch.ethz.inf.vs.californium.endpoint.resource;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;

/**
 * This resource registers itself as an observer on the specified uri,
 * the resource is observable
 * It is used to handle more observers on limited devices
 * 
 * 
 * @author Nico Eigenmann
 */
public class ObservableResource extends LocalResource {

	// The current time represented as string
	private String host;
	private Response lastResponse;

	/*
	 * Constructor for a new ObservableResource
	 */
	public ObservableResource(String identifier, String uri) {
		super(identifier);
		isObservable(true);
		
		GETRequest obsRequest = new GETRequest();
		obsRequest.setURI(uri);
		obsRequest.setOption(new Option(1, OptionNumberRegistry.OBSERVE));
		obsRequest.registerResponseHandler(new observeReciever());
		try {
			obsRequest.execute();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	
	/*
	 * Defines a new timer task to return the current time
	 */
	private class observeReciever implements ResponseHandler {

		@Override
		public void handleResponse(Response response) {
			lastResponse = response;
			changed();
		}
	}

	
	
	
	@Override
	public void performDELETE(DELETERequest request){
		
		//Trying To unregister once (send Get Request without Observe Option)
		GETRequest unRequest = new GETRequest();
		unRequest.setURI("coap://"+host+"/"+getName());
		unRequest.setType(Message.messageType.NON);
		unRequest.enableResponseQueue(true);
		try {
			unRequest.execute();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		remove();
		
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	
	
	@Override
	public void performGET(GETRequest request) {

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		response.addOptions(lastResponse.getOptions());
		response.setContentType(lastResponse.getContentType());
		response.setPayload(lastResponse.getPayload());	

		// complete the request
		request.respond(response);
	}
}

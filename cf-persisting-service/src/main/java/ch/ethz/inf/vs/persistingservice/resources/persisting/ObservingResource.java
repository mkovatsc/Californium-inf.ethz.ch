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
package ch.ethz.inf.vs.persistingservice.resources.persisting;

import java.io.IOException;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.HistoryResource;

/**
 * The Class ObservingResource keeps track, whether the task observes the source or uses polling.
 */
public class ObservingResource extends LocalResource {
	
	/** Is the source observable */
	private boolean observable;
	
	/** The observing. */
	private boolean observing;
	
	/** The device. */
	private String device;
	
	/** The set. */
	private boolean set;
	
	private List<Option> getOptions;
	
	/** History Resource */
	private HistoryResource historyResource;
	
	/**
	 * Instantiates a new observing resource and checks whether the source is observable.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param observing the default for observing
	 * @param device the device
	 * @param options the options
	 */
	public ObservingResource(String resourceIdentifier, boolean observing, String device, List<Option> getOptions) {
		super(resourceIdentifier);
		this.observing = observing;
		this.device = device;
		this.set = false;
		
		// create a new request
		Request request = new GETRequest();
		// add the observe option
		request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
		if (getOptions != null)
			request.setOptions(getOptions);
		// add the check observable handler
		request.registerResponseHandler(new CheckObservableHandler());
		request.setURI(device);		
		
		try {
			request.prettyPrint();
			request.execute();
		} catch (IOException e) {
			System.err.println("Exception: " + e.getMessage());
		}
	}
	
	/**
	 * Checks if is observing.
	 *
	 * @return true, if is observing and not polling.
	 */
	public boolean isObserving() {
		return observing;
	}

	/**
	 * Checks if is sets the.
	 *
	 * @return true, if observing was set after the source is checked for the observer mechanism.
	 */
	public boolean isSet() {
		return set;
	}
	
	/**
	 * 
	 */
	public void setupReferences(HistoryResource historyResource) {
		this.historyResource = historyResource;
	}

	/**
	 * performGET responds with the observing status.
	 * 
	 * true:	observing
	 * false:	polling
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET OBSERVING: get observing for device " + device);
		request.prettyPrint();
		
		request.respond(CodeRegistry.RESP_CONTENT, Boolean.toString(observing));
	}
	
	/**
	 * performPUT change observing status
	 * 
	 * true:	observing
	 * false:	polling
	 */	
	public void performPUT(PUTRequest request) {
		System.out.println("PUT OBSERVING: change the observing state for device " + device);
		request.prettyPrint();
		
		boolean change;
		
		String payload = request.getPayloadString();
		if (payload.equals("true")) {
			change = true;
		} else if (payload.equals("false")) {
			change = false;
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Payload has to be 'true' or 'false'");
			return;
		}
				
		if (change != observing) {
			if (observable && change) {
				observing = true;
				historyResource.stopHistory(false, getOptions, false);
				historyResource.startHistory(change, getOptions);
				request.respond(CodeRegistry.RESP_CHANGED);
			} else if (!observable && change) {
				request.respond(CodeRegistry.RESP_CONTENT, "Source not observable");
			} else {
				observing = false;
				historyResource.stopHistory(true, getOptions, false);
				historyResource.startHistory(change, getOptions);
				request.respond(CodeRegistry.RESP_CHANGED);
			}
		} else {
			request.respond(CodeRegistry.RESP_CHANGED);
		}
	}
	
	/**
	 * The Class CheckObservableHandler handles the response trying to register
	 * as observer on the source.
	 */
	public class CheckObservableHandler implements ResponseHandler {

		/**
		 * The response comes from the get request to check, if the source
		 * device is observable.
		 * <p>
		 * If the response has the observable option, the resource is observable.<br>
		 *
		 * @param response the response
		 */
		@Override
		public void handleResponse(Response response) {
			System.out.println("OBSERVABLE CHECK: checking for observable on device " + device);
						
			if (response.hasOption(OptionNumberRegistry.OBSERVE)) {
				observable = true;
				observing = true;
			} else {
				observable = false;
				observing = false;
			}
			set = true;
			
			Request unregister = new GETRequest();
			unregister.setURI(device);
			
			try {
				unregister.execute();
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
}

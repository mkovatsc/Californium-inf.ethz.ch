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
package ch.ethz.inf.vs.persistingservice.resources.persisting.history.time;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;

/**
 * The Class NewestResource is observable and can be used to retrieve the newest document for a source device.
 */
public class NewestResource extends AbstractTimeResource {

	private String value;
	private String date;
	private String device;
	
	/**
	 * Instantiates a new newest resource and makes it observable.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 */
	public NewestResource(String resourceIdentifier, String device) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.device = device;
		
		value = "";
		date = "";
	}
	
	/**
	 * perform GET responds with the newest value stored in the database.
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET NEWEST: get request for device " + device);
		request.prettyPrint();
		
		acceptGetRequest(request, new NewestQuery());
	}
	
	/**
	 * Accept get request reacts to the get request.
	 * 
	 * @param request
	 * 			the request is the get request received.
	 * @param query
	 * 			the query is a container for a method, which defines the mechanism to retrieve data from the database. 
	 */
	public void acceptGetRequest(GETRequest request, AbstractQuery query) {
		List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
		OptionParser parsedOptions = new OptionParser(options);
		
		String ret = "";
		ret += query.perform(parsedOptions, AbstractQuery.NEWEST);
		
		request.respond(CodeRegistry.RESP_CONTENT, ret);
		
		System.out.println("GETRequst NEWEST: (value: " + ret + ") for device " + device);
	}
	
	/**
	 * The Class NewestQuery accesses the database and returns the retrieved data.
	 */
	private class NewestQuery extends AbstractQuery {
		
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				ret += value + ";" + date;
			} else {
				ret += value;
			}
			return ret;
		}
	}
	
	/**
	 * Notify changed and pass the notification to the subresources.
	 *
	 * @param value the newest value
	 * @param date the current date
	 */
	public void notifyChanged(String value, String date) {
		if (!this.value.equals(value)) {
			this.value = value;
			this.date = date;
			changed();
		}
	}
	
}

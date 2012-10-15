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
package ch.ethz.inf.vs.persistingservice.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;
import ch.ethz.inf.vs.persistingservice.parser.PayloadParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.PersistingResource;

/**
 * The Class TasksResource implements the task resource. The task resource is used to add new tasks to the persisting service.
 */
public class TasksResource extends LocalResource {

	/** The GENERAL_TOP_RES is the name of the general top resource. */
	private String GENERAL_TOP_RES = "general";
	
	/** The top resources. */
	Map<String, Resource> topResources = new HashMap<String, Resource>();
	
	/**
	 * Instantiates a new tasks resource and adds it to the resource tree. The general top resource is added to the resource tree.
	 *
	 * @param resourceIdentifier the resource identifier
	 */
	public TasksResource(String resourceIdentifier) {
		super(resourceIdentifier);

		TopResource topRes = new TopResource(GENERAL_TOP_RES);
		add(topRes);
		topResources.put(GENERAL_TOP_RES, topRes);
	}
	
	/**
	 * Clean up removes empty top resources, when persisting resources are deleted.
	 *
	 * @param resName the res name of the top resource
	 */
	public void cleanUp(String resName) {
		if (resName.equals(GENERAL_TOP_RES)) return; // don't remove the general top resource
		if (topResources.containsKey(resName)) {
			Resource topRes = topResources.get(resName);
			if (topRes.subResourceCount() == 0) {
				topResources.remove(resName);
				topRes.remove();
			}
		}
	}
	
	// Requests //////////////////////////////////////////////////////
	/**
	 * performGET responds with a list of all top resources.
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET TASKS: get all the top resources");
		request.prettyPrint();
		
		Set<Resource> subResources = getSubResources();
		String ret = "";
		for (Resource res : subResources) {
			ret += res.getName() + "\n"; 
		}
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
	
	/**
	 * performPOST creates a new persisting resource. If necessary, a new top resource is added as well. 
	 */
	public void performPOST(POSTRequest request) {
		System.out.println("NEW PERSISTING RESOURCE: create a new persisting resource.");
		request.prettyPrint();
		
		String payload = request.getPayloadString();
		
		// parse the payload
		PayloadParser parsedPayload = new PayloadParser(payload);
		// check payload
		if (parsedPayload.containsLabels("resid", "deviceroot", "deviceres", "type", "topid")) {
			String topid = parsedPayload.getStringValue("topid");
			String resid = parsedPayload.getStringValue("resid");
			String type = parsedPayload.getStringValue("type");
			String deviceroot = parsedPayload.getStringValue("deviceroot");
			String deviceres = parsedPayload.getStringValue("deviceres");
			
			List<Option> options = null;
			// check for options
			if (parsedPayload.containsLabel("options")) {
				options = new ArrayList<Option>();
				String[] opts = (parsedPayload.getStringValue("options")).split("&");
				for (String opt : opts) {
					options.add(new Option(opt, OptionNumberRegistry.URI_QUERY));
				}
			}
		
			Resource topRes = null;
			// add top resource if necessary
			if (topResources.containsKey(topid)) {
				topRes = topResources.get(topid);
			} else {
				topRes = new TopResource(topid);
				add(topRes);
				topResources.put(topid, topRes);
			}
			
			if (!alreadyExisting(topRes, resid)) {
				// add new persisting resource
				topRes.add(new PersistingResource(resid, type, deviceroot, deviceres, options, this, topid));
				request.respond(CodeRegistry.RESP_CREATED);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Topid and resid are already existing. Choose different resid.");
			}
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide: \n" +
														   "topid = ...\n" +
														   "resid = ...\n" +
														   "deviceroot = ...\n" +
														   "deviceres = ...\n" +
														   "(options = ...)\n" +
														   "type = number | string");
		}
	}
	
	private boolean alreadyExisting(Resource topRes, String resid) {
		for (Resource res : topRes.getSubResources()) {
			if (res.getName().equals(resid)) return true;
		}
		
		return false;
	}
	
}

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
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.endpoint;

import java.util.HashMap;
import java.util.Map;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;



/*
 * This class describes the functionality of a local CoAP resource.
 * 
 * A client of the Californium framework will override this class 
 * in order to provide custom resources.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class LocalResource extends Resource {

	// Constructors ////////////////////////////////////////////////////////////

	public LocalResource(String resourceIdentifier, boolean hidden) {
		super(resourceIdentifier, hidden);
	}

	public LocalResource(String resourceIdentifier) {
		super(resourceIdentifier, false);
	}

	// Observing ///////////////////////////////////////////////////////////////

	public void addObserveRequest(GETRequest request) {

		if (request != null) {

			// lazy creation
			if (observeRequests == null) {
				observeRequests = new HashMap<String, GETRequest>();
			}

			observeRequests.put(request.endpointID(), request);

			System.out
					.printf("Observation relationship between %s and %s established.\n",
							request.endpointID(), getResourceIdentifier());

		}
	}

	public void removeObserveRequest(String endpointID) {

		if (observeRequests != null) {
			if (observeRequests.remove(endpointID) != null) {
				System.out
						.printf("Observation relationship between %s and %s terminated.\n",
								endpointID, getResourceIdentifier());
			}
		}
	}

	public boolean isObserved(String endpointID) {
		return observeRequests != null
				&& observeRequests.containsKey(endpointID);
	}

	protected void processObserveRequests() {
		if (observeRequests != null) {
			for (GETRequest request : observeRequests.values()) {
				performGET(request);
			}
		}
	}

	protected void changed() {
		processObserveRequests();
	}

	// REST Operations /////////////////////////////////////////////////////////

	@Override
	public void performGET(GETRequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}

	@Override
	public void performPUT(PUTRequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}

	@Override
	public void performPOST(POSTRequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}

	@Override
	public void performDELETE(DELETERequest request) {
		request.respond(CodeRegistry.RESP_METHOD_NOT_ALLOWED);
	}

	// Sub-resource management /////////////////////////////////////////////////

	/*
	 * Generally forbid the creation of new sub-resources.
	 * Override and define checks to allow creation.
	 */
	@Override
	public void createSubResource(Request request, String newIdentifier) {
		request.respond(CodeRegistry.RESP_FORBIDDEN);
	}

	private Map<String, GETRequest> observeRequests;

}

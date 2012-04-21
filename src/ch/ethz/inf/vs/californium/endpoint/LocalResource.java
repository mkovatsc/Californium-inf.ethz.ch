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
package ch.ethz.inf.vs.californium.endpoint;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.ObservingManager;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;

/**
 * The class LocalResource provides the functionality of a CoAP server resource
 * as a subclass of {@link Resource}. Implementations will inherit this class in order
 * to provide custom resources by overriding some the following methods:
 * <ul>
 * <li>{@link #performGET(GETRequest)}
 * <li>{@link #performPOST(POSTRequest)}
 * <li>{@link #performPUT(PUTRequest)}
 * <li>{@link #performDELETE(DELETERequest)}
 * </ul>
 * These methods are defined by the {@link ch.ethz.inf.vs.californium.coap.RequestHandler} interface and have a default
 * implementation in this class that responds with "4.05 Method Not Allowed."
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class LocalResource extends Resource {

	// Constructors ////////////////////////////////////////////////////////////

	public LocalResource(String resourceIdentifier, boolean hidden) {
		super(resourceIdentifier, hidden);
	}

	public LocalResource(String resourceIdentifier) {
		super(resourceIdentifier, false);
	}

// Observing ///////////////////////////////////////////////////////////////////

	/**
	 * Calling this method will notify all registered observers. Resources that
	 * use this method must also call {@link #isObservable(true)} so that
	 * clients will be registered after a successful GET with Observe option.
	 */
	protected void changed() {
		ObservingManager.getInstance().notifyObservers(this);
	}

// REST Operations /////////////////////////////////////////////////////////////

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
		request.sendResponse();
	}

}

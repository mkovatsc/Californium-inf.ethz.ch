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

import java.net.SocketException;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Communicator;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class LocalEndpoint provides the functionality of a server endpoint
 * as a subclass of {@link Endpoint}. A server implementation using Cf will
 * override this class to provide custom resources. Internally, the main
 * purpose of this class is to forward received requests to the corresponding
 * resource specified by the Uri-Path option. Furthermore, it implements the
 * root resource to return a brief server description to GET requests with
 * empty Uri-Path.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class LocalEndpoint extends Endpoint {
	
	public static final String ENDPOINT_INFO = 
		"************************************************************\n" +
		"This CoAP endpoint is using the Californium (Cf) framework\n" +
		"developed by Dominique Im Obersteg, Daniel Pauli, and\n" +
		"Matthias Kovatsch.\n" +
		"Cf is available under BSD 3-clause license on GitHub:\n" +
		"https://github.com/mkovatsc/Californium\n" +
		"\n" +
		"(c) 2012, Institute for Pervasive Computing, ETH Zurich\n" +
		"Contact: Matthias Kovatsch <kovatsch@inf.ethz.ch>\n" +
		"************************************************************";

	private class RootResource extends LocalResource {

		public RootResource() {
			super("");
		}

		@Override
		public void performGET(GETRequest request) {

			// create response
			Response response = new Response(CodeRegistry.RESP_CONTENT);

			response.setPayload(ENDPOINT_INFO);

			// complete the request
			request.respond(response);
		}
	}

	// TODO Constructor with custom root resource; check for resourceIdentifier==""
	
	public LocalEndpoint(int port, int defaultBlockSize) throws SocketException {

		// initialize communicator
		this.communicator = new Communicator(port, false, defaultBlockSize);
		this.communicator.registerReceiver(this);

		// initialize resources
		this.rootResource = new RootResource();

		this.wellKnownResource = new LocalResource(".well-known", true);
		this.wellKnownResource.setResourceType("");

		this.discoveryResource = new DiscoveryResource(rootResource);

		rootResource.addSubResource(wellKnownResource);
		wellKnownResource.addSubResource(discoveryResource);

	}

	public LocalEndpoint(int port) throws SocketException {
		this(port, Properties.std.getInt("DEFAULT_BLOCK_SIZE"));
	}
	
	public LocalEndpoint() throws SocketException {
		this(Properties.std.getInt("DEFAULT_PORT"));
	}

	@Override
	public void execute(Request request) {

		// check if request exists
		if (request != null) {

			// retrieve resource identifier
			String resourceIdentifier = request.getUriPath();

			// lookup resource
			LocalResource resource = getResource(resourceIdentifier);

			// check if resource available
			if (resource != null) {

				// invoke request handler of the resource
				request.dispatch(resource);

				// check if resource is to be observed
				// TODO: Create ObservingManager
				if (request.getCode()==CodeRegistry.METHOD_GET) {
					if (request.hasOption(OptionNumberRegistry.OBSERVE)) {
	
						// establish new observation relationship
						resource.addObserveRequest((GETRequest) request);

					} else if (resource.isObserved(request.getPeerAddress().toString())) {

						// terminate observation relationship on that resource
						resource.removeObserveRequest(request.getPeerAddress().toString());
					}
				}
			
			} else if (request instanceof PUTRequest) {
				// allows creation of non-existing resources through PUT
				this.createByPUT((PUTRequest) request);
				
			} else {
				// resource does not exist
				System.out.printf("[%s] Resource not found: '%s'\n", getClass().getName(), resourceIdentifier);

				request.respond(CodeRegistry.RESP_NOT_FOUND);
			}
		}
	}

	// delegate to createNew() of top resource
	private void createByPUT(PUTRequest request) {

		String identifier = request.getUriPath(); // always starts with "/"
		
		// find existing parent up the path
		String parentIdentifier = new String(identifier);
		String newIdentifier = "";
		Resource parent = null;
		// will end at rootResource ("")
		do {
			newIdentifier = identifier.substring(parentIdentifier.lastIndexOf('/')+1);
			parentIdentifier = parentIdentifier.substring(0, parentIdentifier.lastIndexOf('/'));
			System.out.println(parentIdentifier);
			System.out.println(newIdentifier);
		} while ((parent = getResource(parentIdentifier))==null);

		parent.createSubResource(request, newIdentifier);
	}

	public void addResource(LocalResource resource) {
		if (rootResource != null) {
			rootResource.addSubResource(resource);
		}
	}

	public void removeResource(String resourceIdentifier) {
		if (rootResource != null) {
			rootResource.removeSubResource(resourceIdentifier);
		}
	}

	public LocalResource getResource(String resourceIdentifier) {
		if (rootResource != null) {
			return (LocalResource) rootResource.getResource(resourceIdentifier);
		} else {
			return null;
		}
	}

	@Override
	public void handleRequest(Request request) {
		execute(request);
	}

	@Override
	public void handleResponse(Response response) {
		// response.handle();
	}

	private Resource wellKnownResource;
	private DiscoveryResource discoveryResource;
}

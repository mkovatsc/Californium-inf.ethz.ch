/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
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

import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.DiscoveryResource;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The class LocalEndpoint provides the functionality of a server endpoint as a
 * subclass of {@link Endpoint}. A server implementation using Cf will override
 * this class to provide custom resources. Internally, the main purpose of this
 * class is to forward received requests to the corresponding resource specified
 * by the Uri-Path option. Furthermore, it implements the root resource to
 * return a brief server description to GET requests with empty Uri-Path.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, Matthias Kovatsch and Francesco
 *         Corazza
 */
public abstract class LocalEndpoint extends Endpoint {

	public static final String ENDPOINT_INFO = "************************************************************\n"
								             + "I-D: draft-ietf-core-coap-13\n"
								    		 + "************************************************************\n"
	                                         + "This server is using the Californium (Cf) CoAP framework\n"
	                                         + "developed by Daniel Pauli, Dominique Im Obersteg,\n"
	                                         + "Stefan Jucker, Francesco Corazza, and Matthias Kovatsch.\n"
	                                         + "Cf is available under BSD 3-clause license on GitHub:\n"
	                                         + "https://github.com/mkovatsc/Californium\n"
	                                         + "\n"
	                                         + "(c) 2013, Institute for Pervasive Computing, ETH Zurich\n"
	                                         + "Contact: Matthias Kovatsch <kovatsch@inf.ethz.ch>\n"
	                                         + "************************************************************";
	private static final int THREAD_POOL_SIZE = Properties.std.getInt("THREAD_POOL_SIZE");;
	private final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

	public LocalEndpoint() throws SocketException {

		// add root resource
		rootResource = new RootResource();
		addResource(new DiscoveryResource(rootResource));
	}

	/**
	 * Adds a resource to the root resource of the endpoint. If the resource
	 * identifier is actually a path, it is split up into multiple resources.
	 * 
	 * @param resource
	 *            - the resource to add to the root resource
	 */
	public void addResource(LocalResource resource) {
		if (rootResource != null) {
			rootResource.add(resource);
		}
	}

	@Override
	public void execute(final Request request) {

		// check if request exists
		if (request != null) {

			// retrieve resource identifier
			String resourcePath = request.getUriPath();

			// lookup resource
			final LocalResource resource = getResource(resourcePath);

			// check if resource available
			if (resource != null) {

				request.setResource(resource);

				LOG.info(String.format("Dispatching execution: %s", resourcePath));

				threadPool.submit(new Runnable() {

					@Override
					public void run() {
						// invoke request handler of the resource
						try {
							request.dispatch(resource);
						} catch (Exception e) {
							LOG.severe(String.format("Resource handler %s crashed: %s", resource.getClass().getSimpleName(), e.getMessage()));
							request.respondAndSend(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
							return;
						}

						// check if resource did generate a response
						if (request.getResponse() != null) {

							// handle the production of the response by the
							// resource
							responseProduced(request.getResponse());
						}
						
						// send response from this thread
						request.sendResponse();
					}
				});

			} else if (request instanceof PUTRequest) {
				// allows creation of non-existing resources through PUT
				createByPUT((PUTRequest) request);
				request.sendResponse();

			} else {
				// resource does not exist
				LOG.warning(String.format("Cannot find resource: %s", resourcePath));

				request.respondAndSend(CodeRegistry.RESP_NOT_FOUND);
			}
			
		}
	}

	/**
	 * Gets the resource.
	 * 
	 * @param resourcePath
	 *            the resource path
	 * @return the resource
	 */
	public LocalResource getResource(String resourcePath) {
		if (rootResource != null) {
			return (LocalResource) rootResource.getResource(resourcePath);
		} else {
			return null;
		}
	}

	/**
	 * Provides access to the root resource that contains all local resources,
	 * e.g., for the surrounding server code to register at an RD.
	 * 
	 * @return the root resource
	 */
	public Resource getRootResource() {
		return rootResource;
	}

	@Override
	public void handleRequest(Request request) {
		if (request.isProxyUriSet()) {
			request.respondAndSend(CodeRegistry.RESP_PROXYING_NOT_SUPPORTED);
		} else {
			execute(request);
		}
	}

	@Override
	public void handleResponse(Response response) {
		// response.handle();
	}

	/**
	 * Removes the resource.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 */
	public void removeResource(String resourceIdentifier) {
		if (rootResource != null) {
			rootResource.removeSubResource(resourceIdentifier);
		}
	}

	/**
	 * 
	 */
	public void start() {
		createCommunicator();
	}

	/**
	 * Delegates a {@link PUTRequest} for a non-existing resource to the.
	 * 
	 * @param request
	 *            - the PUT request
	 *            {@link LocalResource#createSubResource(Request, String)}
	 *            method of the first existing resource up the path.
	 */
	protected void createByPUT(PUTRequest request) {

		String path = request.getUriPath(); // always starts with "/"

		// find existing parent up the path
		String parentIdentifier = new String(path);
		String newIdentifier = "";
		Resource parent = null;
		// will end at rootResource ("")
		do {
			newIdentifier = path.substring(parentIdentifier.lastIndexOf('/') + 1);
			parentIdentifier = parentIdentifier.substring(0, parentIdentifier.lastIndexOf('/'));
		} while ((parent = getResource(parentIdentifier)) == null);

		parent.createSubResource(request, newIdentifier);
	}

	/**
	 * 
	 */
	protected abstract void createCommunicator();

	/**
	 * Method to notify the implementers of this class that a new response has
	 * been received from a resource.
	 * 
	 * @param response
	 */
	protected abstract void responseProduced(Response response);

	/**
	 * The Class RootResource.
	 */
	private static class RootResource extends LocalResource {

		/**
		 * Instantiates a new root resource.
		 */
		public RootResource() {
			super("", true);
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * ch.ethz.inf.vs.californium.endpoint.LocalResource#performGET(ch.ethz
		 * .inf.vs.californium.coap.GETRequest)
		 */
		@Override
		public void performGET(GETRequest request) {
			
			request.respond(CodeRegistry.RESP_CONTENT, ENDPOINT_INFO, MediaTypeRegistry.TEXT_PLAIN);
		}
	}
}

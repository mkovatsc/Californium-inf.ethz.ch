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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;


public class RemoteEndpoint extends Endpoint {

	public static Endpoint fromURI(String uri) {
		try {
			return new RemoteEndpoint(new URI(uri));
		} catch (URISyntaxException e) {
			System.out.printf(
					"[%s] Failed to create RemoteEndpoint from URI: %s\n",
					"JCoAP", e.getMessage());
			return null;
		}
	}

	public RemoteEndpoint(URI uri) {

		this.communicator = Request.defaultCommunicator();
		this.communicator.registerReceiver(this);

		this.uri = uri;
	}

	@Override
	public void execute(Request request) throws IOException {

		if (request != null) {

			// set authority specific part of the request's URI

			String scheme = uri.getScheme();
			String authority = uri.getAuthority();
			String path = request.getURI() != null ? request.getURI().getPath()
					: uri.getPath();
			String query = request.getURI() != null ? request.getURI()
					.getQuery() : uri.getQuery();
			String fragment = request.getURI() != null ? request.getURI()
					.getFragment() : uri.getFragment();

			try {

				request.setURI(new URI(scheme, authority, path, query, fragment));

			} catch (URISyntaxException e) {

				System.out.printf("[%s] Failed to assign URI to request: %s\n",
						getClass().getName(), e.getMessage());
			}

			// execute the request
			request.execute();
		}

	}

	protected URI uri;

	@Override
	public void handleRequest(Request request) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleResponse(Response response) {
		// response.handle();
	}
}

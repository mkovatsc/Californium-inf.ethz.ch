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
package ch.ethz.inf.vs.californium.examples;

import java.net.SocketException;

import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.endpoint.Endpoint;
import ch.ethz.inf.vs.californium.endpoint.LocalEndpoint;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.californium.examples.resources.CarelessResource;
import ch.ethz.inf.vs.californium.examples.resources.HelloWorldResource;
import ch.ethz.inf.vs.californium.examples.resources.ImageResource;
import ch.ethz.inf.vs.californium.examples.resources.LargeResource;
import ch.ethz.inf.vs.californium.examples.resources.SeparateResource;
import ch.ethz.inf.vs.californium.examples.resources.StorageResource;
import ch.ethz.inf.vs.californium.examples.resources.TimeResource;
import ch.ethz.inf.vs.californium.examples.resources.ToUpperResource;
import ch.ethz.inf.vs.californium.examples.resources.ZurichWeatherResource;
import ch.ethz.inf.vs.californium.util.Log;

/**
 * The class ExampleServer shows how to implement a server by extending 
 * {@link LocalEndpoint}. In the implementation class, use
 * {@link LocalEndpoint#addResource(ch.ethz.inf.vs.californium.endpoint.LocalResource)}
 * to add custom resources extending {@link LocalResource}.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class ExampleServer extends LocalEndpoint {

	// exit codes for runtime errors
	public static final int ERR_INIT_FAILED = 1;
	
	/**
	 * Constructor for a new ExampleServer. Call {@code super(...)} to configure
	 * the port, etc. according to the {@link LocalEndpoint} constructors.
	 * <p>
	 * Add all initial {@link LocalResource}s here.
	 */
	public ExampleServer() throws SocketException {
		
		// add resources to the server
		addResource(new HelloWorldResource());
		addResource(new ToUpperResource());
		addResource(new StorageResource());
		addResource(new SeparateResource());
		addResource(new LargeResource());
		addResource(new TimeResource());
		addResource(new ZurichWeatherResource());
		addResource(new ImageResource());
		addResource(new CarelessResource());
	}

	// Logging /////////////////////////////////////////////////////////////////
	
	@Override
	public void handleRequest(Request request) {
		
		// Add additional handling like special logging here.
		request.prettyPrint();
		
		// dispatch to requested resource
		super.handleRequest(request);
	}

	
	// Application entry point /////////////////////////////////////////////////
	
	public static void main(String[] args) {
		
		Log.init();
		
		// create server
		try {
			
			Endpoint server = new ExampleServer();
			
			
			System.out.printf("ExampleServer listening on port %d.\n", server.port());
			
		} catch (SocketException e) {

			System.err.printf("Failed to create SampleServer: %s\n", e.getMessage());
			System.exit(ERR_INIT_FAILED);
		}
		
	}

}

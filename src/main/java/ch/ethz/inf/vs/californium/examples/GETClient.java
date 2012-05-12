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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;


public class GETClient {

	/*
	 * Application entry point.
	 * 
	 */	
	public static void main(String args[]) {
		
		URI uri = null; // URI parameter of the request
		
		if (args.length > 0) {
			
			// input URI from command line arguments
			try {
				uri = new URI(args[0]);
			} catch (URISyntaxException e) {
				System.err.println("Invalid URI: " + e.getMessage());
				System.exit(-1);
			}
		
			// create new request
			Request request = new GETRequest();
			// specify URI of target endpoint
			request.setURI(uri);
			// enable response queue for blocking I/O
			request.enableResponseQueue(true);
			
			// execute the request
			try {
				request.execute();
			} catch (IOException e) {
				System.err.println("Failed to execute request: " + e.getMessage());
				System.exit(-1);
			}
			
			// receive response
			try {
				Response response = request.receiveResponse();
				
				if (response != null) {
					// response received, output a pretty-print
					response.prettyPrint();
				} else {
					System.out.println("No response received.");
				}
				
			} catch (InterruptedException e) {
				System.err.println("Receiving of response interrupted: " + e.getMessage());
				System.exit(-1);
			}
			
		} else {
			// display help
			System.out.println("Californium (Cf) GET Client");
			System.out.println("(c) 2012, Institute for Pervasive Computing, ETH Zurich");
			System.out.println();
			System.out.println("Usage: " + GETClient.class.getSimpleName() + " URI");
			System.out.println("  URI: The CoAP URI of the remote resource to GET");
		}
	}

}

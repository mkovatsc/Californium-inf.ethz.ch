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
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

import ch.ethz.inf.vs.californium.CaliforniumLogger;
import ch.ethz.inf.vs.californium.Utils;
import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.EndpointManager.ClientMessageDeliverer;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.scandium.DTLSConnector;
import ch.ethz.inf.vs.scandium.ScandiumLogger;

/**
 * This class implements a simple CoAP client for testing purposes. Usage:
 * <p>
 * {@code java -jar SampleClient.jar [-l] METHOD URI [PAYLOAD]}
 * <ul>
 * <li>METHOD: {GET, POST, PUT, DELETE, DISCOVER, OBSERVE}
 * <li>URI: The URI to the remote endpoint or resource}
 * <li>PAYLOAD: The data to send with the request}
 * </ul>
 * Options:
 * <ul>
 * <li>-l: Loop for multiple responses}
 * </ul>
 * Examples:
 * <ul>
 * <li>{@code SampleClient DISCOVER coap://localhost}
 * <li>{@code SampleClient POST coap://someServer.org:5683 my data}
 * </ul>
 *  
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class ExampleClient {

	static {
		CaliforniumLogger.initialize();
		CaliforniumLogger.setLevel(Level.WARNING);
		
		ScandiumLogger.initialize();
		ScandiumLogger.setLevel(Level.FINER);
	}
	
	// resource URI path used for discovery
	private static final String DISCOVERY_RESOURCE = "/.well-known/core";

	// indices of command line parameters
	private static final int IDX_METHOD          = 0;
	private static final int IDX_URI             = 1;
	private static final int IDX_PAYLOAD         = 2;

	// exit codes for runtime errors
	private static final int ERR_MISSING_METHOD  = 1;
	private static final int ERR_UNKNOWN_METHOD  = 2;
	private static final int ERR_MISSING_URI     = 3;
	private static final int ERR_BAD_URI         = 4;
	private static final int ERR_REQUEST_FAILED  = 5;
	private static final int ERR_RESPONSE_FAILED = 6;


	// initialize parameters
	static String method = null;
	static URI uri = null;
	static String payload = "";
	static boolean loop = false;

	// for coaps
	private static Endpoint dtlsEndpoint;
	
	/*
	 * Main method of this client.
	 */
	public static void main(String[] args) throws IOException {
		
		// display help if no parameters specified
		if (args.length == 0) {
			printInfo();
			return;
		}
		
		// input parameters
		int idx = 0;
		for (String arg : args) {
			if (arg.startsWith("-")) {
				if (arg.equals("-l")) {
					loop = true;
				} else {
					System.out.println("Unrecognized option: " + arg);
				}
			} else {
				switch (idx) {
				case IDX_METHOD:
					method = arg.toUpperCase();
					break;
				case IDX_URI:
					try {
						uri = new URI(arg);
					} catch (URISyntaxException e) {
						System.err.println("Failed to parse URI: " + e.getMessage());
						System.exit(ERR_BAD_URI);
					}
					break;
				case IDX_PAYLOAD:
					payload = arg;
					break;
				default:
					System.out.println("Unexpected argument: " + arg);
				}
				++idx;
			}
		}

		// check if mandatory parameters specified
		if (method == null) {
			System.err.println("Method not specified");
			System.exit(ERR_MISSING_METHOD);
		}
		if (uri == null) {
			System.err.println("URI not specified");
			System.exit(ERR_MISSING_URI);
		}
		
		// create request according to specified method
		Request request = newRequest(method);

		// set request URI
		if (method.equals("DISCOVER") && (uri.getPath() == null || uri.getPath().isEmpty() || uri.getPath().equals("/"))) {
			// add discovery resource path to URI
			try {
				uri = new URI(uri.getScheme(), uri.getAuthority(), DISCOVERY_RESOURCE, uri.getQuery());
				
			} catch (URISyntaxException e) {
				System.err.println("Failed to parse URI: " + e.getMessage());
				System.exit(ERR_BAD_URI);
			}
		}
		
		request.setURI(uri);
		request.setPayload(payload);
		request.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
		
		if (request.getScheme().equals(CoAP.COAP_SECURE_URI_SCHEME)) {
			dtlsEndpoint = new CoAPEndpoint(new DTLSConnector(new InetSocketAddress(0)), NetworkConfig.getStandard());
			dtlsEndpoint.setMessageDeliverer(new ClientMessageDeliverer());
			dtlsEndpoint.start();
			EndpointManager.getEndpointManager().setDefaultSecureEndpoint(dtlsEndpoint);
		}
		
		// execute request
		try {
			request.send();

			// loop for receiving multiple responses
			do {
	
				// receive response
				Response response = null;
				try {
					response = request.waitForResponse();
				} catch (InterruptedException e) {
					System.err.println("Failed to receive response: " + e.getMessage());
					System.exit(ERR_RESPONSE_FAILED);
				}
	
				// output response
	
				if (response != null) {
	
					System.out.println(Utils.prettyPrint(response));
					System.out.println("Time elapsed (ms): " + response.getRTT());
	
					// check of response contains resources
					if (response.getOptions().hasContentFormat(MediaTypeRegistry.APPLICATION_LINK_FORMAT)) {
	
						String linkFormat = response.getPayloadString();
	
						// output discovered resources
						System.out.println("\nDiscovered resources:");
						System.out.println(linkFormat);
	
					} else {
	
						// check if link format was expected by client
						if (method.equals("DISCOVER")) {
							System.out.println("Server error: Link format not specified");
						}
					}
	
				} else {
	
					// no response received	
					System.err.println("Request timed out");
					break;
				}
	
			} while (loop);
		
		if (dtlsEndpoint!=null) {
			dtlsEndpoint.stop();
		}
			
		} catch (Exception e) {
			System.err.println("Failed to execute request: " + e.getMessage());
			System.exit(ERR_REQUEST_FAILED);
		}
	}

	/*
	 * Outputs user guide of this program.
	 */
	public static void printInfo() {
		System.out.println("Californium (Cf) Example Client");
		System.out.println("(c) 2014, Institute for Pervasive Computing, ETH Zurich");
		System.out.println();
		System.out.println("Usage: " + ExampleClient.class.getSimpleName() + " [-l] METHOD URI [PAYLOAD]");
		System.out.println("  METHOD  : {GET, POST, PUT, DELETE, DISCOVER, OBSERVE}");
		System.out.println("  URI     : The CoAP URI of the remote endpoint or resource");
		System.out.println("  PAYLOAD : The data to send with the request");
		System.out.println("Options:");
		System.out.println("  -l      : Loop for multiple responses");
		System.out.println("           (automatic for OBSERVE and separate responses)");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  ExampleClient DISCOVER coap://localhost");
		System.out.println("  ExampleClient POST coap://vs0.inf.ethz.ch:5683/storage my data");
	}

	/*
	 * Instantiates a new request based on a string describing a method.
	 * 
	 * @return A new request object, or null if method not recognized
	 */
	private static Request newRequest(String method) {
		if (method.equals("GET")) {
			return Request.newGet();
		} else if (method.equals("POST")) {
			return Request.newPost();
		} else if (method.equals("PUT")) {
			return Request.newPut();
		} else if (method.equals("DELETE")) {
			return Request.newDelete();
		} else if (method.equals("DISCOVER")) {
			return Request.newGet();
		} else if (method.equals("OBSERVE")) {
			Request request = Request.newGet();
			request.setObserve();
			loop = true;
			return request;
		} else {
			System.err.println("Unknown method: " + method);
			System.exit(ERR_UNKNOWN_METHOD);
			return null;
		}
	}

}

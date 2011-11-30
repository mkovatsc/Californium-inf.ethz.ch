package ch.ethz.inf.vs.californium.examples;

/*
 * This class implements a simple CoAP client for testing purposes.
 * 
 * Usage: java -jar SampleClient.jar [-l] METHOD URI [PAYLOAD]
 *   METHOD  : {GET, POST, PUT, DELETE, DISCOVER}
 *   URI     : The URI to the remote endpoint or resource
 *   PAYLOAD : The data to send with the request
 * Options:
 *   -l      : Wait for multiple responses
 * 
 * Examples:
 *   SampleClient DISCOVER coap://localhost
 *   SampleClient POST coap://someServer.org:61616 my data
 * 
 *   
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.coap.*;
import ch.ethz.inf.vs.californium.endpoint.RemoteResource;
import ch.ethz.inf.vs.californium.endpoint.Resource;


public class ExampleClient {

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
	private static final int ERR_BAD_LINK_FORMAT = 7;

	/*
	 * Main method of this client.
	 */
	public static void main(String[] args) {

		// initialize parameters
		String method = null;
		URI uri = null;
		String payload = null;
		boolean loop = false;

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
		if (request == null) {
			System.err.println("Unknown method: " + method);
			System.exit(ERR_UNKNOWN_METHOD);
		}

		if (method.equals("OBSERVE")) {
			request.setOption(new Option(60, OptionNumberRegistry.OBSERVE));
			loop = true;
		}

		// set request URI
		if (
			method.equals("DISCOVER") && 
			(uri.getPath() == null || uri.getPath().isEmpty())
		) {
			// add discovery resource path to URI
			try {
				uri = new URI(uri.getScheme(), uri.getAuthority(), DISCOVERY_RESOURCE,
					uri.getQuery(), uri.getFragment());
				
			} catch (URISyntaxException e) {
				System.err.println("Failed to parse URI: " + e.getMessage());
				System.exit(ERR_BAD_URI);
			}
			
		}
		request.setURI(uri);


		// set request payload
		request.setPayload(payload);
		
		// fix token
		request.setOption(new Option(0xCAFE, OptionNumberRegistry.TOKEN));

		// enable response queue in order to use blocking I/O
		request.enableResponseQueue(true);

		// execute request
		try {
			request.execute();
		
		} catch (IOException e) {
			System.err.println("Failed to execute request: " + e.getMessage());
			e.printStackTrace();
			System.exit(ERR_REQUEST_FAILED);
		}

		// loop for receiving multiple responses
		do {

			// receive response

			System.out.println("Receiving response...");
			Response response = null;
			try {
				response = request.receiveResponse();

				// check for indirect response
				if (response != null && response.isEmptyACK()) {
					response.log();
					System.out
							.println("Request acknowledged, waiting for separate response...");

					response = request.receiveResponse();
				}

			} catch (InterruptedException e) {
				System.err.println("Failed to receive response: "
						+ e.getMessage());
				System.exit(ERR_RESPONSE_FAILED);
			}

			// output response

			if (response != null) {

				response.log();
				System.out
						.println("Round Trip Time (ms): " + response.getRTT());

				// check of response contains resources
				if (response.hasFormat(MediaTypeRegistry.APPLICATION_LINK_FORMAT)) {

					String linkFormat = response.getPayloadString();

					// create resource three from link format
					Resource root = RemoteResource.newRoot(linkFormat);
					if (root != null) {

						// output discovered resources
						System.out.println("\nDiscovered resources:");
						root.log();

					} else {
						System.err.println("Failed to parse link format");
						System.exit(ERR_BAD_LINK_FORMAT);
					}
				} else {

					// check if link format was expected by client
					if (method.equals("DISCOVER")) {
						System.out
								.println("Server error: Link format not specified");
					}
				}

			} else {

				// no response received
				// calculate time elapsed
				long elapsed = System.currentTimeMillis()
						- request.getTimestamp();

				System.out.println("Request timed out (ms): " + elapsed);
				break;
			}

		} while (loop);

		// finish
		System.out.println();
	}

	/*
	 * Outputs user guide of this program.
	 */
	public static void printInfo() {
		System.out.println("Californium Java CoAP Sample Client");
		System.out.println();
		System.out.println("Usage: SampleClient [-l] METHOD URI [PAYLOAD]");
		System.out.println("  METHOD  : {GET, POST, PUT, DELETE, DISCOVER, OBSERVE}");
		System.out.println("  URI     : The URI to the remote endpoint or resource");
		System.out.println("  PAYLOAD : The data to send with the request");
		System.out.println("Options:");
		System.out.println("  -l      : Wait for multiple responses");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  SampleClient DISCOVER coap://localhost");
		System.out.println("  SampleClient POST coap://someServer.org:61616 my data");
	}

	/*
	 * Instantiates a new request based on a string describing a method.
	 * 
	 * @return A new request object, or null if method not recognized
	 */
	private static Request newRequest(String method) {
		if (method.equals("GET")) {
			return new GETRequest();
		} else if (method.equals("POST")) {
			return new POSTRequest();
		} else if (method.equals("PUT")) {
			return new PUTRequest();
		} else if (method.equals("DELETE")) {
			return new DELETERequest();
		} else if (method.equals("DISCOVER")) {
			return new GETRequest();
		} else if (method.equals("OBSERVE")) {
			return new GETRequest();
		} else {
			return null;
		}
	}

}

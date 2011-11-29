package examples;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import coap.GETRequest;
import coap.Request;
import coap.Response;

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
					response.log();
				} else {
					System.out.println("No response received.");
				}
				
			} catch (InterruptedException e) {
				System.err.println("Receiving of response interrupted: " + e.getMessage());
				System.exit(-1);
			}
			
		} else {
			// display help
			System.out.println("Usage: GETClient URI");
		}
	}

}

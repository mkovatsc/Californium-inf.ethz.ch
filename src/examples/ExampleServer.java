package examples;

import java.net.SocketException;

import coap.Request;
import endpoint.Endpoint;
import endpoint.LocalEndpoint;
import examples.resources.CarelessResource;
import examples.resources.HelloWorldResource;
import examples.resources.LargeResource;
import examples.resources.SeparateResource;
import examples.resources.StorageResource;
import examples.resources.TimeResource;
import examples.resources.ToUpperResource;
import examples.resources.ZurichWeatherResource;

/*
 * This class implements a more complex CoAP server for demonstration purposes.
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class ExampleServer extends LocalEndpoint {

	// exit codes for runtime errors
	public static final int ERR_INIT_FAILED = 1;
	
	/*
	 * Constructor for a new SampleServer
	 * 
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
		addResource(new CarelessResource());
	}

	// Logging /////////////////////////////////////////////////////////////////
	
	@Override
	public void handleRequest(Request request) {
		
		// output the request
		System.out.printf("Incoming request from %s:%d:\n", request.getAddress(), request.getPort());
		request.log();
		
		// handle the request
		super.handleRequest(request);
	}

	
	// Application entry point /////////////////////////////////////////////////
	
	public static void main(String[] args) {
		
		// create server
		try {
			
			Endpoint server = new ExampleServer();
			
			System.out.printf("SampleServer listening on port %d.\n", server.port());
			
		} catch (SocketException e) {

			System.err.printf("Failed to create SampleServer: %s\n", e.getMessage());
			System.exit(ERR_INIT_FAILED);
		}
		
	}

}

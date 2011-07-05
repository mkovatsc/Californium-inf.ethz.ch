package demonstrationServer;

import java.net.SocketException;

import coap.Communicator;
import coap.Request;

import demonstrationServer.resources.*;

import endpoint.Endpoint;
import endpoint.LocalEndpoint;

/*
 * This class implements a simple CoAP server for demonstration purposes.
 * 
 * Currently, it just provides some simple resources.
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class DemonstrationServer extends LocalEndpoint {

	/*
	 * Constructor for a new DemonstrationServer
	 */
	public DemonstrationServer(int port, int defaultBlockSize) throws SocketException {

		super(port, defaultBlockSize);
		
		// add resources to the server
		addResource(new HelloWorldResource());
		addResource(new StorageResource());
		addResource(new ToUpperResource());
		addResource(new SeparateResource());
		addResource(new TimeResource());
		addResource(new ZurichWeatherResource());
		addResource(new FeedbackResource());
		addResource(new MirrorResource());
		addResource(new LargeResource());
		addResource(new CarelessResource());
	}

	// Logging /////////////////////////////////////////////////////////////////

	@Override
	public void handleRequest(Request request) {

		// output the request
		System.out.println("Incoming request:");
		request.log();

		// handle the request
		super.handleRequest(request);
	}

	// Application entry point /////////////////////////////////////////////////

	public static void main(String[] args) {

		int port = Communicator.DEFAULT_PORT;
		int defaultBlockSize = Communicator.DEFAULT_BLOCK_SIZE;
		
		// input parameters
		// syntax: DemonstrationServer.jar [PORT] [BLOCKSIZE]
		
		for (String arg : args) {
			System.out.println(arg);
		}
		
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		if (args.length > 1) {
			defaultBlockSize = Integer.parseInt(args[1]);
		}
		
		// create server
		try {

			Endpoint server = new DemonstrationServer(port, defaultBlockSize);

			System.out.printf("Californium DemonstrationServer listening at port %d.\n",
					server.port());
			
			if (defaultBlockSize < 0) {
				System.out.println("Outgoing block-wise transfer disabled");
			}

		} catch (SocketException e) {

			System.err.printf("Failed to create DemonstrationServer: %s\n",
					e.getMessage());
			return;
		}

	}

}

package ch.ethz.inf.vs.californium.example;

import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.Server;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointAddress;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;

/**
 * This is an example server that contains a few resources for demonstration.
 * 
 * @author Martin Lanter
 */
public class ExampleServer {

	public static void main(String[] args) {
		System.out.println("Starting Example Server");
		
		// Disable message logging
		Server.LOG_ENABLED = false;
		CalifonriumLogger.disableLogging();
		
		// Network configuration optimal for performance benchmarks
		NetworkConfig.createStandardWithoutFile()
			// Disable deduplication OR strongly reduce lifetime
			.setBoolean(NetworkConfigDefaults.ENABLE_DOUBLICATION, false)
			.setInt(NetworkConfigDefaults.EXCHANGE_LIFECYCLE, 1500)
			.setInt(NetworkConfigDefaults.MARK_AND_SWEEP_INTERVAL, 2000)
			
			// Increase buffer for network interface to 10 MB
			.setInt(NetworkConfigDefaults.UDP_CONNECTOR_RECEIVE_BUFFER, 10*1000*1000)
			.setInt(NetworkConfigDefaults.UDP_CONNECTOR_SEND_BUFFER, 10*1000*1000)
		
			// Increase threads for receiving and sending packets through the socket
			.setInt(NetworkConfigDefaults.UDP_CONNECTOR_RECEIVER_THREAD_COUNT,
					Runtime.getRuntime().availableProcessors())
			.setInt(NetworkConfigDefaults.UDP_CONNECTOR_SENDER_THREAD_COUNT,
					Runtime.getRuntime().availableProcessors());
		
		// Create server that listens on port 5683
		Server server = new Server();
		server.setExecutor(Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()));
		server.addEndpoint(new Endpoint(new EndpointAddress(null, 5683)));
		server.add(new HelloWorldResource("hello"));
		server.add(new StorageResource("storage"));
		server.add(new ImageResource("image"));
		server.add(new MirrorResource("mirror"));
		server.add(new LargeResource("large"));
		server.add(new RunningResource("running", server));
		server.start();
	}
	
	/*
	 *  Sends a GET request to itself
	 */
	public static void selfTest() {
		try {
			Request request = Request.newGet();
			request.setURI("localhost:5683/hello");
			request.send();
			Response response = request.waitForResponse(1000);
			System.out.println("received "+response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

package ch.inf.vs.californium.example;

import ch.inf.vs.californium.CalifonriumLogger;
import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.NetworkConfig;

/**
 * This is an example server that contains a few resources for demonstration.
 * 
 * @author Martin Lanter
 */
public class ExampleServer {

	public static void main(String[] args) {
		
		/*
		 * These are preferences for highest performance
		 * 	-Xms3000m -Xmx3000m
		 * 	-XX:NewSize=1500m
		 * 	-XX:NewRatio=1
		 * 	-XX:+ExplicitGCInvokesConcurrent -XX:+UseConcMarkSweepGC
		 * 	-XX:+CMSIncrementalMode
		 * 	-XX:GCTimeRatio=32
		 */
		
		// Disable message logging
		Server.LOG_ENABLED = false;
		CalifonriumLogger.disableLogging();
		
		// Disable deduplication OR strongly reduce lifetime
		NetworkConfig.getStandard().setEnableDedublication(false);
		NetworkConfig.getStandard().setExchangeLifecycle(1500);
		NetworkConfig.getStandard().setMarkAndSweepInterval(2000);
		
		// Increase buffer for network interface to 10 MB
		NetworkConfig.getStandard().setReceiveBuffer(10*1000*1000);
		NetworkConfig.getStandard().setSendBuffer(10*1000*1000);
		
		// Create server that listens on port 5683
		Server server = new Server();
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

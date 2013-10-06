package ch.ethz.inf.vs.californium.bench;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.Server;

/**
 * This is an example server that contains a few resources for demonstration.
 * 
 * -Xms4000m -Xmx4000m
-XX:+ExplicitGCInvokesConcurrent -XX:+UseConcMarkSweepGC
-XX:+CMSIncrementalMode
-XX:NewSize=2000m
-XX:NewRatio=1
-XX:GCTimeRatio=32

 * @author Martin Lanter
 */
public class BenchmarkServer {
	
	public static int udp_sender;
	public static int udp_receiver;
	public static int pool; // If we don't use executor in endpoint

	public static void main(String[] args) throws Exception {
//		args = new String[] {"-ports", "1", "5683" ,"-pool", "4", "-udp-sender", "0"};
		String address = null;
		int port = 5683;
		int cores = Runtime.getRuntime().availableProcessors();
		udp_sender = cores;
		udp_receiver = cores;
		pool = cores;
		int[] ports = null;
		
		if (args.length > 0) {
			int index = 0;
			while (index < args.length) {
				String arg = args[index];
				if ("-pool".equals(arg)) {
					pool = Integer.parseInt(args[index+1]);
				} else if ("-udp-sender".equals(arg)) {
					udp_sender = Integer.parseInt(args[index+1]);
				} else if ("-udp-receiver".equals(arg)) {
					udp_receiver = Integer.parseInt(args[index+1]);
				} else if ("-port".equals(arg)) {
					port = Integer.parseInt(args[index+1]);
				} else if ("-ports".equals(arg)) {
					int cn = Integer.parseInt(args[index+1]);
					ports = new int[cn];
					for (int i=0;i<cn;i++) ports[i] = Integer.parseInt(args[index+2+i]);
					index += cn;
				} else if ("-address".equals(arg)) {
					address = args[index+1];
				} else {
					System.err.println("Unknwon arg "+arg);
				}
				index += 2;
			}
		}
		
		InetAddress addr = address!=null ? InetAddress.getByName(address) : null;
		System.out.println("Starting Example Server");
		System.out.println("Available cores: " + cores);
		System.out.println("Use thread pool of size "+pool);
		System.out.println("Bind to address "+addr);
		if (ports != null)
			System.out.println("Bind to ports "+Arrays.toString(ports));
		else System.out.println("Bind to port "+port);
		
		setBenchmarkConfiguration();

		Server server = createServer();
		if (ports != null) {
			for (int p:ports) {
//				Server server = createServer();
				server.addEndpoint(new Endpoint(new InetSocketAddress((InetAddress) addr, p)));
//				server.start();
			}
		} else {
//			Server server = createServer();
			server.addEndpoint(new Endpoint(new InetSocketAddress((InetAddress) addr, port)));
//			server.start();
		}
		server.start();
	}
	
	private static Server createServer() {
		Server server = new Server();
		server.setExecutor(Executors.newScheduledThreadPool(pool));
//		server.setExecutor(new CfExecutor(pool));
		server.add(new HelloWorldResource("hello"));
		server.add(new FibonacciResource("fibonacci"));
		return server;
	}
	
	private static void setBenchmarkConfiguration() {
		
		// Network configuration optimal for performance benchmarks
		NetworkConfig.createStandardWithoutFile()
			// Disable deduplication OR strongly reduce lifetime
			.setString(NetworkConfigDefaults.DEDUPLICATOR, NetworkConfigDefaults.NO_DEDUPLICATOR)
			.setInt(NetworkConfigDefaults.EXCHANGE_LIFECYCLE, 1500)
			.setInt(NetworkConfigDefaults.MARK_AND_SWEEP_INTERVAL, 2000)
			
			// Increase buffer for network interface to 10 MB
			.setInt(NetworkConfigDefaults.UDP_CONNECTOR_RECEIVE_BUFFER, 10*1000*1000)
			.setInt(NetworkConfigDefaults.UDP_CONNECTOR_SEND_BUFFER, 10*1000*1000)
		
			// Increase threads for receiving and sending packets through the socket
			.setInt(NetworkConfigDefaults.UDP_CONNECTOR_RECEIVER_THREAD_COUNT, udp_receiver)
			.setInt(NetworkConfigDefaults.UDP_CONNECTOR_SENDER_THREAD_COUNT, udp_sender)
			
			// Disable message logging
			.setBoolean(NetworkConfigDefaults.LOG_MESSAGES, false)
			.setBoolean(NetworkConfigDefaults.UDP_CONNECTOR_LOG_PACKETS, false);
		
		CalifonriumLogger.disableLogging();
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

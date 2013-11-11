package ch.ethz.inf.vs.californium.bench;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.Server;

/**
 * This server has optimal parameters for benchmarking. The optimal JVM
 * parameters on my machine were:
 * <pre>
 * -Xms4000m -Xmx4000m
 * </pre>
 * @author Martin Lanter
 */
public class BenchmarkServer {
	
	public static final int CORES = Runtime.getRuntime().availableProcessors();
	
	public static final String DEFAULT_ADDRESS = null;
	public static final int DEFAULT_PORT = 5683;
	public static final int DEFAULT_SENDER_COUNT = CORES;
	public static final int DEFAULT_RECEIVER_COUNT = CORES;
	public static final int DEFAULT_ENDPOINT_THREAD_COUNT = CORES;

	public static void main(String[] args) throws Exception {
//		args = new String[] {"-port", "7777" ,"-pool", "500", "-udp-sender", "1"};
		System.out.println("This machine has "+CORES+" cores");
		
		String address = null;
		int port = DEFAULT_PORT;
		int udp_sender = DEFAULT_SENDER_COUNT;
		int udp_receiver = DEFAULT_RECEIVER_COUNT;
		int endpoint_threads = DEFAULT_ENDPOINT_THREAD_COUNT;
		
		if (args.length > 0) {
			int index = 0;
			while (index < args.length) {
				String arg = args[index];
				if ("-pool".equals(arg)) {
					endpoint_threads = Integer.parseInt(args[index+1]);
				} else if ("-udp-sender".equals(arg)) {
					udp_sender = Integer.parseInt(args[index+1]);
				} else if ("-udp-receiver".equals(arg)) {
					udp_receiver = Integer.parseInt(args[index+1]);
				} else if ("-port".equals(arg)) {
					port = Integer.parseInt(args[index+1]);
				} else if ("-address".equals(arg)) {
					address = args[index+1];
				} else {
					System.err.println("Unknwon arg "+arg);
				}
				index += 2;
			}
		}
		
		InetAddress addr = address!=null ? InetAddress.getByName(address) : null;
		InetSocketAddress sockAddr = new InetSocketAddress((InetAddress) addr, port);
		System.out.println("Start benchmark server and bind to " + sockAddr);
		System.out.println("UDP connector uses "+udp_sender+" sender and "+udp_receiver+" receiver threads");
		System.out.println("Endpoints uses thread-pool with "+endpoint_threads+" threads");
		
		setBenchmarkConfiguration(udp_sender, udp_receiver);

		Server server = new Server();
		server.setExecutor(Executors.newScheduledThreadPool(endpoint_threads));
		server.add(new BenchmarkResource("benchmark"));
		server.add(new FibonacciResource("fibonacci"));
		server.addEndpoint(new CoAPEndpoint(sockAddr));
		server.start();
	}
	
	private static void setBenchmarkConfiguration(int udp_sender, int udp_receiver) {
		
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
		
		/*
		 * Since we have already disabled LOG_MESSAGES and UDP_CONNECTOR_LOG_PACKETS,
		 * there should only be some log entries for the server startup. If we also
		 * want to get rid of those, disable all logging here:
		 */
//		 CalifonriumLogger.disableLogging();
	}
	
	/*
	 *  Sends a GET request to itself
	 */
	public static void selfTest() {
		try {
			Request request = Request.newGet();
			request.setURI("localhost:5683/benchmark");
			request.send();
			Response response = request.waitForResponse(1000);
			System.out.println("received "+response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

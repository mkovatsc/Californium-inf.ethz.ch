package ch.ethz.inf.vs.californium.test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Matcher;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This test is deprecated by now!
 * <p>
 * This is not a JUnit test but has to be verified manually.
 * <p>
 * The Mark-And-Sweep algorithm is supposed to remove all entries from the
 * {@link Matcher}'s deduplication HashMap.
 */
@Deprecated
@Ignore
public class MarkAndSweepTest {

	public static final long TIME = 6*1000; // ms
	public static final String TARGET = "test";
	public static final int MARK_AND_SWEEP_INTERVAL = 200;
	public static final int EXCHANGE_LIFECYCLE = 250;
	
	private static Server server;
	private int serverPort;
	
	@Before
	public void startupServer() {
		System.out.println("\nStart "+getClass().getSimpleName());
		createServer();
	}
	
	@After
	public void shutdown() {
		server.destroy();
		EndpointManager.getEndpointManager().getDefaultEndpoint().destroy();
		
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void test() throws Exception {
		
		System.gc();
		long usedMemory0 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;
		
		// Exchange ~ 60'000 messages (not more because MIDs must fit into 16 bit) 
		long start = System.currentTimeMillis();
		ArrayList<Request> requests = new ArrayList<Request>();
		while(start + TIME > System.currentTimeMillis()) { // for one minute
			
			for (int j=0;j<50;j++) {
				Request request = new Request(Code.GET);
				request.setURI("coap://localhost:"+serverPort+"/"+TARGET);
				request.send();
				requests.add(request);
			}
			Thread.sleep(50);
		}
		
		// Matcher's Mark-And-Sweep should now clear all hash maps.
		
		for (Request request:requests)
			Assert.assertNotNull(request.waitForResponse(10000));
		requests.clear();
		
		System.out.println("Wait for three Mark-And-Sweep intervals");
		Thread.sleep(3 * MARK_AND_SWEEP_INTERVAL + 500);
		System.gc();
		Thread.sleep(1000);
		
		long usedMemory1 = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024;
		
		System.out.println("Memory used after server start: "+usedMemory0+" KB");
		System.out.println("Memory used after "+TIME+" ms: "+usedMemory1+" KB");
		Assert.assertTrue(usedMemory1 - usedMemory0 < 1024*1024); // less than 1 MB
	}
	
	
	private void createServer() {
		EndpointManager.getEndpointManager().getDefaultEndpoint().getConfig()
			.setInt(NetworkConfigDefaults.MARK_AND_SWEEP_INTERVAL, MARK_AND_SWEEP_INTERVAL)
			.setInt(NetworkConfigDefaults.EXCHANGE_LIFECYCLE, EXCHANGE_LIFECYCLE);
		
		NetworkConfig config = new NetworkConfig()
			.setInt(NetworkConfigDefaults.MARK_AND_SWEEP_INTERVAL, MARK_AND_SWEEP_INTERVAL)
			.setInt(NetworkConfigDefaults.EXCHANGE_LIFECYCLE, EXCHANGE_LIFECYCLE);

		server = new Server();
		server.add(new ResourceBase(TARGET) {
			private AtomicInteger counter = new AtomicInteger();
			public void handleRequest(Exchange exchange) {
				exchange.accept();
				exchange.respond("Hello "+counter.incrementAndGet());
			}
		});
		CoAPEndpoint endpoint = new CoAPEndpoint(new InetSocketAddress((InetAddress) null, 0), config);
		server.addEndpoint(endpoint);
		server.start();
		serverPort = endpoint.getAddress().getPort();
	}
}

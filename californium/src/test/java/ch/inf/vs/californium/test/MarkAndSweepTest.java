package ch.inf.vs.californium.test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.EndpointManager;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.Matcher;
import ch.inf.vs.californium.network.NetworkConfig;
import ch.inf.vs.californium.resources.ResourceBase;

/**
 * This is not a JUnit test but has to be verified manually.
 * <p>
 * The Mark-And-Sweep algorithm is supposed to remove all entries from the
 * {@link Matcher}'s deduplication HashMap.
 * TODO: Reduce MAS interval
 */
@Ignore
public class MarkAndSweepTest {

	public static final long TIME = 6*1000; // ms
	public static final int SERVER_PORT = 7777;
	public static final String TARGET = "test";
	public static final int MARK_AND_SWEEP_INTERVAL = 200;
	public static final int EXCHANGE_LIFECYCLE = 250;
	
	private static Server server;
	
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
		ArrayList<Request> requests = new ArrayList<>();
		while(start + TIME > System.currentTimeMillis()) { // for one minute
			
			for (int j=0;j<50;j++) {
				Request request = new Request(Code.GET);
				request.setURI("localhost:"+SERVER_PORT+"/"+TARGET);
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
		NetworkConfig c = EndpointManager.getEndpointManager().getDefaultEndpoint().getConfig();
		c.setMarkAndSweepInterval(MARK_AND_SWEEP_INTERVAL);
		c.setExchangeLifecycle(EXCHANGE_LIFECYCLE);
		
		NetworkConfig config = new NetworkConfig();
		config.setMarkAndSweepInterval(MARK_AND_SWEEP_INTERVAL);
		config.setExchangeLifecycle(EXCHANGE_LIFECYCLE);
		server = new Server();
		server.add(new ResourceBase(TARGET) {
			private AtomicInteger counter = new AtomicInteger();
			public void processRequest(Exchange exchange) {
				exchange.accept();
				exchange.respond("Hello "+counter.incrementAndGet());
			}
		});
		server.addEndpoint(new Endpoint(new EndpointAddress(null, SERVER_PORT), config));
		server.start();
	}
}

package ch.ethz.inf.vs.californium.test.maninmiddle;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.ethz.inf.vs.californium.CaliforniumLogger;
import ch.ethz.inf.vs.californium.CoapClient;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;
import ch.ethz.inf.vs.elements.UDPConnector;

/**
 * This test randomly drops packets of a blockwise transfer and checks if the
 * transfer still succeeds.
 */
@Ignore // Not done yet
public class LossyBlockwiseTransferTest {

	private static boolean RANDOM_PAYLOAD_GENERATION = true;
	
	static {
		CaliforniumLogger.initialize();
		Logger.getLogger(UDPConnector.class.toString()).setLevel(Level.OFF);
//		Logger.getLogger(ReliabilityLayer.class.getCanonicalName()).setLevel(Level.ALL);
	}
	
	private Server server;
	private Endpoint client;
	private ManInTheMiddle middle;
	
	private InetSocketAddress serverAddress;
	private InetSocketAddress clientAddress;
	private InetSocketAddress middleAddress;
	
	private TestResource testResource;
	private String respPayload;
	private String reqtPayload;
	
	@Before
	public void setupServer() throws Exception {
		System.out.println("\nStart "+getClass().getSimpleName());
		
		NetworkConfig config = new NetworkConfig()
			.setInt(NetworkConfigDefaults.ACK_TIMEOUT, 200)
			.setFloat(NetworkConfigDefaults.ACK_RANDOM_FACTOR, 1)
			.setInt(NetworkConfigDefaults.ACK_TIMEOUT_SCALE, 1)
			.setInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE, 32)
			.setInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE, 32);
		
		client = new CoAPEndpoint(new InetSocketAddress(5683) /* TODO: remove addr */, config);
		client.setMessageDeliverer(new EndpointManager.ClientMessageDeliverer());
		client.start();

		server = new Server(config, 7777 /* TODO: change to 0 */);
		testResource = new TestResource("test");
		server.add(testResource);
		server.start();
		
		InetAddress localhost = InetAddress.getLocalHost();
		serverAddress = new InetSocketAddress(localhost, server.getEndpoints().get(0).getAddress().getPort());
		clientAddress = new InetSocketAddress(localhost, client.getAddress().getPort());
		
		middle = new ManInTheMiddle(clientAddress, serverAddress);
		middleAddress = middle.getAddress();
		
		System.out.println("Server at "+serverAddress+", client at "+clientAddress+", middle at "+middleAddress);
	}
	
	@After
	public void shutdownServer() {
		System.out.println();
		server.destroy();
		client.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void test() throws Throwable {
		try {
			
			String uri = "coap://192.168.1.101:" + middleAddress.getPort()+"/test";
			reqtPayload = "";
			respPayload = generatePayload(200);
			
			System.out.println("uri: "+uri);

			CoapClient coapclient = new CoapClient(uri);
			coapclient.setTimeout(5000);
			coapclient.setEndpoint(client);
			
			middle.drop(5,6,8,9,15);
			
			String resp = coapclient.get().getResponseText();
			Assert.assertEquals(respPayload, resp);

			Random rand = new Random();
			for (int i=0;i<20;i++) {
				int[] numbers = new int[rand.nextInt(10)];
				for (int j=0;j<numbers.length;j++)
					numbers[j] = rand.nextInt(20);
				
				middle.reset();
				middle.drop(numbers);
				resp = coapclient.get().getResponseText();
				Assert.assertEquals(respPayload, resp);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} catch (Throwable t) {
			System.err.println(t);
			throw t;
		}
	}
	
	private static String generatePayload(int length) {
		StringBuffer buffer = new StringBuffer();
		if (RANDOM_PAYLOAD_GENERATION) {
			Random rand = new Random();
			while(buffer.length() < length) {
				buffer.append(rand.nextInt());
			}
		} else { // Deterministic payload
			int n = 1;
			while(buffer.length() < length) {
				buffer.append(n++);
			}
		}
		return buffer.substring(0, length);
	}
	
	// All tests are made with this resource
	private class TestResource extends ResourceBase {
		
		public TestResource(String name) { 
			super(name);
		}
		
		public void handleGET(CoapExchange exchange) {
			exchange.respond(ResponseCode.CONTENT, respPayload);
		}
		
		public void handlePUT(CoapExchange exchange) {
			System.out.println("Server has received request payload: "+exchange.getRequestText());
			Assert.assertEquals(reqtPayload, exchange.getRequestText());
			exchange.respond(ResponseCode.CHANGED, respPayload);
		}
		
		public void handlePOST(CoapExchange exchange) {
			System.out.println("Server has received request payload: "+exchange.getRequestText());
			Assert.assertEquals(reqtPayload, exchange.getRequestText());
			exchange.respond(ResponseCode.CHANGED, respPayload);
		}
	}
}

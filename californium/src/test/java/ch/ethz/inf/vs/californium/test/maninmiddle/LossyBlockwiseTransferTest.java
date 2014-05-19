package ch.ethz.inf.vs.californium.test.maninmiddle;

import java.net.InetSocketAddress;
import java.util.Random;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

/**
 * This test randomly drops packets of a blockwise transfer and checks if the
 * transfer still succeeds.
 */
public class LossyBlockwiseTransferTest {

	private static boolean RANDOM_PAYLOAD_GENERATION = true;
	
	private Server server;
	private Endpoint client;
	private ManInTheMiddle middle;
	
	private int clientPort;
	private int serverPort;
	private int middlePort;
	
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
		
		client = new CoAPEndpoint(new InetSocketAddress(0), config);
		client.setMessageDeliverer(new EndpointManager.ClientMessageDeliverer());
		client.start();
		
		server = new Server(config, 0);
		testResource = new TestResource("test");
		server.add(testResource);
		server.start();

		clientPort = client.getAddress().getPort();
		serverPort = server.getEndpoints().get(0).getAddress().getPort();
		middle = new ManInTheMiddle(clientPort, serverPort);
		middlePort = middle.getPort();
		
		System.out.println("Client at "+clientPort+", middle at "+middlePort+", server at "+serverPort);
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
			
			String uri = "coap://localhost:" + middlePort + "/test";
			reqtPayload = "";
			respPayload = generatePayload(250);
			
			System.out.println("uri: "+uri);

			CoapClient coapclient = new CoapClient(uri);
			coapclient.setTimeout(5000);
			coapclient.setEndpoint(client);
			
			middle.drop(5,6,8,9,15);
			
			String resp = coapclient.get().getResponseText();
			Assert.assertEquals(respPayload, resp);
			System.out.println("Received " + resp.length() + " bytes");

			Random rand = new Random();
			
			for (int i=0;i<5;i++) {
				int[] numbers = new int[10];
				for (int j=0;j<numbers.length;j++)
					numbers[j] = rand.nextInt(16);
				
				middle.reset();
				middle.drop(numbers);
				
				resp = coapclient.get().getResponseText();
				Assert.assertEquals(respPayload, resp);
				System.out.println("Received " + resp.length() + " bytes");
				
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

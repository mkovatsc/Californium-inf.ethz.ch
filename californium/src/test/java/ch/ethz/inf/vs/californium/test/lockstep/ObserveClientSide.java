package ch.ethz.inf.vs.californium.test.lockstep;

import static ch.ethz.inf.vs.californium.coap.CoAP.Code.GET;
import static ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode.CONTENT;
import static ch.ethz.inf.vs.californium.coap.CoAP.Type.ACK;
import static ch.ethz.inf.vs.californium.coap.CoAP.Type.CON;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointManager.ClientMessageDeliverer;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;

/**
 * This test implements all examples from the blockwise draft 14 for a client.
 */
public class ObserveClientSide {

	private static boolean RANDOM_PAYLOAD_GENERATION = true;
	
	private LockstepEndpoint server;
	
	private Endpoint client;
	private int clientPort = 5683;
	
	private int mid = 8000;
	
	private String respPayload;
	
	private ClientBlockwiseInterceptor clientInterceptor = new ClientBlockwiseInterceptor();
	
	@Before
	public void setupServer() throws IOException {
		System.out.println("\nStart "+getClass().getSimpleName());
		
		NetworkConfig config = new NetworkConfig()
			.setInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE, 32)
			.setInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE, 32)
			.setInt(NetworkConfigDefaults.ACK_TIMEOUT, 200) // client retransmits after 200 ms
			.setInt(NetworkConfigDefaults.ACK_RANDOM_FACTOR, 1);
		client = new CoAPEndpoint(new InetSocketAddress(clientPort), config);
		client.setMessageDeliverer(new ClientMessageDeliverer());
		client.addInterceptor(clientInterceptor);
		client.start();
		clientPort = client.getAddress().getPort();
		System.out.println("Client binds to port "+clientPort);
	}
	
	@After
	public void shutdownServer() {
		System.out.println();
		client.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void test() throws Throwable {
		try {
			testGETWithLostACK();
			testGETObserveWithLostACK();
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Make sure you did not forget a .go() at the end of a line.");
			throw e;
		} catch (Throwable t) {
			System.err.println(t);
			throw t;
		}
	}
	
	private void testGETWithLostACK() throws Exception {
		System.out.println("Simple blockwise GET:");
		respPayload = generatePayload(10);
		String path = "test";
		server = createLockstepEndpoint();
		
		Request request = createRequest(GET, path);
		client.sendRequest(request);
		
		server.expectRequest(CON, GET, path).storeMID("A").storeToken("B").go(); // lost;
		clientInterceptor.log(" // lost");
		server.expectRequest(CON, GET, path).loadMID("A").storeToken("B").go(); // lost;
		
		server.sendEmpty(ACK).loadMID("A").go();
		Thread.sleep(50);
		server.sendResponse(CON, CONTENT).loadToken("B").payload(respPayload).mid(++mid).go();
		server.expectEmpty(ACK, mid).mid(mid).go(); // lost
		clientInterceptor.log(" // lost");
		server.sendResponse(CON, CONTENT).loadToken("B").payload(respPayload).mid(mid).go();
		server.expectEmpty(ACK, mid).mid(mid).go(); // lost
		clientInterceptor.log(" // lost");
		server.sendResponse(CON, CONTENT).loadToken("B").payload(respPayload).mid(mid).go();
		server.expectEmpty(ACK, mid).mid(mid).go();
		
		Response response = request.waitForResponse(1000);
		Assert.assertNotNull("Client received no response", response);
		Assert.assertEquals("Client received wrong response code:", CONTENT, response.getCode());
		Assert.assertEquals("Client received wrong payload:", respPayload, response.getPayloadString());
		
		printServerLog();
	}
	
	private void testGETObserveWithLostACK() throws Exception {
		System.out.println("Simple blockwise GET:");
		respPayload = generatePayload(10);
		String path = "test";
		server = createLockstepEndpoint();
		int obs = 100;
		
		Request request = createRequest(GET, path);
		request.setObserve();
		client.sendRequest(request);
		
		server.expectRequest(CON, GET, path).storeMID("A").storeToken("B").observe(0).go();
		server.sendEmpty(ACK).loadMID("A").go();
		Thread.sleep(50);
		server.sendResponse(CON, CONTENT).loadToken("B").payload(respPayload).mid(++mid).observe(++obs).go();
		server.expectEmpty(ACK, mid).go(); // lost
		clientInterceptor.log(" // lost");
		server.sendResponse(CON, CONTENT).loadToken("B").payload(respPayload).mid(mid).observe(obs).go();
		server.expectEmpty(ACK, mid).go();
		
		Response response = request.waitForResponse(1000);
		Assert.assertNotNull("Client received no response", response);
		Assert.assertEquals("Client received wrong response code:", CONTENT, response.getCode());
		Assert.assertEquals("Client received wrong payload:", respPayload, response.getPayloadString());
		System.out.println("Relation established");
		Thread.sleep(1000);
		
		respPayload = generatePayload(10); // changed
		server.sendResponse(CON, CONTENT).loadToken("B").payload(respPayload).mid(++mid).observe(++obs).go();

		server.expectEmpty(ACK, mid).go(); // lost
		
		clientInterceptor.log(" // lost");
		server.sendResponse(CON, CONTENT).loadToken("B").payload(respPayload).mid(mid).observe(obs).go();
		server.expectEmpty(ACK, mid).go();

		response = request.waitForResponse(1000);
		Assert.assertNotNull("Client received no response", response);
		Assert.assertEquals("Client received wrong response code:", CONTENT, response.getCode());
		Assert.assertEquals("Client received wrong payload:", respPayload, response.getPayloadString());
		printServerLog();
	}
	
	private LockstepEndpoint createLockstepEndpoint() {
		try {
			LockstepEndpoint endpoint = new LockstepEndpoint();
			endpoint.setDestination(new InetSocketAddress(InetAddress.getLocalHost(), clientPort));
			return endpoint;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private Request createRequest(Code code, String path) throws Exception {
		Request request = new Request(code);
		String uri = "coap://"+InetAddress.getLocalHost().getHostAddress()+":"+(server.getPort())+"/"+path;
		request.setURI(uri);
		return request; 
	}
	
	private void printServerLog() {
		System.out.println(clientInterceptor.toString());
		clientInterceptor.clear();
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
}

package ch.ethz.inf.vs.californium.test.lockstep;

import static ch.ethz.inf.vs.californium.coap.CoAP.Code.GET;
import static ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode.CONTENT;
import static ch.ethz.inf.vs.californium.coap.CoAP.Type.ACK;
import static ch.ethz.inf.vs.californium.coap.CoAP.Type.CON;
import static ch.ethz.inf.vs.californium.coap.CoAP.Type.NON;
import static ch.ethz.inf.vs.californium.coap.CoAP.Type.RST;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.Matcher;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.network.layer.Blockwise14Layer;
import ch.ethz.inf.vs.californium.network.layer.ObserveLayer;
import ch.ethz.inf.vs.californium.network.layer.ReliabilityLayer;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;
import ch.ethz.inf.vs.californium.test.BlockwiseTransfer14Test.ServerBlockwiseInterceptor;
import ch.ethz.inf.vs.elements.UDPConnector;

public class ObserveServerSide {

private static boolean RANDOM_PAYLOAD_GENERATION = true;
	
	private Server server;
	private int serverPort = 5683;
	
	private int mid = 7000;
	
	private TestObserveResource testObsResource;
	private String respPayload;
	private Type respType;
	private int timeout = 100;
	
	private ServerBlockwiseInterceptor serverInterceptor = new ServerBlockwiseInterceptor();
	
	@Before
	public void setupServer() {
		System.out.println("\nStart "+getClass().getSimpleName());
//		CalifonriumLogger.disableLogging();
		Logger ul = Logger.getLogger(UDPConnector.class.toString());
		ul.setLevel(Level.OFF);
		LockstepEndpoint.DEFAULT_VERBOSE = false;
		
		CalifonriumLogger.setLoggerLevel(Level.ALL,
				ObserveLayer.class, Blockwise14Layer.class, Matcher.class, ReliabilityLayer.class);
		
		testObsResource = new TestObserveResource("obs");
		
		NetworkConfig config = new NetworkConfig()
			.setInt(NetworkConfigDefaults.ACK_TIMEOUT, timeout)
			.setFloat(NetworkConfigDefaults.ACK_RANDOM_FACTOR, 1.0f)
			.setInt(NetworkConfigDefaults.ACK_TIMEOUT_SCALE, 1)
			.setInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE, 32)
			.setInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE, 32);
		server = new Server(config, serverPort);
		server.add(testObsResource);
		server.getEndpoints().get(0).addInterceptor(serverInterceptor);
		server.start();
	}
	
	@After
	public void shutdownServer() {
		System.out.println();
		server.destroy();
		CalifonriumLogger.setLoggerLevel(Level.INFO,
				ObserveLayer.class, Blockwise14Layer.class, Matcher.class, ReliabilityLayer.class);
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void test() throws Throwable {
		try {
			
			testEstablishmentAndTimeout();
			testEstablishmentAndTimeoutWithUpdateInMiddle();
			testEstablishmentAndRejectCancellation();
//			testObserveWithBlock();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} catch (Throwable t) {
			System.err.println(t);
			throw t;
		}
	}
	
	private void testEstablishmentAndTimeout() throws Exception {
		System.out.println("Establish an observe relation. Cancellation after timeout");
		respPayload = generatePayload(30);
		byte[] tok = generateNextToken();
		String path = "obs";
		
		LockstepEndpoint client = createLockstepEndpoint();
		respType = null;
		client.sendRequest(CON, GET, tok, ++mid).path(path).observe(0).go();
		client.expectResponse(ACK, CONTENT, tok, mid).storeObserve("Z").payload(respPayload).go();
		Assert.assertEquals("Resource has established relation:", 1, testObsResource.getObserverCount());
		serverInterceptor.log("\nObserve relation established");
		
		// First notification
		respType = NON;
		testObsResource.change("First notification");
		client.expectResponse().type(NON).code(CONTENT).token(tok).checkObs("Z", "A").payload(respPayload).go();
		
		// Second notification
		testObsResource.change("Second notification");
		client.expectResponse().type(NON).code(CONTENT).token(tok).checkObs("A", "B").payload(respPayload).go();
		
		// Third notification
		respType = CON;
		testObsResource.change("Third notification");
		client.expectResponse().type(CON).code(CONTENT).token(tok).storeMID("MID").checkObs("B", "C").payload(respPayload).go();
		client.sendEmpty(ACK).loadMID("MID").go();
		
		// Forth notification
		respType = NON;
		testObsResource.change("Forth notification");
		client.expectResponse().type(NON).code(CONTENT).token(tok).checkObs("C", "D").payload(respPayload).go();
		
		// Fifth notification
		respType = CON;
		testObsResource.change("Fifth notification");
		client.expectResponse().type(CON).code(CONTENT).token(tok).storeMID("MID").checkObs("D", "E").payload(respPayload).go();
		serverInterceptor.log("// lost");
		client.expectResponse().type(CON).code(CONTENT).token(tok).loadMID("MID").loadObserve("E").payload(respPayload).go();
		serverInterceptor.log("// lost");
		client.expectResponse().type(CON).code(CONTENT).token(tok).loadMID("MID").loadObserve("E").payload(respPayload).go();
		serverInterceptor.log("// lost");
		client.expectResponse().type(CON).code(CONTENT).token(tok).loadMID("MID").loadObserve("E").payload(respPayload).go();
		serverInterceptor.log("// lost");
		client.expectResponse().type(CON).code(CONTENT).token(tok).loadMID("MID").loadObserve("E").payload(respPayload).go();
		serverInterceptor.log("// lost");
		
		Thread.sleep(timeout+100);
		
		Assert.assertEquals("Resource has not removed relation:", 0, testObsResource.getObserverCount());
		
		printServerLog();
	}
	
	private void testEstablishmentAndTimeoutWithUpdateInMiddle() throws Exception {
		System.out.println("Establish an observe relation. Cancellation after timeout. During the timeouts, the resource still changes.");
		respPayload = generatePayload(30);
		byte[] tok = generateNextToken();
		String path = "obs";
		
		LockstepEndpoint client = createLockstepEndpoint();
		respType = null;
		client.sendRequest(CON, GET, tok, ++mid).path(path).observe(0).go();
		client.expectResponse(ACK, CONTENT, tok, mid).storeObserve("A").payload(respPayload).go();
		Assert.assertEquals("Resource has established relation:", 1, testObsResource.getObserverCount());
		serverInterceptor.log("\nObserve relation established");
		
		// First notification
		respType = CON;
		testObsResource.change("First notification "+generatePayload(10));
		client.expectResponse().type(CON).code(CONTENT).token(tok).storeMID("MID").checkObs("A", "B").payload(respPayload).go();
		serverInterceptor.log("// lost ");
		client.expectResponse().type(CON).code(CONTENT).token(tok).loadMID("MID").loadObserve("B").payload(respPayload).go();
		serverInterceptor.log("// lost (1. retransmission)");
		
		// Resource changes and sends next CON which will be transmitted after the former has timeouted
		testObsResource.change("Second notification "+generatePayload(10));
		client.expectResponse().type(CON).code(CONTENT).token(tok).storeMID("MID").checkObs("B", "C").payload(respPayload).go();
		serverInterceptor.log("// lost (2. retransmission)");
		
		// Resource changes. Even though the next notification is a NON it becomes
		// a CON because it replaces the retransmission of the former CON control notifiation
		respType = NON;
		testObsResource.change("Third notification "+generatePayload(10));
		client.expectResponse().type(CON).code(CONTENT).token(tok).storeMID("MID").checkObs("C", "D").payload(respPayload).go();
		serverInterceptor.log("// lost (3. retransmission)");
		
		client.expectResponse().type(CON).code(CONTENT).token(tok).loadMID("MID").loadObserve("D").payload(respPayload).go();
		serverInterceptor.log("// lost (4. retransmission)");
		
		Thread.sleep(timeout+100);
		Assert.assertEquals("Resource has not removed relation:", 0, testObsResource.getObserverCount());
		
		printServerLog();
	}
	
	private void testEstablishmentAndRejectCancellation() throws Exception {
		System.out.println("Establish an observe relation. Cancellation due to a reject from the client");
		respPayload = generatePayload(30);
		byte[] tok = generateNextToken();
		String path = "obs";
		
		LockstepEndpoint client = createLockstepEndpoint();
		respType = null;
		client.sendRequest(CON, GET, tok, ++mid).path(path).observe(0).go();
		client.expectResponse(ACK, CONTENT, tok, mid).storeObserve("A").payload(respPayload).go();
		Assert.assertEquals("Resource has established relation:", 1, testObsResource.getObserverCount());
		serverInterceptor.log("\nObserve relation established");
		
		// First notification
		respType = CON;
		testObsResource.change("First notification "+generatePayload(10));
		client.expectResponse().type(CON).code(CONTENT).token(tok).storeMID("MID").checkObs("A", "B").payload(respPayload).go();
		serverInterceptor.log("// lost ");
		client.expectResponse().type(CON).code(CONTENT).token(tok).loadMID("MID").loadObserve("B").payload(respPayload).go();
		
		System.out.println("Reject notification");
		client.sendEmpty(RST).loadMID("MID").go();
		
		Thread.sleep(100);
		Assert.assertEquals("Resource has not removed relation:", 0, testObsResource.getObserverCount());
		printServerLog();
	}
	
	private void testObserveWithBlock() throws Exception {
//		System.out.println("Observe with blockwise");
//		respPayload = generatePayload(10);
//		byte[] tok = generateNextToken();
//		String path = "obs";
//		
//		LockstepEndpoint client = createLockstepEndpoint();
//		respType = null;
////		client.sendRequest(CON, GET, tok, ++mid).path(path).observe(0).go();
////		client.expectResponse(ACK, CONTENT, tok, mid).storeObserve("A").block2(0, true, 32).payload(respPayload, 0, 32).go();
//		
////		byte[] tok2 = generateNextToken();
////		client.sendRequest(CON, GET
//		// First notification
//		respType = CON;
//		testObsResource.change("First notification "+generatePayload(10));
//		client.expectResponse().type(CON).code(CONTENT).token(tok).storeMID("MID").checkObs("A", "B").payload(respPayload).go();
//		serverInterceptor.log("// lost ");
//		client.expectResponse().type(CON).code(CONTENT).token(tok).loadMID("MID").loadObserve("B").payload(respPayload).go();
//		
//		System.out.println("Reject notification");
//		client.sendEmpty(RST).loadMID("MID").go();
//		
//		Thread.sleep(100);
//		Assert.assertEquals("Resource has not removed relation:", 0, testObsResource.getObserverCount());
//		printServerLog();
	}
	
	private LockstepEndpoint createLockstepEndpoint() {
		try {
			LockstepEndpoint endpoint = new LockstepEndpoint();
			endpoint.setDestination(new InetSocketAddress(InetAddress.getLocalHost(), serverPort));
			return endpoint;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void printServerLog() {
		System.out.println(serverInterceptor.toString());
		serverInterceptor.clear();
	}
	
	private static int currentToken = 10;
	private static byte[] generateNextToken() {
		return b(++currentToken);
	}
	
	private static byte[] b(int... is) {
		byte[] bytes = new byte[is.length];
		for (int i=0;i<bytes.length;i++)
			bytes[i] = (byte) is[i];
		return bytes;
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
	private class TestObserveResource extends ResourceBase {
		
		public TestObserveResource(String name) { 
			super(name);
			setObservable(true);
		}
		
		public void handleGET(Exchange exchange) {
			Response response = new Response(CONTENT);
			response.setType(respType);
			response.setPayload(respPayload);
			respond(exchange, response);
		}
		
		public void change(String newPayload) {
			System.out.println("Resource changed: "+newPayload);
			respPayload = newPayload;
			changed();
		}
	}
	
}

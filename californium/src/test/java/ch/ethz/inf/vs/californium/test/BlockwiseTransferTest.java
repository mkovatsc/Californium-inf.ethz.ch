package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CoAP;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointAddress;
import ch.ethz.inf.vs.californium.network.EndpointManager;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.MessageIntercepter;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.server.MessageDeliverer;
import ch.ethz.inf.vs.californium.server.Server;

/**
 * This test tests the blockwise transfer of requests and responses. This test
 * sets the maximum message size and the default block size to 32 bytes and
 * sends messages blockwise. All four combinations with short and long requests
 * and responses are tested.
 */
public class BlockwiseTransferTest {

	private static final String SHORT_REQUEST  = "<Short request>";
	private static final String LONG_REQUEST   = "<Long request 1x2x3x4x5x>".replace("x", "ABCDEFGHIJKLMNOPQRSTUVWXYZ ");
	private static final String SHORT_RESPONSE = "<Short response>";
	private static final String LONG_RESPONSE  = "<Long response 1x2x3x4x5x>".replace("x", "ABCDEFGHIJKLMNOPQRSTUVWXYZ ");
	
	private boolean request_short = true;
	private boolean respond_short = true;
	
	private Server server;
	private ServerBlockwiseInterceptor interceptor;
	private int serverPort;
	
	@Before
	public void setupServer() {
		try {
			System.out.println("\nStart "+getClass().getSimpleName());
			EndpointManager.clear();
			
			server = createSimpleServer();
			EndpointManager.getEndpointManager().getDefaultEndpoint().getConfig()
				.setInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE, 32)
				.setInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE, 32);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@After
	public void shutdownServer() {
		try {
			server.destroy();
			System.out.println("End "+getClass().getSimpleName());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Test
	public void test_all() throws Exception {
		test_POST_short_short();
		test_POST_long_short();
		test_POST_short_long();
		test_POST_long_long();
		test_GET_short();
		test_GET_long();
	}
	
	public void test_POST_short_short() throws Exception {
		request_short = true;
		respond_short = true;
		executePOSTRequest();
	}
	
	public void test_POST_long_short() throws Exception {
		request_short = false;
		respond_short = true;
		executePOSTRequest();
	}
	
	public void test_POST_short_long() throws Exception {
		request_short = true;
		respond_short = false;
		executePOSTRequest();
	}
	
	public void test_POST_long_long() throws Exception {
		request_short = false;
		respond_short = false;
		executePOSTRequest();
	}
	
	public void test_GET_short() throws Exception {
		respond_short = true;
		executeGETRequest();
	}
	
	public void test_GET_long() throws Exception {
		respond_short = false;
		executeGETRequest();
	}
	
	private void executeGETRequest() throws Exception {
		String payload = "nothing";
		try {
			interceptor.clear();
			Request request = Request.newGet();
			request.setDestination(InetAddress.getLocalHost());
			request.setDestinationPort(serverPort);
			request.send();
			
			// receive response and check
			Response response = request.waitForResponse(1000);
			
			assertNotNull(response);
			payload = response.getPayloadString();
			if (respond_short)
				assertEquals(payload, SHORT_RESPONSE);
			else assertEquals(payload, LONG_RESPONSE);
		} finally {
			Thread.sleep(100); // Quickly wait until last ACKs arrive
			System.out.println("Client received "+payload
				+ "\n" + interceptor.toString() + "\n");
		}
	}
	
	private void executePOSTRequest() throws Exception {
		String payload = "payload";
		try {
			interceptor.clear();
			Request request = new Request(CoAP.Code.POST);
			request.setDestination(InetAddress.getLocalHost());
			request.setDestinationPort(serverPort);
			request.getOptions().setBlock2(0, false, 0);
			if (request_short)
				request.setPayload(SHORT_REQUEST.getBytes());
			else request.setPayload(LONG_REQUEST.getBytes());
			request.send();
			
			// receive response and check
			Response response = request.waitForResponse(1000);
			
			assertNotNull(response);
			payload = response.getPayloadString();
			if (respond_short)
				assertEquals(payload, SHORT_RESPONSE);
			else assertEquals(payload, LONG_RESPONSE);
		} finally {
			Thread.sleep(100); // Quickly wait until last ACKs arrive
			System.out.println("Client received "+payload
				+ "\n" + interceptor.toString() + "\n");
		}
	}
	
	private Server createSimpleServer() {
		Server server = new Server();
		NetworkConfig config = new NetworkConfig();
		config.setInt(NetworkConfigDefaults.DEFAULT_BLOCK_SIZE, 32);
		config.setInt(NetworkConfigDefaults.MAX_MESSAGE_SIZE, 32);
		interceptor = new ServerBlockwiseInterceptor();
		Endpoint endpoind = new Endpoint(new EndpointAddress(0), config);
		endpoind.addInterceptor(interceptor);
		server.addEndpoint(endpoind);
		server.setMessageDeliverer(new MessageDeliverer() {
			@Override
			public void deliverRequest(Exchange exchange) {
				if (exchange.getRequest().getCode() == Code.GET)
					processGET(exchange);
				else
					processPOST(exchange);
			}
			
			private void processPOST(Exchange exchange) {
				String payload = exchange.getRequest().getPayloadString();
				if (request_short)
					assertEquals(payload, SHORT_REQUEST);
				else assertEquals(payload, LONG_REQUEST);
				System.out.println("Server received "+payload+"\n");
					
				Response response = new Response(ResponseCode.CONTENT);
				if (respond_short)
					response.setPayload(SHORT_RESPONSE);
				else response.setPayload(LONG_RESPONSE);
				exchange.respond(response);
			}
			
			private void processGET(Exchange exchange) {
				System.out.println("Server received GET request\n");
				Response response = new Response(ResponseCode.CONTENT);
				if (respond_short)
					response.setPayload(SHORT_RESPONSE);
				else response.setPayload(LONG_RESPONSE);
				exchange.respond(response);
			}
			
			@Override
			public void deliverResponse(Exchange exchange, Response response) { }
		});
		server.start();
		serverPort = endpoind.getAddress().getPort();
		System.out.println("serverPort: "+serverPort);
		return server;
	}
	
	private static class ServerBlockwiseInterceptor implements MessageIntercepter {

		private StringBuilder buffer = new StringBuilder();
		
		@Override
		public void sendRequest(Request request) {
			buffer.append("ERROR: Server sent "+request+"\n");
		}

		@Override
		public void sendResponse(Response response) {
			buffer.append(
					String.format("<-----   %s [MID=%d], %s%s%s\n",
					response.getType(), response.getMID(), response.getCode(),
					blockOptionString(1, response.getOptions().getBlock1()),
					blockOptionString(2, response.getOptions().getBlock2())));
		}

		@Override
		public void sendEmptyMessage(EmptyMessage message) {
			buffer.append(
					String.format("<-----   %s [MID=%d], 0\n",
					message.getType(), message.getMID()));
		}

		@Override
		public void receiveRequest(Request request) {
			buffer.append(
					String.format("%s [MID=%d], %s%s%s    ----->\n",
					request.getType(), request.getMID(), request.getCode(),
					blockOptionString(1, request.getOptions().getBlock1()),
					blockOptionString(2, request.getOptions().getBlock2())));
		}

		@Override
		public void receiveResponse(Response response) {
			buffer.append("ERROR: Server received "+response+"\n");
		}

		@Override
		public void receiveEmptyMessage(EmptyMessage message) {
			buffer.append(
					String.format("%s [MID=%d], 0                        ----->\n",
					message.getType(), message.getMID()));
		}
		
		private String blockOptionString(int nbr, BlockOption option) {
			if (option == null) return "";
			return String.format(", %d:%d/%d/%d", nbr, option.getNum(),
					option.isM()?1:0, option.getSize());
		}
		
		public String toString() {
			return buffer.toString();
		}
		
		public void clear() {
			buffer = new StringBuilder();
		}
		
	}
	
}

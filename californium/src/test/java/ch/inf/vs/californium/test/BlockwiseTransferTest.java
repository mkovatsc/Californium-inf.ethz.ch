package ch.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.inf.vs.californium.MessageDeliverer;
import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.BlockOption;
import ch.inf.vs.californium.coap.CoAP;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointManager;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.MessageIntercepter;
import ch.inf.vs.californium.network.NetworkConfig;

/**
 * This test sets the maximum message size and the default block size to 32
 * bytes and sends messages blockwise. All four combinations with short and long
 * requests and responses are tested.
 */
public class BlockwiseTransferTest {

	private static final int SERVER_PORT = 7777;
	private static final String SHORT_REQUEST  = "<Short request>";
	private static final String LONG_REQUEST   = "<Long request xxxxx>".replace("x", "ABCDEFGHIJKLMNOPQRSTUVWXYZ ");
	private static final String SHORT_RESPONSE = "<Short response>";
	private static final String LONG_RESPONSE  = "<Long response xxxxx>".replace("x", "ABCDEFGHIJKLMNOPQRSTUVWXYZ ");
	
	private boolean request_short = true;
	private boolean respond_short = true;
	
	private Server server;
	private ServerBlockwiseInterceptor interceptor;
	
	@Before
	public void setupServer() {
		System.out.println("\nStart "+getClass().getSimpleName());
		EndpointManager.clear();
		server = createSimpleServer(SERVER_PORT);
		EndpointManager.getEndpointManager().getDefaultEndpoint().getConfig().setDefaultBlockSize(32);
		EndpointManager.getEndpointManager().getDefaultEndpoint().getConfig().setMaxMessageSize(32);
	}
	
	@After
	public void shutdownServer() {
		server.destroy();
		System.out.println("End "+getClass().getSimpleName());
	}
	
	@Test
	public void test_all() throws Exception {
		test_short_short();
		test_long_short();
		test_short_long();
		test_long_long();
	}
	
	public void test_short_short() throws Exception {
		request_short = true;
		respond_short = true;
		executeRequest();
	}
	
	public void test_long_short() throws Exception {
		request_short = false;
		respond_short = true;
		executeRequest();
	}
	
	public void test_short_long() throws Exception {
		request_short = true;
		respond_short = false;
		executeRequest();
	}
	
	public void test_long_long() throws Exception {
		request_short = false;
		respond_short = false;
		executeRequest();
	}
	
	private void executeRequest() throws Exception {
		String payload = "nix";
		try {
			interceptor.clear();
			Request request = new Request(CoAP.Code.POST);
			request.setDestination(InetAddress.getLocalHost());
			request.setDestinationPort(SERVER_PORT);
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
	
	private Server createSimpleServer(int port) {
		Server server = new Server();
		NetworkConfig config = new NetworkConfig();
		config.setDefaultBlockSize(32);
		config.setMaxMessageSize(32);
		interceptor = new ServerBlockwiseInterceptor();
		Endpoint entpoind = new Endpoint(port, config);
		entpoind.addInterceptor(interceptor);
		server.addEndpoint(entpoind);
		server.setMessageDeliverer(new MessageDeliverer() {
			@Override
			public void deliverRequest(Exchange exchange) {
				String payload = exchange.getRequest().getPayloadString();
				if (request_short)
					assertEquals(payload, SHORT_REQUEST);
				else assertEquals(payload, LONG_REQUEST);
				System.out.println("Server  received "+payload+"\n");
					
				Response response = new Response(ResponseCode.CONTENT);
				if (respond_short)
					response.setPayload(SHORT_RESPONSE.getBytes());
				else response.setPayload(LONG_RESPONSE.getBytes());
				exchange.respond(response);
			}
			@Override
			public void deliverResponse(Exchange exchange, Response response) { }
		});
		server.start();
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
					response.getType(), response.getMid(), response.getCode(),
					blockOptionString(1, response.getOptions().getBlock1()),
					blockOptionString(2, response.getOptions().getBlock2())));
		}

		@Override
		public void sendEmptyMessage(EmptyMessage message) {
			buffer.append(
					String.format("<-----   %s [MID=%d], 0\n",
					message.getType(), message.getMid()));
		}

		@Override
		public void receiveRequest(Request request) {
			buffer.append(
					String.format("%s [MID=%d], %s%s%s    ----->\n",
					request.getType(), request.getMid(), request.getCode(),
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
					message.getType(), message.getMid()));
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

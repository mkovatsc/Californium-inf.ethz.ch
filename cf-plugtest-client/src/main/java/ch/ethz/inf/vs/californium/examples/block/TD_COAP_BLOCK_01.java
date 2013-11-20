package ch.ethz.inf.vs.californium.examples.block;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.CoAPEndpoint;
import ch.ethz.inf.vs.californium.network.MessageIntercepter;
import ch.ethz.inf.vs.californium.test.lockstep.LockstepEndpoint;

public class TD_COAP_BLOCK_01 implements Plugtest {

	private CoAPEndpoint client;
	
	private Checker checker = new Checker();
	
	public TD_COAP_BLOCK_01() {
		client = new CoAPEndpoint();
		client.addInterceptor(checker);
		client.start();
	}

	public boolean execute() throws Exception {
		Request request = new Request(Code.GET);
		request.setURI("coap://"+PlugTestClientBlock.serverURI+"/large");
		request.getOptions().setBlock2(BlockOption.size2Szx(64), false, 0);
		client.sendRequest(request);
		
		checker.expectRequest().block2(0, false, 64).go();

		return true;
	}

	@Override
	public String getIdentifier() {
		return "TD_COAP_BLOCK_01";
	}

	@Override
	public String getObjective() {
		return "Handle GET blockwise transfer for large resource (early negotiation)";
	}
	
	private class Checker extends LockstepEndpoint implements MessageIntercepter {

		private LinkedBlockingQueue<Request> requests = new LinkedBlockingQueue<Request>();
		
		@Override
		public Request waitForRequest() throws Exception {
			return requests.poll(1, TimeUnit.SECONDS);
		}
		
		@Override
		public void sendRequest(Request request) {
			requests.add(request);
		}

		@Override
		public void sendEmptyMessage(EmptyMessage message) {
			
		}

		@Override
		public void receiveResponse(Response response) {
			
		}

		@Override
		public void receiveEmptyMessage(EmptyMessage message) {
			
		}

		@Override // Client does not send responses
		public void sendResponse(Response response) { }

		@Override // Client does not receive requests
		public void receiveRequest(Request request) { }
	}
}

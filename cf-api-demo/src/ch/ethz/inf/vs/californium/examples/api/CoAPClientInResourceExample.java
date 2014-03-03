package ch.ethz.inf.vs.californium.examples.api;

import ch.ethz.inf.vs.californium.CoapClient;
import ch.ethz.inf.vs.californium.CoapHandler;
import ch.ethz.inf.vs.californium.CoapResponse;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ConcurrentResourceBase;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class CoAPClientInResourceExample extends ConcurrentResourceBase {

	public CoAPClientInResourceExample(String name) {
		super(name, SINGLE_THREADED);
	}

	@Override
	public void handleGET(final CoapExchange exchange) {
		exchange.accept();
		
		CoapClient client = createClient("localhost:5683/target");
		client.get(new CoapHandler() {
			@Override
			public void onLoad(CoapResponse response) {
				exchange.respond(response.getCode(), response.getPayload());
			}
			
			@Override
			public void onError() {
				exchange.respond(ResponseCode.BAD_GATEWAY);
			}
		});
		
		// exchange has not been responded yet
	}
	
	@Override
	public void handlePOST(CoapExchange exchange) {
		exchange.accept();
	
		ResponseCode response;
		synchronized (this) {
			// critical section
			response = ResponseCode.CHANGED;
		}

		exchange.respond(response);
	}

	public static void main(String[] args) {
		Server server = new Server();
		server.add(new CoAPClientInResourceExample("example"));
		server.add(new ResourceBase("target") {
			@Override
			public void handleGET(CoapExchange exchange) {
				exchange.respond("Target payload");
//				exchange.reject();
			}
		});
		server.start();
	}
}

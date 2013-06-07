package test;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.network.EndpointManager;

public class TestClient {

	public static void main(String[] args) throws Exception {
		
		try {
		Server.initializeLogger();
		System.out.println("start client");
		
		Request request = Request.newGet();
		request.setType(Type.CON);
		request.setURI("coaps://localhost:7777/ress");
		request.setObserve();
		request.send();
		
		Response response;
		
		while (true) {
			response = request.waitForResponse();
			System.out.println("Response: "+response.getPayloadString());
			request.setResponse(null);
		}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

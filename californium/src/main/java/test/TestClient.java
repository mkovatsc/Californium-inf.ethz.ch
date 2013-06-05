package test;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.network.EndpointManager;

public class TestClient {

	public static void main(String[] args) {
		
		try {
		Server.initializeLogger();
		System.out.println("start client");
		
		Request request = Request.newGet();
		request.setType(Type.NON);
		request.setURI("coaps://localhost:7777/ress");
		EndpointManager.getEndpointManager().getDefaultDtlsEndpoint().sendRequest(request);
		
		Response response = request.waitForResponse(1000);
		System.out.println("Response: "+response.getPayloadString());
		
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

package test;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

public class TestClient {

	public static void main(String[] args) throws Exception {
		
		Server.initializeLogger();
		System.out.println("start client");
		
		final String uri = "coaps://localhost:7777/ress";
		
		System.out.println("Send request 1 with observe");
		Request request = Request.newGet();
		request.setType(Type.CON);
		request.setURI(uri);
		request.setObserve();
		request.send();
		
		Response response;
		for (int i=0;i<3;i++) {
			response = request.waitForResponse();
			System.out.println("Response for 1: "+response.getPayloadString());
			request.setResponse(null);
		}
		System.exit(0);
		
	}
}

package ch.inf.vs.californium.debug;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.Request;

public class TestClient {

	public static void main(String[] args) {
		try {
			Request request = new Request(Code.POST);
			request.setConfirmable(true);
			request.setURI("localhost:7777/eins/zwei/drei?a=b&c=d");
			request.setPayload("huhu");
			System.out.println("  Client wants to send request "+request);
		
			request.send();
			
			String response = request.waitForResponse().getPayloadString();
			System.out.println("  Response: "+response);
			
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

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
		
		final String uri = "coaps://localhost:7777/ress";
		
		new Thread() {
			public void run() {
				System.out.println("Send request 1 with observe");
				Request request = Request.newGet();
				request.setType(Type.CON);
				request.setURI(uri);
				request.setObserve();
				request.send();
				
				try {
					Response response;
					while (true) {
						response = request.waitForResponse();
						System.out.println("Response for 1: "+response.getPayloadString());
						request.setResponse(null);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		Thread.sleep(3000);
		
		new Thread() {
			public void run() {
				try {
					System.out.println("Send request 2 with observe");
					Request r2 = Request.newGet();
					r2.setType(Type.CON);
					r2.setURI(uri);
					r2.setObserve();
					r2.send();
					
					Response response;
					while (true) {
						response = r2.waitForResponse();
						System.out.println("Response for 2: "+response.getPayloadString());
						r2.setResponse(null);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		Thread.sleep(3000);
		
		new Thread() {
			public void run() {
				try {
					System.out.println("Send request 3 without observe");
					Request r3 = Request.newGet();
					r3.setType(Type.CON);
					r3.setURI(uri);
					r3.send();
					
					Response response;
					while (true) {
						response = r3.waitForResponse();
						System.out.println("Response for 3: "+response.getPayloadString());
						r3.setResponse(null);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		Thread.sleep(3000);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}

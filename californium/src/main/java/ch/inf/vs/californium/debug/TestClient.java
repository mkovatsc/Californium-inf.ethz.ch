package ch.inf.vs.californium.debug;

import java.net.InetAddress;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.Request;

public class TestClient {

	public static void main(String[] args) {
		Server.initializeLogger();
		try {
			Request request = new Request(Code.POST);
			request.setConfirmable(true);
			request.setDestination(InetAddress.getLocalHost());
			request.setDestinationPort(7777);
			request.setPayload("This POST request is ABCDEFGHIJKLMNOPQRSTUVQXYZ ABCDEFGHIJKLMNOPQRSTUVQXYZ ABCDEFGHIJKLMNOPQRSTUVQXYZ ABCDEFGHIJKLMNOPQRSTUVQXYZ".getBytes());
			System.out.println("  Client wants to send request "+request);
		
			request.send();
			
			Thread.sleep(2000);
			
			String response = request.waitForResponse().getPayloadString();
			System.out.println("  Response: "+response);
			
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

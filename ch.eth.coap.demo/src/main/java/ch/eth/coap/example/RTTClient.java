package ch.eth.coap.example;

import java.io.IOException;

import ch.eth.coap.coap.GETRequest;
import ch.eth.coap.coap.Request;
import ch.eth.coap.coap.Response;

public class RTTClient {

	/*
	 * Main method of this client.
	 */
	public static void main(String[] args) {
		
		int n = 1000;
		int total = 0;
		
		for (int i = 0; i < n; i++) {
		
			Request request = new GETRequest();
			request.enableResponseQueue(true);
			
			request.setURI("coap://localhost/timeResource");
			try {
				request.execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
			try {
				Response response = request.receiveResponse();
				if (response != null) {
					if (response.getRTT() < 0) {
						System.out.println("Response received, RTT=" + response.getRTT());
					}
					total += response.getRTT();
				} else {
					System.out.println("No response received");
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.printf("Average RTT over %d rounds: %f ms\n", n, (double)total/n);
	}
	
}

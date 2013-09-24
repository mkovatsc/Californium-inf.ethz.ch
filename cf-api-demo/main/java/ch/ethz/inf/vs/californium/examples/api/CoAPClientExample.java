package ch.ethz.inf.vs.californium.examples.api;

import ch.ethz.inf.vs.californium.CoapClient;
import ch.ethz.inf.vs.californium.CoapHandler;
import ch.ethz.inf.vs.californium.CoapObserveRelation;
import ch.ethz.inf.vs.californium.CoapResponse;

public class CoAPClientExample {

	public static void main(String[] args) {
		
		// synchronous
		
		CoapClient client = new CoapClient("coap://localhost:5683/hello");
//		String content = client.get().getPayloadString();
		String content = client.post("payload").getResponseText();
		
		
		// asynchronous
		client.get(new CoapHandler() {
			@Override public void responded(CoapResponse response) {
				String content = response.getResponseText();
				System.out.println(content);
			}
			
			@Override public void failed() {
				System.err.println("Failed");
			}
		});
		
		CoapObserveRelation relation = client.observe(
				new CoapHandler() {
					@Override public void responded(CoapResponse response) {
						String content = response.getResponseText();
						System.out.println(content);
					}
					
					@Override public void failed() {
						System.err.println("Failed");
					}
				});
		
		relation.cancel();
		
		System.out.println(content);
	}
	
}

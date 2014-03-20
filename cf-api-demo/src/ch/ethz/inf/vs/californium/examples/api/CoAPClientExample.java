package ch.ethz.inf.vs.californium.examples.api;

import ch.ethz.inf.vs.californium.CoapClient;
import ch.ethz.inf.vs.californium.CoapHandler;
import ch.ethz.inf.vs.californium.CoapObserveRelation;
import ch.ethz.inf.vs.californium.CoapResponse;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;

public class CoAPClientExample {

	public static void main(String[] args) {
		
		CoapClient client = new CoapClient("coap://localhost:5683/hello");
		
		// synchronous
		String content1 = client.get().getResponseText();
		System.out.println(content1);
		String content2 = client.post("payload", MediaTypeRegistry.TEXT_PLAIN).getResponseText();
		System.out.println(content2);
		
		// asynchronous
		client.get(new CoapHandler() {
			@Override public void onLoad(CoapResponse response) {
				String content = response.getResponseText();
				System.out.println(content);
			}
			
			@Override public void onError() {
				System.err.println("Failed");
			}
		});
		
		// observing
		CoapObserveRelation relation = client.observe(
				new CoapHandler() {
					@Override public void onLoad(CoapResponse response) {
						String content = response.getResponseText();
						System.out.println(content);
					}
					
					@Override public void onError() {
						System.err.println("Failed");
					}
				});
		relation.proactiveCancel();
	}
	
}

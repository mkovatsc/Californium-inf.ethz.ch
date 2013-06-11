package test;

import org.junit.Test;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.AbstractResource;

public class TestBoth {

	private static int counter = 0;
	
	@Test
	public void test() throws Exception {
		System.out.println("start client and server");
		
		Server.initializeLogger();
		Server server = new Server(7777);
		server.add(new AbstractResource("ress") {
			public void processGET(Exchange exchange) {
				exchange.respond("response "+counter++);
			}
		});
		server.start();
		
		
		for (int i=0;i<50;i++) {
			Thread.sleep(100);
			Request request = Request.newGet();
			request.setURI("localhost:7777/ress");
			request.send();
			Response response = request.waitForResponse(1000);
			if (response == null) {
				System.out.println("ERROR");
				break;
			} else {
				System.out.println(response.getPayloadString());
			}
		}
		
		Thread.sleep(10000);
		System.exit(0);
	}
	
}

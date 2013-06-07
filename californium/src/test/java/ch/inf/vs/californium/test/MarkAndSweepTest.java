package ch.inf.vs.californium.test;

import java.util.Random;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.AbstractResource;

/**
 * This is not a JUnit test
 */
public class MarkAndSweepTest {

	public static final int SERVER_PORT = 7777;
	public static final String TARGET = "test";
	
	private static Server server;
	
	public static void main(String[] args) throws Exception {
		Server.initializeLogger();
		server = new Server(SERVER_PORT);
		server.add(new AbstractResource(TARGET) {
			public void processRequest(Exchange exchange) {
				exchange.accept();
				exchange.respond("Hello");
			}
		});
		server.start();
		
		Random random = new Random();
		for (int i=0; i<100; i++) {
			
			Request request = new Request(Code.GET);
			request.setURI("localhost:"+SERVER_PORT+"/"+TARGET);
			request.send();
			
			Thread.sleep(100+random.nextInt(1000));
		}
		
		// Matcher's Mark-And-Sweep should now clear all hash maps.
	}
	
}

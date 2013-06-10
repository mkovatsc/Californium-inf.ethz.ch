package test;

import java.util.Arrays;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.AbstractResource;

public class TestServer {
	
	public static void main(String[] args) throws Exception {
		Server.initializeLogger();
		System.out.println("start server");
		Server server = new Server(7777);
		server.add(new MyResource());
		server.start();
	}
	
	private static class MyResource extends AbstractResource {
		
		private int value = 77;
		
		public MyResource() {
			super("ress");
			setObservable(true);
			new Thread() {
				public void run() {
					try {
						while (true) {
							Thread.sleep(1000);
							value++;
							changed();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
		
		public void processGET(Exchange exchange) {
			String tok = Arrays.toString(exchange.getRequest().getToken());
			exchange.respond("hi, this is "+value+" but I change. You had tok = "+tok);
		}
	}
	
}

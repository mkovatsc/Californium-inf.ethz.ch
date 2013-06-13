package test;

import java.util.Arrays;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.resources.ResourceBase;

public class TestServer {
	
	public static void main(String[] args) throws Exception {
		System.out.println("start server");
		Server server = new Server(7777);
		server.add(new MyResource());
		server.start();
	}
	
	private static class MyResource extends ResourceBase {
		
		private int value = 77;
		private int myMID = 1;
		
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
			exchange.accept();
			String tok = Arrays.toString(exchange.getRequest().getToken());
			Response response = new Response(ResponseCode.CONTENT);
			response.setPayload("hi, this is "+value+" but I change. You had tok = "+tok);
			response.setMid(myMID++);
			exchange.respond(response);
		}
	}
	
}

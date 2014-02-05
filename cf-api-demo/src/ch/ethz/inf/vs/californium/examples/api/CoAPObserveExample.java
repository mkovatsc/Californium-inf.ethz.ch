package ch.ethz.inf.vs.californium.examples.api;

import static ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode.*;

import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.server.Server;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class CoAPObserveExample extends ResourceBase {

	public CoAPObserveExample(String name) {
		super(name);
		setObservable(true); // enable observing
		setObserveType(Type.CON); // configure the notification type to CONs
		getAttributes().setObservable(); // mark observable in the Link-Format
		
		// schedule a periodic update task, otherwise let events call changed()
		Timer timer = new Timer();
		timer.schedule(new UpdateTask(), 0, 1000);
	}
	
	private class UpdateTask extends TimerTask {
		@Override
		public void run() {
			// .. periodic update of the resource
			changed(); // notify all observers
		}
	}
	
	@Override
	public void handleGET(CoapExchange exchange) {
		exchange.setMaxAge(1); // the Max-Age value should match the update interval
		exchange.respond("update");
	}
	
	@Override
	public void handleDELETE(CoapExchange exchange) {
		delete(); // will also call clearAndNotifyObserveRelations(ResponseCode.NOT_FOUND)
		exchange.respond(DELETED);
	}
	
	@Override
	public void handlePUT(CoapExchange exchange) {
		// ...
		exchange.respond(CHANGED);
		changed(); // notify all observers
	}
	
	public static void main(String[] args) {
		Server server = new Server();
		server.add(new CoAPObserveExample("hello"));
		server.start();
	}

}

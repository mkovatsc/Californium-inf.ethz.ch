package ch.inf.vs.californium.network.layer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;

public interface Layer {

	public void sendRequest(Exchange exchange, Request request);
	
	public void sendResponse(Exchange exchange, Response response);
	
	public void sendEmptyMessage(Exchange exchange, EmptyMessage emptyMessage);
	
	
	public void receiveRequest(Exchange exchange, Request request);
	
	public void receiveResponse(Exchange exchange, Response response);
	
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message);
	
	
	public void setLowerLayer(Layer layer);
	
	public void setUpperLayer(Layer layer);
	
	public void setExecutor(ScheduledExecutorService executor);
	
	
	public static class TopDownBuilder {
		
		private LinkedList<Layer> stack = new LinkedList<>();
		
		public TopDownBuilder add(Layer layer) {
			if (stack.size() > 0)
				stack.getLast().setLowerLayer(layer);
			stack.add(layer);
			return this;
		}
		
		public List<Layer> create() {
			return stack;
		}
		
	}
	
	public static class BottomUpBuilder {
		
		private LinkedList<Layer> stack = new LinkedList<>();
		
		public BottomUpBuilder add(Layer layer) {
			stack.getLast().setUpperLayer(layer);
			return this;
		}
		
		public List<Layer> create() {
			return stack;
		}
	}
	
}

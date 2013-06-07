package ch.inf.vs.californium.network.layer;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import ch.inf.vs.californium.MessageDeliverer;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.Exchange;
import ch.inf.vs.californium.network.NetworkConfig;
import ch.inf.vs.californium.network.StackBottom;
import ch.inf.vs.californium.network.Exchange.Origin;

public class CoapStack {

	final static Logger logger = Logger.getLogger(CoapStack.class.getName());

	private List<Layer> layers;
	private StackBottom handler;

	private StackTopAdapter top;
	private StackBottomAdapter bottom;

	private Endpoint endpoint;
	private MessageDeliverer deliverer;
	
	public CoapStack(Endpoint endpoint, NetworkConfig config, StackBottom handler) {
		this.endpoint = endpoint;
		this.top = new StackTopAdapter();
		this.handler = handler;
		this.layers = 
				new Layer.TopDownBuilder()
				.add(top)
				.add(new TokenLayer())
				.add(new ObserveLayer())
				.add(new BlockwiseLayer(config))
				.add(new ReliabilityLayer(config))
				.add(bottom = new StackBottomAdapter())
				.create();
	}
	
	// delegate to top
	public void sendRequest(Request request) {
		top.sendRequest(request);
	}

	// delegate to top
	public void sendResponse(Exchange exchange, Response response) {
		top.sendResponse(exchange, response);
	}

	// delegate to top
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		top.sendEmptyMessage(exchange, message);
	}

	// delegate to bottom
	public void receiveRequest(Exchange exchange, Request request) {
		bottom.receiveRequest(exchange, request);
	}

	// delegate to bottom
	public void receiveResponse(Exchange exchange, Response response) {
		bottom.receiveResponse(exchange, response);
	}

	// delegate to bottom
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		bottom.receiveEmptyMessage(exchange, message);
	}

	public void setExecutor(ScheduledExecutorService executor) {
		for (Layer layer:layers)
			layer.setExecutor(executor);
	}
	
	public void setDeliverer(MessageDeliverer deliverer) {
		this.deliverer = deliverer;
	}
	
	private class StackTopAdapter extends AbstractLayer {
		
		public void sendRequest(Request request) {
			Exchange exchange = new Exchange(request, Origin.LOCAL);
			sendRequest(exchange, request); // layer method
		}
		
		@Override
		public void sendRequest(Exchange exchange, Request request) {
			assert(exchange == null);
			exchange.setRequest(request);
			super.sendRequest(exchange, request);
		}
		
		@Override
		public void sendResponse(Exchange exchange, Response response) {
			exchange.setResponse(response);
			super.sendResponse(exchange, response);
		}
		
		@Override
		public void receiveRequest(Exchange exchange, Request request) {
			if (exchange.getRequest() == null)
				throw new NullPointerException("Final assembled request of exchange must not be null");
			exchange.setEndpoint(endpoint);
			if (deliverer != null) {
//				logger.info("Top of CoAP stack delivers request");
				deliverer.deliverRequest(exchange);
			} else {
				logger.severe("Top of CoAP stack has no deliverer to deliver request");
			}
		}

		@Override
		public void receiveResponse(Exchange exchange, Response response) {
			exchange.setEndpoint(endpoint);
			if (deliverer != null) {
				logger.info("Top of CoAP stack delivers response");
				deliverer.deliverResponse(exchange, response); // notify request that response has arrived
			} else {
				logger.severe("Top of CoAP stack has no deliverer to deliver response");
			}
		}
		
		@Override
		public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
			ignore(message);
		}
	}
	
	private class StackBottomAdapter extends AbstractLayer {
	
		@Override
		public void sendRequest(Exchange exchange, Request request) {
			handler.sendRequest(exchange, request);
		}

		@Override
		public void sendResponse(Exchange exchange, Response response) {
			handler.sendResponse(exchange, response);
		}

		@Override
		public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
			handler.sendEmptyMessage(exchange, message);
		}
		
	}
}

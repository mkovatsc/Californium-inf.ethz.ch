package ch.inf.vs.californium.network;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import ch.inf.vs.californium.MessageDeliverer;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.layer.AbstractLayer;
import ch.inf.vs.californium.network.layer.BlockwiseLayer;
import ch.inf.vs.californium.network.layer.Layer;
import ch.inf.vs.californium.network.layer.ReliabilityLayer;
import ch.inf.vs.californium.network.layer.TokenLayer;

public class CoapStack {

	final static Logger logger = Logger.getLogger(CoapStack.class.getName());

	private List<Layer> layers;
	private ParsingLayer parser;
	private RawDataChannel channel;

	private StackTopAdapter top;
	private StackBottomAdapter bottom;

	private Endpoint endpoint;
	private MessageDeliverer deliverer;
	
	public CoapStack(Endpoint endpoint, StackConfiguration config, RawDataChannel channel) {
		this.endpoint = endpoint;
		this.channel = channel;
		this.top = new StackTopAdapter();
		this.bottom = new StackBottomAdapter();
		this.layers = 
				new Layer.TopDownBuilder()
				.add(top)
				.add(new TokenLayer())
				.add(new BlockwiseLayer(config))
				.add(new ReliabilityLayer(config))
				.add(new MatchingLayer())
				.add(new InterceptorLayer())
				.add(parser = new ParsingLayer())
				.add(bottom)
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
	
	// delegate to bottome
	public void receiveData(RawData raw) {
		bottom.receiveData(raw);
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
			Exchange exchange = new Exchange(request, true);
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
				logger.info("Top of CoAP stack delivers request");
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
	
	private class StackBottomAdapter extends AbstractLayer implements RawDataChannel {
	
		@Override
		public void sendRequest(Exchange exchange, Request request) {
			logger.info("==> send request "+request);
			RawData raw = new RawData(request.getBytes());
			raw.setAddress(request.getDestination());
			raw.setPort(request.getDestinationPort());
			sendData(raw);
		}

		@Override
		public void sendResponse(Exchange exchange, Response response) {
			logger.info("==> send response "+response);
			RawData raw = new RawData(response.getBytes());
			raw.setAddress(response.getDestination());
			raw.setPort(response.getDestinationPort());
			sendData(raw);
		}

		@Override
		public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
			logger.info("==> send empty message "+message);
			RawData raw = new RawData(message.getBytes());
			raw.setAddress(message.getDestination());
			raw.setPort(message.getDestinationPort());
			sendData(raw);
		}
		
		@Override // delegate
		public void receiveData(RawData msg) {
			parser.receiveData(msg);
		}
	
		@Override // delegate
		public void sendData(RawData msg) {
			channel.sendData(msg);
		}
	}
	
	private class InterceptorLayer extends AbstractLayer {
		
		@Override
		public void sendRequest(Exchange exchange, Request request) {
			for (MessageInterceptor interceptor:endpoint.getInterceptors())
				interceptor.sendRequest(request);
			super.sendRequest(exchange, request);
		}

		@Override
		public void sendResponse(Exchange exchange, Response response) {
			for (MessageInterceptor interceptor:endpoint.getInterceptors())
				interceptor.sendResponse(response);
			super.sendResponse(exchange, response);
		}

		@Override
		public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
			for (MessageInterceptor interceptor:endpoint.getInterceptors())
				interceptor.sendEmptyMessage(message);
			super.sendEmptyMessage(exchange, message);
		}

		@Override
		public void receiveRequest(Exchange exchange, Request request) {
			for (MessageInterceptor interceptor:endpoint.getInterceptors())
				interceptor.receiveRequest(request);
			super.receiveRequest(exchange, request);
		}

		@Override
		public void receiveResponse(Exchange exchange, Response response) {
			for (MessageInterceptor interceptor:endpoint.getInterceptors())
				interceptor.receiveResponse(response);
			super.receiveResponse(exchange, response);
		}

		@Override
		public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
			for (MessageInterceptor interceptor:endpoint.getInterceptors())
				interceptor.receiveEmptyMessage(message);
			super.receiveEmptyMessage(exchange, message);
		}
		
	}
}

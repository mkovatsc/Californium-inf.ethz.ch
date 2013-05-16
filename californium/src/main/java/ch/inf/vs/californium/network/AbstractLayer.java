package ch.inf.vs.californium.network;

import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Message;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

public abstract class AbstractLayer implements Layer {

	private final static Logger logger = Logger.getLogger(AbstractLayer.class.getName());
	
	/*
	 * Idea: If we make it sendReq(Exch, Req), we ensure, that subclass calls
	 * correct send method of superclass, since sendRes(Exch, Req) would not
	 * work.
	 */
	
	private Layer upperLayer;
	private Layer lowerLayer;
	
	protected ScheduledExecutorService executor;
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		logger.info(getClass().getSimpleName()+": send request "+request);
		if (lowerLayer != null)
			lowerLayer.sendRequest(exchange, request);
		else logger.severe("No lower layer found to send request "+exchange);
	}

	@Override
	public void sendResponse(Exchange exchange, Response response) {
		logger.info(getClass().getSimpleName()+": send response "+response);
		if (lowerLayer != null)
			lowerLayer.sendResponse(exchange, response);
		else logger.severe("No lower layer found to send response "+exchange);
	}

	@Override
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		logger.info(getClass().getSimpleName()+": send empty message");
		if (lowerLayer != null)
			lowerLayer.sendEmptyMessage(exchange, message);
		else logger.severe("No lower layer found to send empty message "+message+" for exchange "+exchange);
	}

	@Override
	public void receiveRequest(Exchange exchange, Request request) {
		logger.info(getClass().getSimpleName()+": receive request");
		if (upperLayer != null)
			upperLayer.receiveRequest(exchange, request);
		else logger.severe("No upper layer found to receive request "+request+" for exchange "+exchange);
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		if (upperLayer != null)
			upperLayer.receiveResponse(exchange, response);
		else logger.severe("No upper layer found to receive response "+response+" for exchange "+exchange);
	}

	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		if (upperLayer != null)
			upperLayer.receiveEmptyMessage(exchange, message);
		else logger.severe("No upper layer found to receive empty message "+message+" for exchange "+exchange);
	}

	@Override
	public void setLowerLayer(Layer layer) {
		if (lowerLayer != layer) {
			if (lowerLayer != null)
				lowerLayer.setUpperLayer(null);
			lowerLayer = layer;
			lowerLayer.setUpperLayer(this);
		}
	}

	@Override
	public void setUpperLayer(Layer layer) {
		if (upperLayer != layer) {
			if (upperLayer != null)
				upperLayer.setLowerLayer(null);
			upperLayer = layer;
			upperLayer.setLowerLayer(this);
		}
	}

	@Override
	public void setExecutor(ScheduledExecutorService executor) {
		this.executor = executor;
	}
	
	public void ignore(Message message) {
		message.setIgnored(true);
	}
	
	public void reject(Exchange exchange, Message message) {
		sendEmptyMessage(exchange, EmptyMessage.newRST(message));
	}
}

package ch.inf.vs.californium.network.layer;

import java.util.logging.Logger;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;

public class ObserveLayer extends AbstractLayer {

	/** The logger. */
	private final static Logger logger = Logger.getLogger(ObserveLayer.class.getName());
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		super.sendRequest(exchange, request);
	}

	@Override
	public void sendResponse(Exchange exchange, Response response) {
		super.sendResponse(exchange, response);
	}

	@Override
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		super.sendEmptyMessage(exchange, message);
	}

	@Override
	public void receiveRequest(Exchange exchange, Request request) {
		super.receiveRequest(exchange, request);
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		super.receiveResponse(exchange, response);
	}

	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		super.receiveEmptyMessage(exchange, message);
	}
	
}

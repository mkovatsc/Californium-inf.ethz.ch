package ch.inf.vs.californium.network.layer;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.Exchange;

/**
 * Doesn't do much yet except for setting a simple token... (TODO)
 */
public class TokenLayer extends AbstractLayer {

	private byte current = 1; // TODO: make better
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		exchange.getCurrentRequest().setToken(new byte[] {current++});
		if (exchange.getCurrentRequest().getToken() == null)
			throw new NullPointerException("Sending request's token cannot be null");
		super.sendRequest(exchange, request);
	}

	@Override
	public void sendResponse(Exchange exchange, Response response) {
		response.setToken(exchange.getRequest().getToken());
		if (response.getToken() == null)
			throw new NullPointerException("Sending response's token cannot be null");
		super.sendResponse(exchange, response);
	}

	@Override
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		super.sendEmptyMessage(exchange, message);
	}

	@Override
	public void receiveRequest(Exchange exchange, Request request) {
		if (exchange.getCurrentRequest().getToken() == null)
			throw new NullPointerException("Received requests's token cannot be null");
		super.receiveRequest(exchange, request);
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		if (response.getToken() == null)
			throw new NullPointerException("Received response's token cannot be null");
		super.receiveResponse(exchange, response);
	}

	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		super.receiveEmptyMessage(exchange, message);
	}
}

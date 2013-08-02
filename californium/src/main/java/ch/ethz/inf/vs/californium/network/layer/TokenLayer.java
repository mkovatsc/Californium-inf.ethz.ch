package ch.ethz.inf.vs.californium.network.layer;

import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;

/**
 * Doesn't do much yet except for setting a simple token... (TODO)
 */
public class TokenLayer extends AbstractLayer {

//	private byte current = 1; // TODO: make better
	private AtomicInteger counter = new AtomicInteger();
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		if (request.getToken() == null)
			request.setToken(createNewToken());
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
	
	private byte[] createNewToken() {
		int token = counter.incrementAndGet();
		return new byte[] { (byte) (token>>>24), (byte) (token>>>16), (byte) (token>>>8), (byte) token}; 
	}
}

package ch.ethz.inf.vs.californium.network.layer;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.inf.vs.californium.Utils;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;

/**
 * Doesn't do much yet except for setting a simple token. Notice that empty
 * tokens must be represented as byte array of length 0 (not null).
 */
public class TokenLayer extends AbstractLayer {
	
	private AtomicInteger counter;
	
	public TokenLayer(NetworkConfig config) {
		if (config.getBoolean(NetworkConfigDefaults.USE_RANDOM_TOKEN_START))
			counter = new AtomicInteger(new Random().nextInt());
		else counter = new AtomicInteger(0);
	}
	
	@Override
	public void sendRequest(Exchange exchange, Request request) {
		if (request.getToken() == null)
			request.setToken(createNewToken());
//		if (exchange.getCurrentRequest().getToken() == null)
//			throw new NullPointerException("Sending request's token cannot be null, use byte[0] for empty tokens");
		super.sendRequest(exchange, request);
	}

	@Override
	public void sendResponse(Exchange exchange, Response response) {
		// A response must have the same token as the request it belongs to. If
		// the token is empty, we must use a byte array of length 0.
		if (response.getToken() == null) {
			LOGGER.fine("Set token from current request: "+Utils.toHexString(exchange.getCurrentRequest().getToken()));
			response.setToken(exchange.getCurrentRequest().getToken());
		}
		super.sendResponse(exchange, response);
	}

	@Override
	public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
		super.sendEmptyMessage(exchange, message);
	}

	@Override
	public void receiveRequest(Exchange exchange, Request request) {
		if (exchange.getCurrentRequest().getToken() == null)
			throw new NullPointerException("Received requests's token cannot be null, use byte[0] for empty tokens");
		super.receiveRequest(exchange, request);
	}

	@Override
	public void receiveResponse(Exchange exchange, Response response) {
		if (response.getToken() == null)
			throw new NullPointerException("Received response's token cannot be null, use byte[0] for empty tokens");
		super.receiveResponse(exchange, response);
	}

	@Override
	public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
		super.receiveEmptyMessage(exchange, message);
	}
	
	/**
	 * Create a new token
	 * @return the new token
	 */
	private byte[] createNewToken() {
		int token = counter.incrementAndGet();
		return new byte[] { (byte) (token>>>24), (byte) (token>>>16), (byte) (token>>>8), (byte) token}; 
	}
}

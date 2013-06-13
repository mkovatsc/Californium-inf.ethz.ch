package ch.inf.vs.californium.network;

import java.util.logging.Logger;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.resources.CalifonriumLogger;

public class MessageLogger implements MessageIntercepter {

	private final static Logger LOGGER = CalifonriumLogger.getLogger(MessageLogger.class);
	
	private final EndpointAddress address;
	
	public MessageLogger(EndpointAddress address) {
		this.address = address;
	}
	
	@Override
	public void sendRequest(Request request) {
		LOGGER.info("<== " + address.getPort() + " send request "+request +
				" to "+request.getDestinationPort());
	}

	@Override
	public void sendResponse(Response response) {
		LOGGER.info("<== " + address.getPort() + " send response "+response +
				" to "+response.getDestinationPort());
	}

	@Override
	public void sendEmptyMessage(EmptyMessage message) {
		LOGGER.info("<== " + address.getPort() + " send empty message "+message +
				" to "+message.getDestinationPort());
	}

	@Override
	public void receiveRequest(Request request) {
		LOGGER.info("==> " + address.getPort() + " receive request "+request);
	}

	@Override
	public void receiveResponse(Response response) {
		LOGGER.info("==> " + address.getPort() + " receive response "+response);
	}

	@Override
	public void receiveEmptyMessage(EmptyMessage message) {
		LOGGER.info("==> " + address.getPort() + " receive empty message "+message);
	}
}

package ch.inf.vs.californium.network;

import java.util.logging.Logger;

import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.resources.CalifonriumLogger;

/**
 * The MessageLogger logs all incoming and outgoing messages. The MessageLogger
 * is used by an {@link Endpoint} and is located between the serializer/parser
 * and the matcher. Each message comes or goes to the connector is logged.
 */
public class MessageLogger implements MessageIntercepter {

	/** The logger. */
	private final static Logger LOGGER = CalifonriumLogger.getLogger(MessageLogger.class);
	
	/** The address of the endpoint. */
	private final EndpointAddress address;
	
	/**
	 * Instantiates a new message logger.
	 *
	 * @param address the address
	 */
	public MessageLogger(EndpointAddress address) {
		this.address = address;
	}
	
	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#sendRequest(ch.inf.vs.californium.coap.Request)
	 */
	@Override
	public void sendRequest(Request request) {
		LOGGER.info("<== " + address.getPort() + " send request "+request +
				" to "+request.getDestinationPort());
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#sendResponse(ch.inf.vs.californium.coap.Response)
	 */
	@Override
	public void sendResponse(Response response) {
		LOGGER.info("<== " + address.getPort() + " send response "+response +
				" to "+response.getDestinationPort());
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#sendEmptyMessage(ch.inf.vs.californium.coap.EmptyMessage)
	 */
	@Override
	public void sendEmptyMessage(EmptyMessage message) {
		LOGGER.info("<== " + address.getPort() + " send empty message "+message +
				" to "+message.getDestinationPort());
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#receiveRequest(ch.inf.vs.californium.coap.Request)
	 */
	@Override
	public void receiveRequest(Request request) {
		LOGGER.info("==> " + address.getPort() + " receive request "+request);
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#receiveResponse(ch.inf.vs.californium.coap.Response)
	 */
	@Override
	public void receiveResponse(Response response) {
		LOGGER.info("==> " + address.getPort() + " receive response "+response);
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#receiveEmptyMessage(ch.inf.vs.californium.coap.EmptyMessage)
	 */
	@Override
	public void receiveEmptyMessage(EmptyMessage message) {
		LOGGER.info("==> " + address.getPort() + " receive empty message "+message);
	}
}

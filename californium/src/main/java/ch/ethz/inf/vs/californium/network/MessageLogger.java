package ch.ethz.inf.vs.californium.network;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

/**
 * The MessageLogger logs all incoming and outgoing messages. The MessageLogger
 * is used by an {@link Endpoint} and is located between the serializer/parser
 * and the matcher. Each message comes or goes to the connector is logged.
 */
public class MessageLogger implements MessageIntercepter {

	/** The logger. */
	private final static Logger LOGGER = CalifonriumLogger.getLogger(MessageLogger.class);
	
	/** The address of the endpoint. */
	private final InetSocketAddress address;
	
	/** The configuration */
	private NetworkConfig config; // TODO: observe config and do not always use getBoolean
	
	// TODO: need to update field address in case it was a wildcard address
	private boolean logEnabled;
	
	/**
	 * Instantiates a new message logger.
	 *
	 * @param address the address
	 */
	public MessageLogger(InetSocketAddress address, NetworkConfig config) {
		this.address = address;
		this.config = config;
		this.logEnabled = config.getBoolean(NetworkConfigDefaults.LOG_MESSAGES);
	}
	
	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#sendRequest(ch.inf.vs.californium.coap.Request)
	 */
	@Override
	public void sendRequest(Request request) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s ==> (%s:%d) send request %s",
				address, request.getDestination(), request.getDestinationPort(), request));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#sendResponse(ch.inf.vs.californium.coap.Response)
	 */
	@Override
	public void sendResponse(Response response) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s ==> (%s:%d) send response %s",
				address, response.getDestination(), response.getDestinationPort(), response));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#sendEmptyMessage(ch.inf.vs.californium.coap.EmptyMessage)
	 */
	@Override
	public void sendEmptyMessage(EmptyMessage message) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s ==> (%s:%d) send empty message %s",
				address, message.getDestination(), message.getDestinationPort(), message));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#receiveRequest(ch.inf.vs.californium.coap.Request)
	 */
	@Override
	public void receiveRequest(Request request) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s <== (%s:%d) receive request %s",
				address, request.getSource(), request.getSourcePort(), request));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#receiveResponse(ch.inf.vs.californium.coap.Response)
	 */
	@Override
	public void receiveResponse(Response response) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s <== (%s:%d) receive response %s",
				address, response.getSource(), response.getSourcePort(), response));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#receiveEmptyMessage(ch.inf.vs.californium.coap.EmptyMessage)
	 */
	@Override
	public void receiveEmptyMessage(EmptyMessage message) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s <== (%s:%d) receive empty message %s",
				address, message.getSource(), message.getSourcePort(), message));
	}
	
}

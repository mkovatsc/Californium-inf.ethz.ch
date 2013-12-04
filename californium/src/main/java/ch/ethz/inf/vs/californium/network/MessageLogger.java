package ch.ethz.inf.vs.californium.network;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.network.config.NetworkConfigObserverAdapter;

/**
 * The MessageLogger logs all incoming and outgoing messages. The MessageLogger
 * is used by an {@link CoAPEndpoint} and is located between the serializer/parser
 * and the matcher. Each message comes or goes to the connector is logged.
 */
public class MessageLogger implements MessageIntercepter {

	/** The logger. */
	private final static Logger LOGGER = Logger.getLogger(MessageLogger.class.getCanonicalName());
	
	/** The address of the endpoint. */
	private final InetSocketAddress address;
	
	/** Indicates if the logger should log messages */
	private boolean logEnabled;
	
	/**
	 * Instantiates a new message logger.
	 *
	 * @param address the address
	 */
	public MessageLogger(InetSocketAddress address, NetworkConfig config) {
		this.address = address;
		this.logEnabled = config.getBoolean(NetworkConfigDefaults.LOG_MESSAGES);
		
		// Observe the configuration. If LOG_MESSAGES is changed, also change it here.
		config.addConfigObserver(new NetworkConfigObserverAdapter() {
			@Override
			public void changed(String key, boolean value) {
				System.out.println("changed key: "+key+" to "+value);
				if (NetworkConfigDefaults.LOG_MESSAGES.equals(key)) {
					logEnabled = value;
				}
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#sendRequest(ch.inf.vs.californium.coap.Request)
	 */
	@Override
	public void sendRequest(Request request) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s ==> %s:%d req %s",
				address, request.getDestination(), request.getDestinationPort(), request));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#sendResponse(ch.inf.vs.californium.coap.Response)
	 */
	@Override
	public void sendResponse(Response response) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s ==> %s:%d res %s",
				address, response.getDestination(), response.getDestinationPort(), response));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#sendEmptyMessage(ch.inf.vs.californium.coap.EmptyMessage)
	 */
	@Override
	public void sendEmptyMessage(EmptyMessage message) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s ==> %s:%d emp %s",
				address, message.getDestination(), message.getDestinationPort(), message));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#receiveRequest(ch.inf.vs.californium.coap.Request)
	 */
	@Override
	public void receiveRequest(Request request) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s <== %s:%d req %s",
				address, request.getSource(), request.getSourcePort(), request));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#receiveResponse(ch.inf.vs.californium.coap.Response)
	 */
	@Override
	public void receiveResponse(Response response) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s <== %s:%d res %s",
				address, response.getSource(), response.getSourcePort(), response));
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.MessageIntercepter#receiveEmptyMessage(ch.inf.vs.californium.coap.EmptyMessage)
	 */
	@Override
	public void receiveEmptyMessage(EmptyMessage message) {
		if (logEnabled)
			LOGGER.info(String.format("%-15s <== %s:%d emp %s",
				address, message.getSource(), message.getSourcePort(), message));
	}
	
}

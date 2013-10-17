package ch.ethz.inf.vs.californium.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.layer.BlockwiseLayer;
import ch.ethz.inf.vs.californium.network.layer.CoapStack;
import ch.ethz.inf.vs.californium.network.layer.ExchangeForwarder;
import ch.ethz.inf.vs.californium.network.layer.ObserveLayer;
import ch.ethz.inf.vs.californium.network.layer.ReliabilityLayer;
import ch.ethz.inf.vs.californium.network.layer.TokenLayer;
import ch.ethz.inf.vs.californium.network.serializer.DataParser;
import ch.ethz.inf.vs.californium.network.serializer.Serializer;
import ch.ethz.inf.vs.californium.server.MessageDeliverer;
import ch.ethz.inf.vs.elements.Connector;
import ch.ethz.inf.vs.elements.RawData;
import ch.ethz.inf.vs.elements.RawDataChannel;
import ch.ethz.inf.vs.elements.UDPConnector;

/**
 * Endpoint encapsulates the stack that executes the CoAP protocol. Endpoint
 * forwards incoming messages to a {@link MessageDeliverer}. The deliverer will
 * deliver requests to its destination resource. The resource sends the response
 * back over the same endpoint. The endpoint sends outgoing messages over a
 * connector. The connector encapsulates the transport protocol.
 * <p>
 * The CoAP Draft 18 describes an endpoint as: "A CoAP Endpoint is is identified
 * by transport layer multiplexing information that can include a UDP port
 * number and a security association." (draft-ietf-core-coap-14: 1.2)
 * <p>
 * The following diagram describes the structure of an endpoint. The endpoint
 * implements CoAP in layers. Incoming and outgoing messages always travel from
 * layer to layer. An {@link Exchange} represents the known state about the
 * exchange between a request and one or more corresponding responses. The
 * matcher remembers outgoing messages and matches incoming responses, acks and
 * rsts to them. MessageInterceptors receive every incoming and outgoing
 * message. By default, only one interceptor is used to log messages.
 * 
 * <pre>
 * +-----------------------+
 * |   {@link MessageDeliverer}    +--> (Resource Tree)
 * +-------------A---------+
 *               |
 *             * A            
 * +-Endpoint--+-A---------+
 * |           v A         |  
 * |           v A         |  
 * | +---------v-+-------+ |  
 * | | Stack Top         | |  
 * | +-------------------+ |  
 * | | {@link TokenLayer}        | |
 * | +-------------------+ |  
 * | | {@link ObserveLayer}      | |
 * | +-------------------+ |  
 * | | {@link BlockwiseLayer}    | |
 * | +-------------------+ |  
 * | | {@link ReliabilityLayer}  | |
 * | +-------------------+ |  
 * | | Stack Bottom      | |  
 * | +--------+-+--------+ |  
 * |          v A          |  
 * |          v A          |  
 * |        {@link Matcher}        |
 * |          v A          |  
 * |   {@link MessageInterceptor}  |  
 * |          v A          |  
 * |          v A          |  
 * | +--------v-+--------+ |  
 * +-|     {@link Connector}     |-+
 *   +--------+-A--------+    
 *            v A             
 *            v A             
 *         (Network)
 * </pre>
 * <p>
 * The endpoint and its layers use an {@link ScheduledExecutorService} to
 * execute tasks, e.g., when a request arrives.
 */
public class Endpoint {
	
	/** the logger. */
	private final static Logger LOGGER = CalifonriumLogger.getLogger(Endpoint.class);
	
	/** The stack of layers that make up the CoAP protocol */
	private final CoapStack coapstack;
	
	/** The connector over which the endpoint connects to the network */
	private final Connector connector;
	
	/** The configuration of this endpoint */
	private final NetworkConfig config;
	
	/** The executor to run tasks for this endpoint and its layers */
	private ScheduledExecutorService executor;
	
	/** Indicates if the endpoint has been started */
	private boolean started;
	
	/** THe list of endpoint observers (has nothing to do with CoAP observe relations) */
	private List<EndpointObserver> observers = new ArrayList<EndpointObserver>(0);
	
	/** The list of interceptors */
	private List<MessageIntercepter> interceptors = new ArrayList<MessageIntercepter>(0);

	/** The matcher which matches incoming responses, akcs and rsts an exchange */
	private Matcher matcher;
	
	/** The serializer to serialize messages to bytes */
	private Serializer serializer;
	
	/**
	 * Instantiates a new endpoint.
	 */
	public Endpoint() {
		this(0);
	}
	
	/**
	 * Instantiates a new endpoint with the specified port
	 *
	 * @param port the port
	 */
	public Endpoint(int port) {
		this(new InetSocketAddress(port));
	}
	
	/**
	 * Instantiates a new endpoint with the specified address.
	 *
	 * @param address the address
	 */
	public Endpoint(InetSocketAddress address) {
		this(address, NetworkConfig.getStandard());
	}
	
	/**
	 * Instantiates a new endpoint with the specified address and configuration.
	 *
	 * @param address the address
	 * @param config the configuration
	 */
	public Endpoint(InetSocketAddress address, NetworkConfig config) {
		this(createUDPConnector(address, config), config);
	}
	
	/**
	 * Instantiates a new endpoint with the specified connector and
	 * configuration.
	 *
	 * @param connector the connector
	 * @param config the config
	 */
	public Endpoint(Connector connector, NetworkConfig config) {
		this.config = config;
		this.connector = connector;
		this.serializer = new Serializer();
		
		ExchangeForwarder forwarder = new ExchangeForwarderImpl();
		this.matcher = new Matcher(forwarder, config);		
		this.coapstack = new CoapStack(config, forwarder);

		this.interceptors.add(new MessageLogger(connector.getAddress(), config));

		// connector delivers bytes to CoAP stack
		connector.setRawDataReceiver(new RawDataChannelImpl()); 
	}
	
	/**
	 * Creates a new UDP connector.
	 *
	 * @param address the address
	 * @param config the configuration
	 * @return the connector
	 */
	private static Connector createUDPConnector(InetSocketAddress address, NetworkConfig config) {
		UDPConnector c = new UDPConnector(address);
		c.setReceiverThreadCount(config.getInt(NetworkConfigDefaults.UDP_CONNECTOR_RECEIVER_THREAD_COUNT));
		c.setSenderThreadCount(config.getInt(NetworkConfigDefaults.UDP_CONNECTOR_SENDER_THREAD_COUNT));
		c.setReceiveBufferSize(config.getInt(NetworkConfigDefaults.UDP_CONNECTOR_RECEIVE_BUFFER));
		c.setSendBufferSize(config.getInt(NetworkConfigDefaults.UDP_CONNECTOR_SEND_BUFFER));
		c.setLogPackets(config.getBoolean(NetworkConfigDefaults.UDP_CONNECTOR_LOG_PACKETS));
		c.setReceiverPacketSize(config.getInt(NetworkConfigDefaults.UDP_CONNECTOR_DATAGRAM_SIZE));
		return c;
	}
	
	/**
	 * Start this endpoint and all its components.. The starts its connector. If
	 * no executor has been set yet, the endpoint uses a single-threaded
	 * executor.
	 */
	public synchronized void start() {
		if (started) {
			LOGGER.info("Endpoint for "+getAddress()+" hsa already started");
			return;
		}
		if (executor == null) {
			LOGGER.info("Endpoint "+toString()+" has no executer yet to start. Creates default single-threaded executor.");
			setExecutor(Executors.newSingleThreadScheduledExecutor());
		}
		
		try {
			LOGGER.info("Start endpoint for address "+getAddress());
			started = true;
			matcher.start();
			connector.start();
			EndpointManager.getEndpointManager().registerEndpoint(this);
			for (EndpointObserver obs:observers)
				obs.started(this);
			
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Exception while starting connector "+getAddress(), e);
			stop();
		}
	}
	
	/**
	 * Stop this endpoint and all its components, e.g., the connector. A
	 * stoppped endpoint can be started again.
	 */
	public synchronized void stop() {
		if (!started) {
			LOGGER.info("Endpoint for address "+getAddress()+" is already stopped");
		} else {
			LOGGER.info("Stop endpoint for address "+getAddress());
			started = false;
			EndpointManager.getEndpointManager().unregisterEndpoint(this);
			connector.stop();
			matcher.stop();
			for (EndpointObserver obs:observers)
				obs.stopped(this);
			matcher.clear();
		}
	}
	
	/**
	 * Destroys the endpoint and all its components. A destroyed endpoint cannot
	 * be started again.
	 */
	public synchronized void destroy() {
		LOGGER.info("Destroy endpoint for address "+getAddress());
		if (started)
			stop();
		connector.destroy();
		for (EndpointObserver obs:observers)
			obs.destroyed(this);
	}
	
	// Needed for tests: Remove duplicates so that we can reuse port 7777
	public void clear() {
		matcher.clear();
	}
	
	/**
	 * Checks if the endpoint has started.
	 *
	 * @return true, if has started
	 */
	public boolean isStarted() {
		return started;
	}
	
	/**
	 * Sets the executor for this endpoint and all its components.
	 *
	 * @param executor the new executor
	 */
	public synchronized void setExecutor(ScheduledExecutorService executor) {
		this.executor = executor;
		this.coapstack.setExecutor(executor);
		this.matcher.setExecutor(executor);
	}
	
	/**
	 * Adds the observer to the list of observers. This has nothing to do with
	 * CoAP observe relations.
	 * 
	 * @param obs the obs
	 */
	public void addObserver(EndpointObserver obs) {
		observers.add(obs);
	}
	
	/**
	 * Removes the endpoint observer.This has nothing to do with
	 * CoAP observe relations.
	 *
	 * @param obs the obsserver
	 */
	public void removeObserver(EndpointObserver obs) {
		observers.remove(obs);
	}
	
	/**
	 * Adds the interceptor.
	 *
	 * @param interceptor the interceptor
	 */
	public void addInterceptor(MessageIntercepter interceptor) {
		interceptors.add(interceptor);
	}
	
	/**
	 * Removes the interceptor.
	 *
	 * @param interceptor the interceptor
	 */
	public void removeInterceptor(MessageIntercepter interceptor) {
		interceptors.remove(interceptor);
	}
	
	/**
	 * Gets the list of interceptors.
	 *
	 * @return the interceptors
	 */
	public List<MessageIntercepter> getInterceptors() {
		return new ArrayList<MessageIntercepter>(interceptors);
	}
	
	/**
	 * Send the specified request.
	 *
	 * @param request the request
	 */
	public void sendRequest(final Request request) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					coapstack.sendRequest(request);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * Send the specified response.
	 *
	 * @param exchange the exchange
	 * @param response the response
	 */
	public void sendResponse(final Exchange exchange, final Response response) {
		// TODO: If the currently executing thread is not a thread of the
		// executor, a new task on the executor should be created to send the
		// response. (Just uncomment this code)
//		executor.execute(new Runnable() {
//			public void run() {
//				try {
//					coapstack.sendResponse(exchange, response);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
		coapstack.sendResponse(exchange, response);
	}
	
	/**
	 * Send the specified empty message.
	 *
	 * @param exchange the exchange
	 * @param message the message
	 */
	public void sendEmptyMessage(final Exchange exchange, final EmptyMessage message) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					coapstack.sendEmptyMessage(exchange, message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * Sets the message deliverer.
	 *
	 * @param deliverer the new message deliverer
	 */
	public void setMessageDeliverer(MessageDeliverer deliverer) {
		coapstack.setDeliverer(deliverer);
	}
	
	/**
	 * Gets the address this endpoint is associated with.
	 *
	 * @return the address
	 */
	public InetSocketAddress getAddress() {
		return connector.getAddress();
	}

	/**
	 * Gets this endpoint's configuration.
	 *
	 * @return the configuration
	 */
	public NetworkConfig getConfig() {
		return config;
	}

	/**
	 * The stack of layers uses this forwarder to send messages. The forwarder
	 * will then give them to the matcher, the interceptors and finally send
	 * them over the connector.
	 */
	private class ExchangeForwarderImpl implements ExchangeForwarder {
		
		@Override
		public void sendRequest(Exchange exchange, Request request) {
			matcher.sendRequest(exchange, request);
			for (MessageIntercepter interceptor:interceptors)
				interceptor.sendRequest(request);
			if (!request.isCanceled())
				connector.send(serializer.serialize(request));
		}

		@Override
		public void sendResponse(Exchange exchange, Response response) {
			matcher.sendResponse(exchange, response);
			for (MessageIntercepter interceptor:interceptors)
				interceptor.sendResponse(response);
			if (!response.isCanceled())
				connector.send(serializer.serialize(response));
		}

		@Override
		public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
			matcher.sendEmptyMessage(exchange, message);
			for (MessageIntercepter interceptor:interceptors)
				interceptor.sendEmptyMessage(message);
			if (!message.isCanceled())
				connector.send(serializer.serialize(message));
		}
	}
	
	/**
	 * The connector uses this channel to forward messages (in form of
	 * {@link RawData}) to the endpoint. The endpoint creates a new task to
	 * process the message. The task consists of invoking the matcher to look
	 * for an associated Exchange and then forwards the message with the
	 * exchange to the stack of layers.
	 */
	private class RawDataChannelImpl implements RawDataChannel {

		@Override
		public void receiveData(final RawData raw) {
			if (raw.getAddress() == null)
				throw new NullPointerException();
			if (raw.getPort() == 0)
				throw new NullPointerException();
			
			// Create a new task to process this message
			Runnable task = new Runnable() {
				public void run() {
					receiveMessage(raw);
				}
			};
			executeTask(task);
		}
		
		/*
		 * The endpoint's executor executes this method to convert the raw bytes
		 * into a message, look for an associated exchange and forward it to
		 * the stack of layers.
		 */
		private void receiveMessage(RawData raw) {
			DataParser parser = new DataParser(raw.getBytes());
			
			if (parser.isRequest()) {
				// This is a request
				Request request = parser.parseRequest();
				request.setSource(raw.getAddress());
				request.setSourcePort(raw.getPort());
				for (MessageIntercepter interceptor:interceptors)
					interceptor.receiveRequest(request);
				if (!request.isCanceled()) {
					Exchange exchange = matcher.receiveRequest(request);
					if (exchange != null) {
						exchange.setEndpoint(Endpoint.this);
						coapstack.receiveRequest(exchange, request);
					}
				}
				
			} else if (parser.isResponse()) {
				// This is  a response
				Response response = parser.parseResponse();
				response.setSource(raw.getAddress());
				response.setSourcePort(raw.getPort());
				for (MessageIntercepter interceptor:interceptors)
					interceptor.receiveResponse(response);
				if (!response.isCanceled()) {
					Exchange exchange = matcher.receiveResponse(response);
					if (exchange != null) {
						exchange.setEndpoint(Endpoint.this);
						response.setRTT(System.currentTimeMillis() - exchange.getTimestamp());
						coapstack.receiveResponse(exchange, response);
					}
				}
				
			} else { 
				// This is an empty message
				EmptyMessage message = parser.parseEmptyMessage();
				message.setSource(raw.getAddress());
				message.setSourcePort(raw.getPort());
				for (MessageIntercepter interceptor:interceptors)
					interceptor.receiveEmptyMessage(message);
				if (message.getType() == Type.CON
						|| message.getType() == Type.NON) {
					// Reject (ping)
					EmptyMessage rst = EmptyMessage.newRST(message);
					connector.send(serializer.serialize(rst));
				} else if (!message.isCanceled()) {
					Exchange exchange = matcher.receiveEmptyMessage(message);
					if (exchange != null) {
						exchange.setEndpoint(Endpoint.this);
						coapstack.receiveEmptyMessage(exchange, message);
					}
				}
			}
		}

	}
	
	/**
	 * Execute the specified task on the endpoint's executor.
	 *
	 * @param task the task
	 */
	private void executeTask(final Runnable task) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					task.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}
}

package ch.inf.vs.californium.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.MessageDeliverer;
import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

/**
 * A CoAP Endpoint is is identified by transport layer multiplexing information
 * that can include a UDP port number and a security association.
 * (draft-ietf-core-coap-14: 1.2)
 */
public class Endpoint {

	private final static Logger LOGGER = Logger.getLogger(Server.class.getName());
	
	private final EndpointAddress address;
	private final CoapStack coapstack;
	private final Connector connector;
	private final StackConfiguration config;
	
	private ScheduledExecutorService executor;
	private boolean started;
	
	private List<EndpointObserver> observers = new ArrayList<>(0);
	
	public Endpoint() {
		this(0);
	}
	
	public Endpoint(int port) {
		this(null, port);
	}
	
	public Endpoint(int port, StackConfiguration config) {
		this(null, port, config);
	}
	
	public Endpoint(InetAddress address, int port) {
		this(new EndpointAddress(address, port));
	}
	
	public Endpoint(InetAddress address, int port, StackConfiguration config) {
		this(new EndpointAddress(address, port), config);
	}
	
	public Endpoint(EndpointAddress address) {
		this(address, new StackConfiguration());
	}
	
	public Endpoint(EndpointAddress address, StackConfiguration config) {
		this.address = address;
		this.config = config;
		RawDataChannel channel = new RawDataChannelImpl();
		coapstack = new CoapStack(this, config, channel);
		connector = new UDPConnector(address);
		connector.setRawDataReceiver(channel); // connector delivers bytes to CoAP stack
	}
	
	public void start() {
		if (started) {
			LOGGER.info("Endpoint for "+getAddress()+" hsa already started");
			return;
		}
		if (executor == null)
			throw new IllegalStateException("Endpoint "+toString()+" has no executor yet and cannot start");
		
		try {
			LOGGER.info("Start endpoint for address "+getAddress());
			connector.start();
			EndpointManager.getEndpointManager().registerEndpoint(this);
			started = true;
			for (EndpointObserver obs:observers)
				obs.started(this);
			
		} catch (IOException e) {
			stop();
			throw new RuntimeException(e);
		}
	}
	
	public void stop() {
		if (!started) {
			LOGGER.info("Endpoint for address "+getAddress()+" is already stopped");
		} else {
			LOGGER.info("Stop endpoint for address "+getAddress());
			started = false;
			EndpointManager.getEndpointManager().unregisterEndpoint(this);
			connector.stop();
			for (EndpointObserver obs:observers)
				obs.stopped(this);
		}
	}
	
	public void destroy() {
		LOGGER.info("Destroy endpoint for address "+getAddress());
		if (started)
			stop();
		connector.destroy();
		for (EndpointObserver obs:observers)
			obs.destroyed(this);
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public void setExecutor(ScheduledExecutorService executor) {
		this.executor = executor;
		this.coapstack.setExecutor(executor);
	}
	
	public void addObserver(EndpointObserver obs) {
		observers.add(obs);
	}
	
	public void removeObserver(EndpointObserver obs) {
		observers.remove(obs);
	}
	
	public void sendRequest(final Request request) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					LOGGER.info("Endpoint sends request: "+request);
					coapstack.sendRequest(request);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	// TODO: Maybe we can do this a little nicer (e.g. call-back object)
	public void sendResponse(final Exchange exchange, final Response response) {
		// TODO: This should only be done if the executing thread is not already a thread pool thread
		executor.execute(new Runnable() {
			public void run() {
				try {
					LOGGER.info("Endpoint sends response back: "+response);
					coapstack.sendResponse(exchange, response);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void sendEmptyMessage(final Exchange exchange, final EmptyMessage message) {
		executor.execute(new Runnable() {
			public void run() {
				try {
					LOGGER.info("Endpoint sends empty message: "+message);
					coapstack.sendEmptyMessage(exchange, message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void setMessageDeliverer(MessageDeliverer deliverer) {
		coapstack.setDeliverer(deliverer);
	}
	
	private class RawDataChannelImpl implements RawDataChannel {

		@Override
		public void receiveData(final RawData msg) {
			if (msg.getAddress() == null)
				throw new NullPointerException();
			if (msg.getPort() == 0)
				throw new NullPointerException();
			
//			LOGGER.info("Endpoint creates new task to process message");
			// Process data on the stack's executor
			executor.execute(new Runnable() {
				public void run() {
					try {
						coapstack.receiveData(msg);
					} catch (Exception e) {
						e.printStackTrace();
						LOGGER.log(Level.WARNING, "Exception "+e+" while processing message", e);
					}
				}
			});
		}

		@Override
		public void sendData(RawData msg) {
//			LOGGER.info("Endpoint.RawDataChannelImpl forwards message to connector");
			// Process data on connector's threads
			connector.send(msg);
		}
	}

	public EndpointAddress getAddress() {
		return address;
	}

	public StackConfiguration getConfig() {
		return config;
	}
}

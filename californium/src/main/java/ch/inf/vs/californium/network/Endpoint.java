package ch.inf.vs.californium.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.coap.Message;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

/**
 * A CoAP Endpoint is is identified by transport layer multiplexing information
 * that can include a UDP port number and a security association.
 * (draft-ietf-core-coap-14: 1.2)
 */
public class Endpoint {

	private final static Logger LOGGER = Logger.getLogger(Server.class.getName());
	
	private Executor executor;
	
	private Connector connector;
	
	public Endpoint(int port) {
		connector = new UDPConnector(port);
		connector.setRawDataReceiver(new RawDataReceiverImpl());
	}
	
	public void start() throws IOException {
		if (executor == null)
			throw new IllegalStateException("Endpoint "+toString()+" has no executor yet and cannot start");
		
		try {
			connector.start();
		} catch (IOException e) {
			connector.stop();
			throw new IOException(e);
		}
	}
	
	public void stop() {
		connector.stop();
	}
	
	public void destroy() {
		connector.destroy();
	}
	
	public void receiveMessage(RawData raw) {
		DataUnparser parser = new DataUnparser(raw); // TODO: ThreadLocal<T>
		if (parser.isRequest()) {
			Request request = parser.unparseRequest();
			
		} else if (parser.isResponse()) {
			Response response = parser.unparseResponse();
			
		} else {
			Message message = parser.unparseEmptyMessage();
			// message is ACK or RST
		}
		LOGGER.info("Message received. TODO: deliver to server");
	}
	
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}
	
	private class RawDataReceiverImpl implements RawDataReceiver {

		@Override
		public void receiveData(final RawData msg) {
			executor.execute(new Runnable() {
				public void run() {
					Endpoint.this.receiveMessage(msg);
				}
			});
		}
	}
}

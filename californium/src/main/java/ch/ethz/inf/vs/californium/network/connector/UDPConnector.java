
package ch.ethz.inf.vs.californium.network.connector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.CalifonriumLogger;
import ch.ethz.inf.vs.californium.Server;
import ch.ethz.inf.vs.californium.network.Endpoint;
import ch.ethz.inf.vs.californium.network.EndpointAddress;
import ch.ethz.inf.vs.californium.network.NetworkConfig;
import ch.ethz.inf.vs.californium.network.NetworkConfigDefaults;
import ch.ethz.inf.vs.californium.network.RawData;
import ch.ethz.inf.vs.californium.network.RawDataChannel;

/**
 * The UDPConnector connects a server to the network using the UDP protocol. The
 * <code>UDPConnector</code> is bound to an {@link Endpoint} by a
 * {@link RawDataChannel}. An <code>Endpoint</code> sends messages encapsulated
 * within a {@link RawData} by calling the method {@link #send(RawData)} on the
 * connector. When the connector receives a message, it invokes
 * {@link RawDataChannel#receiveData(RawData)}. UDP broadcast is allowed.
 * // TODO: describe that we can make many threads
 */
public class UDPConnector implements Connector {

	private final static Logger LOGGER = CalifonriumLogger.getLogger(UDPConnector.class);

	public static final int UNDEFINED = 0;
	
	private boolean running;
	
	private DatagramSocket socket;
	
	private final NetworkConfig config;
	private final EndpointAddress localAddr;
	
	private List<Thread> receiverThreads;
	private List<Thread> senderThreads;

	/** The queue of outgoing block (for sending). */
	private final BlockingQueue<RawData> outgoing; // Messages to send
	
	/** The receiver of incoming messages */
	private RawDataChannel receiver; // Receiver of messages
	
	public UDPConnector(EndpointAddress address, NetworkConfig config) {
		this.localAddr = address;
		this.config = config;
		this.running = false;
		
		int capacity = config.getInt(NetworkConfigDefaults.UDP_CONNECTOR_OUT_CAPACITY);
		this.outgoing = new LinkedBlockingQueue<RawData>(capacity);
	}
	
	@Override
	public synchronized void start() throws IOException {
		if (running) return;
		this.running = true;
		
		// if localAddr is null or port is 0, the system decides
		socket = new DatagramSocket(localAddr.getPort(), localAddr.getAddress());
		
		int receiveBuffer = config.getInt(
				NetworkConfigDefaults.UDP_CONNECTOR_RECEIVE_BUFFER);
		if (receiveBuffer != UNDEFINED)
			socket.setReceiveBufferSize(receiveBuffer);
		receiveBuffer = socket.getReceiveBufferSize();
		
		int sendBuffer = config.getInt(
				NetworkConfigDefaults.UDP_CONNECTOR_SEND_BUFFER);
		if (sendBuffer != UNDEFINED)
			socket.setSendBufferSize(sendBuffer);
		sendBuffer = socket.getSendBufferSize();
		
		// if a wildcard in the address was used, we set the value that was
		// ultimately chosen by the system.
		if (localAddr.getAddress() == null)
			localAddr.setAddress(socket.getLocalAddress());
		if (localAddr.getPort() == 0)
			localAddr.setPort(socket.getLocalPort());

		// start receiver and sender threads
		int senderCount = config.getInt(NetworkConfigDefaults.UDP_CONNECTOR_SENDER_THREAD_COUNT);
		int receiverCount = config.getInt(NetworkConfigDefaults.UDP_CONNECTOR_RECEIVER_THREAD_COUNT);
		LOGGER.fine("UDP-connector starts "+senderCount+" sender threads and "+receiverCount+" receiver threads");
		
		receiverThreads = new LinkedList<Thread>();
		for (int i=0;i<receiverCount;i++) {
			receiverThreads.add(new Receiver("UDP-Receiver["+i+"]"+localAddr));
		}
		
		senderThreads = new LinkedList<Thread>();
		for (int i=0;i<senderCount;i++) {
			senderThreads.add(new Sender("UDP-Sender["+i+"]"+localAddr));
		}

		for (Thread t:receiverThreads)
			t.start();
		for (Thread t:senderThreads)
			t.start();
		/*
		 * Java bug: sometimes, socket.getReceiveBufferSize() and
		 * socket.setSendBufferSize() block forever when called here. When
		 * called up there, it seems to work. This issue occurred in Java
		 * 1.7.0_09, Windows 7.
		 */
		LOGGER.info("UDP connector listening on "+socket.getLocalSocketAddress()+", recv buf = "+receiveBuffer+", send buf = "+sendBuffer);
	}

	@Override
	public synchronized void stop() {
		if (!running) return;
		this.running = false;
		// stop all threads
		for (Thread t:senderThreads)
			t.interrupt();
		for (Thread t:receiverThreads)
			t.interrupt();
		outgoing.clear();
		if (socket != null)
			socket.close();
		socket = null;
	}

	@Override
	public synchronized void destroy() {
		stop();
	}
	
	@Override
	public void send(RawData msg) {
		if (msg == null)
			throw new NullPointerException();
		outgoing.add(msg);
	}

	@Override
	public void setRawDataReceiver(RawDataChannel receiver) {
		this.receiver = receiver;
	}
	
	private abstract class Worker extends Thread {

		/**
		 * Instantiates a new worker.
		 *
		 * @param name the name
		 */
		private Worker(String name) {
			super(name);
			setDaemon(false); // TODO: or rather true?
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			try {
				LOGGER.info("Start "+getName()+", (running = "+running+")");
				while (running) {
					try {
						work();
					} catch (Throwable t) {
						if (running)
							LOGGER.log(Level.WARNING, "Exception \""+t+"\" in thread " + getName()+": running="+running, t);
						else
							LOGGER.info("Exception \""+t+"\" in thread " + getName()+" has successfully stopped socket thread");
					}
				}
			} finally {
				LOGGER.info(getName()+" has terminated (running = "+running+")");
			}
		}

		/**
		 * // TODO: describe
		 * 
		 * @throws Exception the exception to be properly logged
		 */
		protected abstract void work() throws Exception;
	}
	
	private class Receiver extends Worker {
		
		private DatagramPacket datagram;
		private int size;
		
		private Receiver(String name) {
			super(name);
			this.size = config.getInt(NetworkConfigDefaults.UDP_CONNECTOR_DATAGRAM_SIZE);
			this.datagram = new DatagramPacket(new byte[size], size);
		}
		
		protected void work() throws IOException {
			datagram.setLength(size);
			socket.receive(datagram);
			if (Server.LOG_ENABLED)
				LOGGER.info("Connector ("+socket.getLocalSocketAddress()+") received "+datagram.getLength()+" bytes from "+datagram.getAddress()+":"+datagram.getPort());

			byte[] bytes = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getLength());
			RawData msg = new RawData(bytes);
			msg.setAddress(datagram.getAddress());
			msg.setPort(datagram.getPort());
			
			receiver.receiveData(msg);
		}
		
	}
	
	private class Sender extends Worker {
		
		private DatagramPacket datagram;
		
		private Sender(String name) {
			super(name);
			this.datagram = new DatagramPacket(new byte[0], 0);
		}
		
		protected void work() throws InterruptedException, IOException {
			RawData raw = outgoing.take(); // Blocking
			datagram.setData(raw.getBytes());
			datagram.setAddress(raw.getAddress());
			datagram.setPort(raw.getPort());
			if (Server.LOG_ENABLED)
				LOGGER.info("Connector ("+socket.getLocalSocketAddress()+") sends "+datagram.getLength()+" bytes to "+datagram.getSocketAddress());
			socket.send(datagram);
		}
	}
	
}

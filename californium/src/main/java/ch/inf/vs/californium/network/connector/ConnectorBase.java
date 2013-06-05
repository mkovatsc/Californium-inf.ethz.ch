package ch.inf.vs.californium.network.connector;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.RawData;
import ch.inf.vs.californium.network.RawDataChannel;

/**
 * ConnectorBase is a partial implementation of a {@link Connector}. It connects
 * a server to a network interface and a port. ConnectorBase contains two
 * separate threads for sending and receiving. The receiver thread constantly
 * calls {@link #receiveNext()} which is supposed to listen on a socket until a
 * datagram arrives and forward it to the {@link RawDataChannel}. The sender
 * thread constantly calls {@link #sendNext() which is supposed to wait on the
 * outgoing queue for a {@link RawData} message to send. Both
 * {@link #sendNext()} and {@link #receiveNext()} are expected to be blocking.
 */
public abstract class ConnectorBase implements Connector {
	
	/** The Logger. */
	private final static Logger LOGGER = Logger.getLogger(ConnectorBase.class.getName());

	/** The local address. */
	private final EndpointAddress localAddr;
	
	/** The receiver thread. */
	private Worker receiverThread; // TODO: Is it beneficial to start more than one thread?
	
	/** The sender thread. */
	private Worker senderThread;

	/** The queue of outgoing block (for sending). */
	private final BlockingQueue<RawData> outgoing; // Messages to send
	
	/** The receiver of incoming messages */
	private RawDataChannel receiver; // Receiver of messages
	
	/** Indicates whether the connector has started and not stopped yet */
	private boolean running;
	
	/**
	 * Instantiates a new connector base.
	 *
	 * @param address the address to listen to
	 */
	public ConnectorBase(EndpointAddress address) {
		if (address == null)
			throw new NullPointerException();
		this.localAddr = address;

		// TODO: optionally define maximal capacity
		this.outgoing = new LinkedBlockingQueue<RawData>();
	}

	/**
	 * Gets the name of the connector, e.g. the transport protocol used such as UDP or DTlS.
	 *
	 * @return the name
	 */
	public abstract String getName();
	
	/**
	 * Blocking method. Waits until a message comes from the network. New
	 * messages should be wrapped into a {@link RawData} object and
	 * {@link #forwardIncoming(RawData)} should be called to forward it to the
	 * {@link RawDataChannel}. // TODO: changes
	 * 
	 * @throws Exception
	 *             any exceptions that should be properly logged
	 */
	protected abstract RawData receiveNext() throws Exception;
	
	/**
	 * Blocking method. Waits until a new message should be sent over the
	 * network. //TODO: changed
	 * 
	 * @throws Exception any exception that should be properly logged
	 */
	protected abstract void sendNext(RawData raw) throws Exception;
	
	private void receiveNextMessageFromNetwork() throws Exception {
		RawData raw = receiveNext();
		// TODO
//		if (raw == null)
//			throw new NullPointerException();
		if (raw != null)
			receiver.receiveData(raw);
	}
	
	private void sendNextMessageOverNetwork() throws Exception {
		RawData raw = outgoing.take(); // Blocking
		if (raw == null)
			throw new NullPointerException();
		sendNext(raw);
	}
	
//	/**
//	 * Blocking method. Gets the next outgoing message (for sending).
//	 *
//	 * @return the next outgoing message
//	 * @throws InterruptedException the interrupted exception
//	 * @throws IOException Signals that an I/O exception has occurred.
//	 */
//	protected RawData getNextOutgoing() throws InterruptedException, IOException {
//		return outgoing.take();
//	}
//	
//	/**
//	 * Forward incoming message to the receiver.
//	 *
//	 * @param raw the raw data
//	 */
//	protected void forwardIncoming(RawData raw) {
//		
//	}
	
	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#start()
	 */
	@Override
	public synchronized void start() throws IOException {
		if (running) return;
		receiverThread = new Worker(getName()+"-Receiver("+localAddr.getPort()+")") {
			public void work() throws Exception { receiveNextMessageFromNetwork(); }};
		
		senderThread = new Worker(getName()+"-Sender("+localAddr.getPort()+")") {
			public void work() throws Exception { sendNextMessageOverNetwork(); } };
		
		receiverThread.start();
		senderThread.start();
			
		running = true;
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#stop()
	 */
	@Override
	public synchronized void stop() {
		if (!running) return;
		running = false;
		receiverThread.interrupt();
		senderThread.interrupt();
		receiverThread = null;
		senderThread = null;
		outgoing.clear();
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#destroy()
	 */
	@Override
	public synchronized void destroy() {
		stop();
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#send(ch.inf.vs.californium.network.RawData)
	 */
	@Override
	public void send(RawData msg) {
		if (msg == null)
			throw new NullPointerException();
		outgoing.add(msg);
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#setRawDataReceiver(ch.inf.vs.californium.network.RawDataChannel)
	 */
	@Override
	public void setRawDataReceiver(RawDataChannel receiver) {
		this.receiver = receiver;
	}
	
	/**
	 * Abstract worker thread that wraps calls to
	 * {@link ConnectorBase#getNextOutgoing()} and
	 * {@link ConnectorBase#receiveNext()}. Therefore, exceptions do not crash
	 * the threads and will be properly logged.
	 */
	private abstract class Worker extends Thread {

		/**
		 * Instantiates a new worker.
		 *
		 * @param name the name, e.g., of the transport protocol
		 */
		private Worker(String name) {
			super(name);
			setDaemon(false);
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			try {
				LOGGER.info("Start "+getName());
				while (running) {
					try {
						work();
					} catch (Exception e) {
						if (running)
							LOGGER.log(Level.WARNING, "Exception \""+e+"\" in thread " + getName()+": running="+running, e);
						else
							LOGGER.info("Exception \""+e+"\" in thread " + getName()+" has successfully stopped socket thread");
					}
				}
			} finally {
				LOGGER.info(getName()+" has terminated");
			}
		}

		/**
		 * Override this method and call {@link ConnectorBase#receiveNext()} or
		 * {@link ConnectorBase#sendNext()}.
		 * 
		 * @throws Exception the exception to be properly logged
		 */
		protected abstract void work() throws Exception;
	}

	/**
	 * Gets the local address this connector is listening to.
	 *
	 * @return the local address
	 */
	public EndpointAddress getLocalAddr() {
		return localAddr;
	}
	
	/**
	 * Gets the receiver.
	 *
	 * @return the receiver
	 */
	public RawDataChannel getReceiver() {
		return receiver;
	}

	/**
	 * Sets the receiver for incoming messages.
	 *
	 * @param receiver the new receiver
	 */
	public void setReceiver(RawDataChannel receiver) {
		this.receiver = receiver;
	}

	/**
	 * Checks the connector has started but not stopped yet.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning() {
		return running;
	}

}

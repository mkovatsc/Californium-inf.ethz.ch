package ch.inf.vs.californium.network.connector;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.RawData;
import ch.inf.vs.californium.network.RawDataChannel;

public abstract class ConnectorBase implements Connector {

	private final static Logger LOGGER = Logger.getLogger(ConnectorBase.class.getName());

	private final EndpointAddress localAddr;
	
	private Worker receiverThread; // TODO: Is it beneficial to start more than one thread?
	private Worker senderThread;

	private final BlockingQueue<RawData> outgoing; // Messages to send
	private RawDataChannel receiver; // Receiver of messages
	
	private boolean running;
	
	public ConnectorBase(EndpointAddress address) {
		if (address == null)
			throw new NullPointerException();
		this.localAddr = address;

		// TODO: optionally define maximal capacity
		this.outgoing = new LinkedBlockingQueue<RawData>();
	}

	protected abstract void receiveNext() throws Exception;
	protected abstract void sendNext() throws Exception;
	public abstract String getName();
	
	protected RawData getNextOutgoing() throws InterruptedException, IOException {
		return outgoing.take();
	}
	
	protected void forwardIncomming(RawData raw) {
		receiver.receiveData(raw);
	}
	
	@Override
	public synchronized void start() throws IOException {
		if (running) return;
		receiverThread = new Worker(getName()+"-Receiver("+localAddr.getPort()+")") {
			public void work() throws Exception { receiveNext(); }};
		
		senderThread = new Worker(getName()+"-Sender("+localAddr.getPort()+")") {
			public void work() throws Exception { sendNext(); } };
		
		receiverThread.start();
		senderThread.start();
			
		running = true;
	}

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

	@Override
	public synchronized void destroy() {
		stop();
	}

	@Override
	public void send(RawData msg) {
		outgoing.add(msg);
	}

	@Override
	public void setRawDataReceiver(RawDataChannel receiver) {
		this.receiver = receiver;
	}
	
	private abstract class Worker extends Thread {

		private Worker(String name) {
			super(name);
			setDaemon(false);
		}

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

		protected abstract void work() throws Exception;
	}

	public EndpointAddress getLocalAddr() {
		return localAddr;
	}
	
	public RawDataChannel getReceiver() {
		return receiver;
	}

	public void setReceiver(RawDataChannel receiver) {
		this.receiver = receiver;
	}

	public boolean isRunning() {
		return running;
	}

}

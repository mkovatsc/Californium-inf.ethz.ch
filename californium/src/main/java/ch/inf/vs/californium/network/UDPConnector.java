
package ch.inf.vs.californium.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDPConnector implements Connector {

	private final static Logger LOGGER = Logger.getLogger(UDPConnector.class.getName());

	private DatagramSocket socket;

	private EndpointAddress localAddr;

	private Worker receiverThread; // TODO: Is it beneficial to start more than one thread?
	private Worker senderThread;

	private RawDataChannel receiver; // Receiver of messages
	private BlockingQueue<RawData> outgoing; // Messages to send

	private int datagramSize = 1000; // TODO: change dynamically?
	
	private volatile boolean running;

	public UDPConnector(EndpointAddress address) {
		if (address == null)
			throw new NullPointerException();
		this.localAddr = address;

		// TODO: optionally define maximal capacity
		this.outgoing = new LinkedBlockingQueue<RawData>();
	}

	@Override
	public void start() throws SocketException {
		this.running = true;
		if (socket == null) {
			// if localAddr is null or port is 0, the system decides
			socket = new DatagramSocket(localAddr.getPort(), localAddr.getInetAddress());
			if (localAddr.getInetAddress() == null)
				localAddr.setAddress(socket.getLocalAddress());
			if (localAddr.getPort() == 0)
				localAddr.setPort(socket.getLocalPort());
		} else ; // TODO: bind
		
		LOGGER.info("UDP connector listening on "+localAddr);
		
		// socket.setReuseAddress(true);

		receiverThread = new ReceiverThreat();
		senderThread = new SenderThread();
		receiverThread.start();
		senderThread.start();
	}

	@Override
	public void stop() {
		running = false;
		receiverThread.interrupt();
		senderThread.interrupt();
//		socket.disconnect(); // TODO might be the wrong one
		outgoing.clear();
	}

	@Override
	public void destroy() {
		stop();
		socket.close();
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
				LOGGER.info(getName()+" has terminted");
			}
		}

		public abstract void work() throws Exception;
	}

	private class ReceiverThreat extends Worker {

		/*
		 * TODO: Can we increase datagramSize to make data extraction faster?
		 */
		private byte[] buffer = new byte[datagramSize];
		private DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

		public ReceiverThreat() {
			super("UDP-ReceiverThread[addr:" + socket.getInetAddress() + ":" + localAddr.getPort() + "]");
		}

		public void work() throws IOException {
			socket.receive(datagram);
			LOGGER.info("Connector received "+datagram.getLength()+" bytes from "+datagram.getAddress()+":"+datagram.getPort());

			byte[] bytes = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getLength());
			RawData msg = new RawData(bytes);
			msg.setAddress(datagram.getAddress());
			msg.setPort(datagram.getPort());
			receiver.receiveData(msg);
		}
	}

	private class SenderThread extends Worker {

		private DatagramPacket datagram = new DatagramPacket(new byte[0], 0);

		public SenderThread() {
			super("UDP-SenderThread[addr:" + socket.getInetAddress() + ":" + localAddr.getPort() + "]");
		}

		public void work() throws InterruptedException, IOException {
			RawData msg = outgoing.take();
			datagram.setData(msg.getBytes());
			datagram.setAddress(msg.getAddress());
			datagram.setPort(msg.getPort());
			LOGGER.info("Connector sends "+datagram.getLength()+" bytes to "+datagram.getAddress()+":"+datagram.getPort());
			socket.send(datagram);
		}
	}
}

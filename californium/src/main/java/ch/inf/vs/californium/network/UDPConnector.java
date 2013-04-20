
package ch.inf.vs.californium.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDPConnector implements Connector {

	private final static Logger LOGGER = Logger.getLogger(UDPConnector.class.getName());

	private DatagramSocket socket;

	private InetAddress localAddr;
	private int port;

	private Worker receiverThread; // TODO: make more than one thread
	private Worker senderThread;

	private RawDataReceiver receiver;
	private BlockingQueue<RawData> outgoing;

	private int datagramSize = 1000; // TODO: change dynamically

	public UDPConnector(int port) {
		this(null, port);
	}

	public UDPConnector(InetAddress localAddr, int port) {
		this.localAddr = localAddr;
		this.port = port;

		// TODO: optionally define maximal capacity
		this.outgoing = new LinkedBlockingQueue<RawData>();
	}

	@Override
	public void start() throws SocketException {
		if (socket == null)
		// if localAddr is null or port is 0, the system decides
		socket = new DatagramSocket(new InetSocketAddress(localAddr, port));
		// socket.setReuseAddress(true);

		receiverThread = new ReceiverThreat();
		senderThread = new SenderThread();
		receiverThread.start();
		senderThread.start();
	}

	@Override
	public void stop() {
		receiverThread.interrupt();
		senderThread.interrupt();
		socket.disconnect(); // TODO might be the wrong one
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
	public void setRawDataReceiver(RawDataReceiver receiver) {
		this.receiver = receiver;
	}

	private abstract class Worker extends Thread {

		private Worker(String name) {
			super(name);
			setDaemon(true);
		}

		public void run() {
			while (!isInterrupted()) {
				try {
					work();
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Exception in thread \"" + Thread.currentThread().getName() + "\"", e);
				}
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
			super("UDP-ReceiverThread[addr:" + socket.getInetAddress() + ":" + port + "]");
		}

		public void work() throws IOException {
			socket.receive(datagram);

			byte[] bytes = datagram.getData();
			receiver.receiveData(new RawData(bytes));
//			incomming.add(new RawMessage(bytes)); // throws exception if full
		}
	}

	private class SenderThread extends Worker {

		private DatagramPacket datagram = new DatagramPacket(new byte[0], 0);

		public SenderThread() {
			super("UDP-SenderThread[addr:" + socket.getInetAddress() + ":" + port + "]");
		}

		public void work() throws InterruptedException, IOException {
			RawData msg = outgoing.take();
			datagram.setData(msg.getBytes());
			socket.send(datagram);
		}
	}
}


package ch.inf.vs.californium.network.connector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.RawData;
import ch.inf.vs.californium.network.RawDataChannel;

public class UDPConnector extends ConnectorBase {

	private final static Logger LOGGER = Logger.getLogger(UDPConnector.class.getName());

	private DatagramSocket socket;

	private final EndpointAddress localAddr;

	private int datagramSize = 1000; // TODO: change dynamically?
	private byte[] buffer = new byte[datagramSize]; // Can we speed it up with larger buffer?
	private DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
	private DatagramPacket sendDatagram = new DatagramPacket(new byte[0], 0);
	
	public UDPConnector(EndpointAddress address) {
		super(address);
		this.localAddr = address;
	}

	@Override
	public synchronized void start() throws IOException {
		if (socket == null) {
			// if localAddr is null or port is 0, the system decides
			socket = new DatagramSocket(localAddr.getPort(), localAddr.getAddress());
			if (localAddr.getAddress() == null)
				localAddr.setAddress(socket.getLocalAddress());
			if (localAddr.getPort() == 0)
				localAddr.setPort(socket.getLocalPort());
		} else ; // TODO: bind
		super.start();
		
		LOGGER.info("UDP connector listening on "+localAddr);
	}

	@Override
	public synchronized void stop() {
		super.stop();
//		socket.disconnect(); // TODO might be the wrong one
	}

	@Override
	public synchronized void destroy() {
		super.destroy();
		socket.close();
	}

	@Override
	public String getName() {
		return "UDP";
	}

	@Override
	protected void receiveNext() throws Exception {
		socket.receive(datagram);
		LOGGER.info("Connector received "+datagram.getLength()+" bytes from "+datagram.getAddress()+":"+datagram.getPort());

		byte[] bytes = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getLength());
		RawData msg = new RawData(bytes);
		msg.setAddress(datagram.getAddress());
		msg.setPort(datagram.getPort());
		forwardIncoming(msg);
	}

	@Override
	protected void sendNext() throws Exception {
		RawData msg = getNextOutgoing();
		sendDatagram.setData(msg.getBytes());
		sendDatagram.setAddress(msg.getAddress());
		sendDatagram.setPort(msg.getPort());
		LOGGER.info("Connector sends "+sendDatagram.getLength()+" bytes to "+sendDatagram.getAddress()+":"+sendDatagram.getPort());
		socket.send(sendDatagram);		
	}
}

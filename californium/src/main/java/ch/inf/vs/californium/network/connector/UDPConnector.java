
package ch.inf.vs.californium.network.connector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.logging.Logger;

import ch.inf.vs.californium.CalifonriumLogger;
import ch.inf.vs.californium.Server;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.NetworkConfig;
import ch.inf.vs.californium.network.RawData;
import ch.inf.vs.californium.network.RawDataChannel;

/**
 * The UDPConnector connects a server to the network using the UDP protocol. The
 * <code>UDPConnector</code> is bound to an {@link Endpoint} by a
 * {@link RawDataChannel}. An <code>Endpoint</code> sends messages encapsulated
 * within a {@link Raw} by calling the method {@link #send(RawData)} on the
 * connector. When the connector receives a message, it invokes
 * {@link RawDataChannel#receiveData(RawData)}. UDP broadcast is allowed.
 */
public class UDPConnector extends ConnectorBase {

	private final static Logger LOGGER = CalifonriumLogger.getLogger(UDPConnector.class);

	private DatagramSocket socket;

	private final NetworkConfig config;
	private final EndpointAddress localAddr;

	private int datagramSize = 1000; // TODO: change dynamically?
	private byte[] buffer = new byte[datagramSize]; // Can we speed it up with larger buffer?
	private DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
	private DatagramPacket sendDatagram = new DatagramPacket(new byte[0], 0);
	
	public UDPConnector(EndpointAddress address, NetworkConfig config) {
		super(address);
		this.localAddr = address;
		this.config = config;
	}
	
	// TODO: How to make sure, that we are not already started?
	@Override
	public synchronized void start() throws IOException {
		// if localAddr is null or port is 0, the system decides
		socket = new DatagramSocket(localAddr.getPort(), localAddr.getAddress());
		
		int receiveBuffer = config.getReceiveBuffer();
		if (receiveBuffer != 0)
			socket.setReceiveBufferSize(receiveBuffer);
		receiveBuffer = socket.getReceiveBufferSize();
		
		int sendBuffer = config.getSendBuffer();
		if (sendBuffer != 0)
			socket.setSendBufferSize(sendBuffer);
		sendBuffer = socket.getSendBufferSize();
		
		if (localAddr.getAddress() == null)
			localAddr.setAddress(socket.getLocalAddress());
		if (localAddr.getPort() == 0)
			localAddr.setPort(socket.getLocalPort());
		super.start();
		
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
		super.stop();
		if (socket != null)
			socket.close();
		socket = null;
//		socket.disconnect(); // TODO might be the wrong one
	}

	@Override
	public synchronized void destroy() {
		stop();
		super.destroy();
	}

	@Override
	public String getName() {
		return "UDP";
	}

	@Override
	protected RawData receiveNext() throws Exception {
		socket.receive(datagram);
		if (Server.LOG_ENABLED)
			LOGGER.fine("Connector ("+socket.getLocalSocketAddress()+") received "+datagram.getLength()+" bytes from "+datagram.getAddress()+":"+datagram.getPort());

		byte[] bytes = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getLength());
		RawData msg = new RawData(bytes);
		msg.setAddress(datagram.getAddress());
		msg.setPort(datagram.getPort());
		return msg;
	}

	@Override
	protected void sendNext(RawData msg) throws Exception {
		sendDatagram.setData(msg.getBytes());
		sendDatagram.setAddress(msg.getAddress());
		sendDatagram.setPort(msg.getPort());
		if (Server.LOG_ENABLED)
			LOGGER.fine("Connector ("+socket.getLocalSocketAddress()+") sends "+sendDatagram.getLength()+" bytes to "+sendDatagram.getAddress()+":"+sendDatagram.getPort());
		socket.send(sendDatagram);		
	}
}

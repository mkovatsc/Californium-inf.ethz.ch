package ch.inf.vs.californium.network.connector;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.RawData;


/**
 * TODO: Is a TCP connector even useful? Is it wise to use the same stack as for
 * UDP? Shoudl we rather use a non-blocking TCP Sockets?
 * 
 * In the discussion
 * http://www.ietf.org/mail-archive/web/core/current/msg02797.html Carsten
 * Bormann said on March 2012:
 * "Klaus, I think we may need to write up the TCP header format..." so I guess
 * the exact specification for TCP over CoAP is not decided yet,
 * 
 * Find source code of that thing TcpDatagramSocket:
 * http://docs.oracle.com/cd/E18686_01/coh.37/e18683/com/tangosol/net/TcpDatagramSocket.html
 */
public class TCPConnector extends ConnectorBase {

	private final static Logger LOGGER = Logger.getLogger(TCPConnector.class.getName());
	
	private EndpointAddress address;
	
	private ServerSocket socket;
	
	private int datagramSize = 1000; // TODO: change dynamically?
	private byte[] buffer = new byte[datagramSize];
	
	public TCPConnector(EndpointAddress address) {
		super(address);
		this.address = address;
	}

	@Override
	public synchronized void start() throws IOException {
		this.socket = new ServerSocket(address.getPort(), 0, address.getInetAddress());
		super.start();
	}
	
	@Override
	public synchronized void stop() {
		super.stop();
		try {
			this.socket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Exception \""+e+"\" while closing ServerSocket", e);
		}
	}
	
	@Override
	protected void receive() throws Exception {
		Socket connection = socket.accept();
		
		@SuppressWarnings("unused")
		InputStream input = connection.getInputStream();

		int length = 0;
		/*
		 * TODO: Problem: We don't know how many bytes to read until we have
		 * received a full message! This is also discussed in the link from
		 * above.
		 */
		
		byte[] bytes = Arrays.copyOfRange(buffer, 0, length);
		RawData msg = new RawData(bytes);
		msg.setAddress(null /*FIXME*/);
		msg.setPort( 0 /*FIXME*/);
		getReceiver().receiveData(msg);
	}

	@Override
	protected void send() throws Exception {
		// TODO
	}

	@Override
	public String getName() {
		return "TCP"; // TODO: append [addr={addr}: {port}]
	}

}

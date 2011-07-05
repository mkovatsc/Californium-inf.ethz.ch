package layers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import util.Log;

import coap.Message;

/*
 * This class describes the functionality of a UDP layer that is able
 * to exchange CoAP messages.
 * 
 * According to the UDP protocoll, messages are exchanged over an unreliable
 * channel and thus may arrive out of order, appear duplicated or go missing
 * without notice.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */

public class UDPLayer extends Layer {

	// CoAP specific constants /////////////////////////////////////////////////

	// default CoAP port as defined in draft-ietf-core-coap-05, section 7.1:
	// MUST be supported by a server for resource discovery and
	// SHOULD be supported for providing access to other resources.
	public static final int DEFAULT_PORT = 5683;

	// CoAP URI scheme name as defined in draft-ietf-core-coap-05, section 11.4:
	public static final String URI_SCHEME_NAME = "coap";

	// Implementation specific constants ///////////////////////////////////////

	// buffer size for incoming datagrams
	// TODO find correct value
	private static final int RX_BUFFER_SIZE    = 4 * 1024; // bytes

	// Inner Classes ///////////////////////////////////////////////////////////

	class ReceiverThread extends Thread {
		
		public ReceiverThread() {
			super("ReceiverThread");
		}
		
		@Override
		public void run() {
			// always listen for incoming datagrams
			while (true) {

				// allocate buffer
				byte[] buffer = new byte[RX_BUFFER_SIZE];

				// initialize new datagram
				DatagramPacket datagram = new DatagramPacket(buffer,
						buffer.length);

				// receive datagram
				try {
					socket.receive(datagram);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
				
				datagramReceived(datagram);
					
			}
		}
	}

	// Constructors ////////////////////////////////////////////////////////////

	/*
	 * Constructor for a new UDP layer
	 * 
	 * @param port The local UDP port to listen for incoming messages
	 * @param daemon True if receiver thread should terminate with main thread
	 */
	public UDPLayer(int port, boolean daemon) throws SocketException {
		// initialize members
		this.socket = new DatagramSocket(port);
		this.receiverThread = new ReceiverThread();

		// decide if receiver thread terminates with main thread
		receiverThread.setDaemon(daemon);

		// start listening right from the beginning
		this.receiverThread.start();

	}

	/*
	 * Constructor for a new UDP layer
	 */
	public UDPLayer() throws SocketException {
		this(0, true); // use any available port on the local host machine
		// TODO use -1 instead of 0?
	}

	// Commands ////////////////////////////////////////////////////////////////

	/*
	 * Decides if the listener thread persists after the main thread terminates
	 * 
	 * @param on True if the listener thread should stay alive after the main
	 * thread terminates. This is useful for e.g. server applications
	 */
	public void setDaemon(boolean on) {
		receiverThread.setDaemon(on);
	}

	// Queries /////////////////////////////////////////////////////////////////

	/*
	 * Checks whether the listener thread persists after the main thread
	 * terminates
	 * 
	 * @return True if the listener thread stays alive after the main thread
	 * terminates. This is useful for e.g. server applications
	 */
	public boolean isDaemon() {
		return receiverThread.isDaemon();
	}

	public int getPort() {
		return socket.getLocalPort();
	}

	// I/O implementation //////////////////////////////////////////////////////

	@Override
	protected void doSendMessage(Message msg) throws IOException {

		// assemble datagram components:

		// retrieve URI
		URI uri = msg.getURI();

		// retrieve remote address
		// throws UnknownHostException, subclass of IOException
		InetAddress address = msg.getAddress();

		// retrieve remote port
		int port = uri != null ? uri.getPort() : -1;
		if (port < 0)
			port = DEFAULT_PORT;

		// retrieve payload
		byte[] payload = msg.toByteArray();

		// create datagram
		DatagramPacket datagram = new DatagramPacket(payload, payload.length,
				address, port);

		// remember when this message was sent for the first time
		// set timestamp only once in order
		// to handle retransmissions correctly
		if (msg.getTimestamp() == 0) {
			msg.setTimestamp(System.currentTimeMillis());
		}

		// send it over the UDP socket
		socket.send(datagram);
		
	}

	@Override
	protected void doReceiveMessage(Message msg) {
		
		// pass message to registered receivers
		deliverMessage(msg);
	}

	// Internal ////////////////////////////////////////////////////////////////

	private void datagramReceived(DatagramPacket datagram) {

		if (datagram.getLength() > 0) {
		
			// get current time
			long timestamp = System.currentTimeMillis();
	
			// extract message data from datagram
			byte[] data = Arrays.copyOfRange(datagram.getData(),
					datagram.getOffset(), datagram.getLength());
	
			// create new message from the received data
			Message msg = Message.fromByteArray(data);
	
			// remember when this message was received
			msg.setTimestamp(timestamp);
	
			// assemble URI components from datagram
	
			String scheme = URI_SCHEME_NAME;
			String userInfo = null;
			// TODO getHostName() leads to replies always in IPv4...
			// String host = datagram.getAddress().getHostName();
			String host = datagram.getAddress().getHostAddress();
			int port = datagram.getPort();
			String path = null;
			String query = null;
			String fragment = null;
	
			// set message URI to sender URI
			try {
	
				msg.setURI(new URI(scheme, userInfo, host, port, path, query,
						fragment));
	
			} catch (URISyntaxException e) {
	
				Log.error(this, "Failed to build URI for incoming message: %s",
					e.getMessage());
			}
	
			// call receive handler
			receiveMessage(msg);
			
		} else {
			
			Log.warning(this, "Empty datagram dropped from: %s",
				datagram.getAddress().getHostName());
		}
	}

	// Attributes //////////////////////////////////////////////////////////////

	// The UDP socket used to send and receive datagrams
	// TODO Use MulticastSocket
	private DatagramSocket socket;

	// The thread that listens on the socket for incoming datagrams
	private ReceiverThread receiverThread;

}

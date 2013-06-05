package ch.inf.vs.californium.network.connector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import ch.inf.vs.californium.network.EndpointAddress;
import ch.inf.vs.californium.network.RawData;
import ch.inf.vs.californium.network.connector.dtls.AlertMessage;
import ch.inf.vs.californium.network.connector.dtls.AlertMessage.AlertDescription;
import ch.inf.vs.californium.network.connector.dtls.AlertMessage.AlertLevel;
import ch.inf.vs.californium.network.connector.dtls.ApplicationMessage;
import ch.inf.vs.californium.network.connector.dtls.ByteArrayUtils;
import ch.inf.vs.californium.network.connector.dtls.ClientHandshaker;
import ch.inf.vs.californium.network.connector.dtls.ClientHello;
import ch.inf.vs.californium.network.connector.dtls.ContentType;
import ch.inf.vs.californium.network.connector.dtls.DTLSFlight;
import ch.inf.vs.californium.network.connector.dtls.DTLSMessage;
import ch.inf.vs.californium.network.connector.dtls.DTLSSession;
import ch.inf.vs.californium.network.connector.dtls.FragmentedHandshakeMessage;
import ch.inf.vs.californium.network.connector.dtls.HandshakeException;
import ch.inf.vs.californium.network.connector.dtls.HandshakeMessage;
import ch.inf.vs.californium.network.connector.dtls.Handshaker;
import ch.inf.vs.californium.network.connector.dtls.Record;
import ch.inf.vs.californium.network.connector.dtls.ResumingClientHandshaker;
import ch.inf.vs.californium.network.connector.dtls.ResumingServerHandshaker;
import ch.inf.vs.californium.network.connector.dtls.ServerHandshaker;
import ch.inf.vs.californium.network.connector.dtls.ServerHello;

public class DTLSConnector extends ConnectorBase {
	
	private final static Logger LOGGER = Logger.getLogger(DTLSConnector.class.getName());

	/////////////////
	// the maximum fragment size before DTLS fragmentation must be applied
//	public static final String KEY_STORE_LOCATION = "path/to/keyStore.jks" /*.replace("/", File.pathSeparator)*/;
	public static final String KEY_STORE_LOCATION = "cert/keyStore.jks";
	
	private int max_fragment_length = 200; // TODO: get from config

	// the initial timer value for retransmission; rfc6347, section: 4.2.4.1
	private int retransmission_timeout = 1000; // milliseconds // TODO: get from config
	
	// maximal number of retransmissions before the attempt to transmit a message is canceled
	private int max_retransmitt = 4; // TODO: get from config
	/////////////////
	
	private final EndpointAddress address;
	
	private int datagramSize = 1000; // TODO: change dynamically?
	private byte[] buffer = new byte[datagramSize];
//	private DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	
	private DatagramSocket socket;
	
	/** The timer daemon to schedule retransmissions. */
	private Timer timer = new Timer(true); // run as daemon
	
	/** Storing sessions according to peer-addresses */
	private ConcurrentHashMap<String, DTLSSession> dtlsSessions = new ConcurrentHashMap<>();

	/** Storing handshakers according to peer-addresses. */
	private ConcurrentHashMap<String, Handshaker> handshakers = new ConcurrentHashMap<>();

	/** Storing flights according to peer-addresses. */
	private ConcurrentHashMap<String, DTLSFlight> flights = new ConcurrentHashMap<>();
	
	public DTLSConnector(EndpointAddress address) {
		super(address);
		this.address = address;
	}
	
	@Override
	public synchronized void start() throws IOException {
		socket = new DatagramSocket(address.getPort(), address.getAddress());
		super.start();
		
		LOGGER.info("DLTS connector listening on "+address);
	}
	
	@Override
	public synchronized void stop() {
		this.socket.close();
		super.stop();
	}
	
	// TODO: We should not return null
	@Override
	protected RawData receiveNext() throws Exception {
		byte[] buffer = new byte[1000];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		
		if (packet.getLength() == 0)
			return null;

		EndpointAddress peerAddress = new EndpointAddress(packet.getAddress(), packet.getPort());
//		LOGGER.info(" => find handshaker for key "+peerAddress.toString());
		DTLSSession session = dtlsSessions.get(peerAddress.toString());
		Handshaker handshaker = handshakers.get(peerAddress.toString());
		byte[] data = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getLength());

		try {
			// TODO multiplex message types: DTLS or CoAP
			List<Record> records = Record.fromByteArray(data);

			for (Record record : records) {
				record.setSession(session);

				RawData raw = null;

				ContentType contentType = record.getType();
//				LOGGER.info(" => contentType: "+contentType);
				DTLSFlight flight = null;
				switch (contentType) {
				case APPLICATION_DATA:
					if (session == null) {
						// There is no session available, so no application data
						// should be received, discard it
						LOGGER.info("Discarded unexpected application data message from " + peerAddress.toString());
						return null;
					}
					// at this point, the current handshaker is not needed
					// anymore, remove it
					handshakers.remove(peerAddress.toString());

					ApplicationMessage applicationData = (ApplicationMessage) record.getFragment();
//					msg = Message.fromByteArray(applicationData.getData());
					raw = new RawData(applicationData.getData());
					break;

				case ALERT:
				case CHANGE_CIPHER_SPEC:
				case HANDSHAKE:
//					LOGGER.info(" => handshaker: "+handshaker);
					if (handshaker == null) {
						/*
						 * A handshake message received, but no handshaker
						 * available: this must mean that we either received
						 * a HelloRequest (from server) or a ClientHello
						 * (from client) => initialize appropriate
						 * handshaker type
						 */

						HandshakeMessage message = (HandshakeMessage) record.getFragment();
//						LOGGER.info("=> received message "+message);

						switch (message.getMessageType()) {
						case HELLO_REQUEST:
							// client side
							if (session == null) {
								// create new session
								session = new DTLSSession(true);
								// store session according to peer address
								dtlsSessions.put(peerAddress.toString(), session);

								LOGGER.finest("Client: Created new session with peer: " + peerAddress.toString());
							}
							handshaker = new ClientHandshaker(peerAddress, null, session);
							handshakers.put(peerAddress.toString(), handshaker);
							break;

						case CLIENT_HELLO:
							/*
							 * Server side: server received a client hello:
							 * check first if client wants to resume a
							 * session (message must contain session
							 * identifier) and then check if particular
							 * session still available, otherwise conduct
							 * full handshake with fresh session.
							 */

							if (!(message instanceof FragmentedHandshakeMessage)) {
								// check if session identifier set
								ClientHello clientHello = (ClientHello) message;
								session = getSessionByIdentifier(clientHello.getSessionId().getSessionId());
							}
							

							if (session == null) {
								// create new session
								session = new DTLSSession(false);
								// store session according to peer address
								dtlsSessions.put(peerAddress.toString(), session);

								LOGGER.info("Server: Created new session with peer: " + peerAddress.toString());
								handshaker = new ServerHandshaker(peerAddress, session);
							} else {
								handshaker = new ResumingServerHandshaker(peerAddress, session);
							}
							handshakers.put(peerAddress.toString(), handshaker);
							break;

						default:
							LOGGER.severe("Received unexpected first handshake message (type="+message.getMessageType()+") from " + peerAddress.toString() + ":\n" + message.toString());
							break;
						}
					}
					flight = handshaker.processMessage(record);
					break;

				default:
					LOGGER.severe("Received unknown DTLS record from " + peerAddress.toString() + ":\n" + ByteArrayUtils.toHexString(data));
					break;
				}

				if (flight != null) {
					cancelPreviousFlight(peerAddress);

					flight.setPeerAddress(peerAddress);
					flight.setSession(session);

					if (flight.isRetransmissionNeeded()) {
						flights.put(peerAddress.toString(), flight);
						scheduleRetransmission(flight);
					}

					sendFlight(flight);
				}

				if (raw != null) {


//					msg.setPeerAddress(new EndpointAddress(datagram.getAddress(), datagram.getPort()));
					raw.setAddress(packet.getAddress());
					raw.setPort(packet.getPort());

					// TODO: Don't understand why we would do that here:
					/*
					if (packet.getLength() > rx_buffer_size) {
						LOGGER.info(String.format("Marking large datagram for blockwise transfer: %s", msg.key()));
						msg.requiresBlockwise(true);
					}
					*/

//					receiveMessage(msg);
					return raw;
				}
			}

		} catch (Exception e) {
			/*
			 * If it is a known handshake failure, send the specific Alert,
			 * otherwise the general Handshake_Failure Alert. 
			 */
			DTLSFlight flight = new DTLSFlight();
			flight.setRetransmissionNeeded(false);
			flight.setPeerAddress(peerAddress);
			flight.setSession(session);
			
			AlertMessage alert;
			if (e instanceof HandshakeException) {
				alert = ((HandshakeException) e).getAlert();
				LOGGER.severe("Handshake Exception (" + peerAddress.toString() + "): " + e.getMessage());
			} else {
				alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
				LOGGER.log(Level.SEVERE, "Unknown Exception (" + peerAddress + ").", e);
			}
//			LOGGER.severe("Datagram which lead to exception (" + peerAddress + "): " + ByteArrayUtils.toHexString(data));
//			LOGGER.severe(logStackTrace(e));
			LOGGER.log(Level.SEVERE, "Datagram which lead to exception (" + peerAddress + "): " + ByteArrayUtils.toHexString(data), e);
			
			if (session == null) {
				// if the first received message failed, no session has been set
				session = new DTLSSession(false);
			}
			cancelPreviousFlight(peerAddress);
			
			flight.addMessage(new Record(ContentType.ALERT, session.getWriteEpoch(), session.getSequenceNumber(), alert, session));
			sendFlight(flight);
		} // receive()
		return null;
	}

	@Override
	protected void sendNext(RawData message) throws Exception {
		// remember when this message was sent for the first time
		// set timestamp only once in order
		// to handle retransmissions correctly
//		if (message.getTimestamp() == -1) {
//			message.setTimestamp(System.nanoTime());
//		}

//		RawData message = getNextOutgoing();
		
//		EndpointAddress peerAddress = message.getPeerAddress();
		EndpointAddress peerAddress = message.getEndpointAddress();
		LOGGER.info("Send message to "+address);

		DTLSSession session = dtlsSessions.get(peerAddress.toString());
		
		/*
		 * When the DTLS layer receives a message from an upper layer, there is
		 * either a already a DTLS session available with the peer or a new
		 * handshake must be executed. If a session is available and active, the
		 * message will be encrypted and send to the peer, otherwise a short
		 * handshake will be initiated.
		 */
		Record encryptedMessage = null;
		Handshaker handshaker = null;

		if (session == null) {
			// no session with endpoint available, create new empty session,
			// start fresh handshake
			session = new DTLSSession(true);
			dtlsSessions.put(peerAddress.toString(), session);
			handshaker = new ClientHandshaker(peerAddress, message, session);

		} else {

			if (session.isActive()) {
				// session to peer is active, send encrypted message
				DTLSMessage fragment = new ApplicationMessage(message.getBytes());
				encryptedMessage = new Record(ContentType.APPLICATION_DATA, session.getWriteEpoch(), session.getSequenceNumber(), fragment, session);

			/*} else if (message.getRetransmissioned() > 0) {
				// TODO when message retransmitted from TransactionLayer: what to do?
				return;*/
				
			} else {
				// try resuming session
				handshaker = new ResumingClientHandshaker(peerAddress, message, session);
			}
		}
		
		DTLSFlight flight = new DTLSFlight();
		// the CoAP message can not be encrypted since no session with peer
		// available, start DTLS handshake protocol
		if (handshaker != null) {
			// get starting handshake message
			handshakers.put(peerAddress.toString(), handshaker);

			flight = handshaker.getStartHandshakeMessage();
			flights.put(peerAddress.toString(), flight);
			scheduleRetransmission(flight);
		}
		
		// the CoAP message has been encrypted and can be sent to the peer
		if (encryptedMessage != null) {
			flight.addMessage(encryptedMessage);
		}
		
		flight.setPeerAddress(peerAddress);
		flight.setSession(session);
		sendFlight(flight);
	}
	
	/**
	 * Searches through all stored sessions and returns that session which
	 * matches the session identifier or <code>null</code> if no such session
	 * available. This method is used when the server receives a
	 * {@link ClientHello} containing a session identifier indicating that the
	 * client wants to resume a previous session. If a matching session is
	 * found, the server will resume the session with a abbreviated handshake,
	 * otherwise a full handshake (with new session identifier in
	 * {@link ServerHello}) is conducted.
	 * 
	 * @param sessionID
	 *            the client's session identifier.
	 * @return the session which matches the session identifier or
	 *         <code>null</code> if no such session exists.
	 */
	private DTLSSession getSessionByIdentifier(byte[] sessionID) {
		if (sessionID == null) {
			return null;
		}
		
//		for (Entry<String, DTLSSession> entry : dtlsSessions.entrySet()) {
//			// FIXME session identifiers may not be set, when the handshake failed after the initial message
//			// these sessions must be deleted when this happens
//			try {
//				byte[] id = entry.getValue().getSessionIdentifier().getSessionId();
//				if (Arrays.equals(sessionID, id)) {
//					return entry.getValue();
//				}
//			} catch (Exception e) {
//				continue;
//			}
//		}
		
		for (DTLSSession session:dtlsSessions.values()) {
			try {
				byte[] id = session.getSessionIdentifier().getSessionId();
				if (Arrays.equals(sessionID, id)) {
					return session;
				}
			} catch (Exception e) {
				continue;
			}
		}
		
		return null;
	}
	
	private void sendFlight(DTLSFlight flight) {
		byte[] payload = new byte[] {};
		// the overhead for the record header (13 bytes) and the handshake
		// header (12 bytes) is 25 bytes
//		int maxPayloadSize = Properties.std.getInt("MAX_FRAGMENT_LENGTH") + 25;
		int maxPayloadSize = max_fragment_length;
		
		// put as many records into one datagram as allowed by the block size
		List<DatagramPacket> datagrams = new ArrayList<DatagramPacket>();

		for (Record record : flight.getMessages()) {
			if (flight.getTries() > 0) {
				// adjust the record sequence number
				int epoch = record.getEpoch();
				record.setSequenceNumber(flight.getSession().getSequenceNumber(epoch));
			}
			
			byte[] recordBytes = record.toByteArray();
			if (payload.length + recordBytes.length > maxPayloadSize) {
				// can't add the next record, send current payload as datagram
				DatagramPacket datagram = new DatagramPacket(payload, payload.length, flight.getPeerAddress().getAddress(), flight.getPeerAddress().getPort());
				datagrams.add(datagram);
				payload = new byte[] {};
			}

			// retrieve payload
			payload = ByteArrayUtils.concatenate(payload, recordBytes);
		}
		DatagramPacket datagram = new DatagramPacket(payload, payload.length, flight.getPeerAddress().getAddress(), flight.getPeerAddress().getPort());
		datagrams.add(datagram);

		// send it over the UDP socket
		try {
			for (DatagramPacket datagramPacket : datagrams) {
//				LOGGER.log(Level.INFO, "do send to "+datagramPacket.getPort(), new RuntimeException());
				socket.send(datagramPacket);
			}
			
		} catch (IOException e) {
//			LOGGER.severe("Could not send the datagram: " + e.getMessage());
//			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "Could not send the datagram", e);
		}
	}
	
	private void handleTimeout(DTLSFlight flight) {

		// TODO Martin: Isn't this supposed to be done in the reliability layer? 
		final int max = max_retransmitt;

		// check if limit of retransmissions reached
		if (flight.getTries() < max) {

			flight.incrementTries();

			sendFlight(flight);

			// schedule next retransmission
			scheduleRetransmission(flight);

		} else {
			LOGGER.info("Maximum retransmissions reached.");
		}
	}

	private void scheduleRetransmission(DTLSFlight flight) {

		// cancel existing schedule (if any)
		if (flight.getRetransmitTask() != null) {
			flight.getRetransmitTask().cancel();
		}

		// create new retransmission task
		flight.setRetransmitTask(new RetransmitTask(flight));

		// calculate timeout using exponential back-off
		if (flight.getTimeout() == 0) {
			// use initial timeout
			flight.setTimeout(retransmission_timeout);
		} else {
			// double timeout
			flight.incrementTimeout();
		}

		// schedule retransmission task
		timer.schedule(flight.getRetransmitTask(), flight.getTimeout());
	}
	
	/**
	 * Cancels the retransmission timer of the previous flight (if available).
	 * 
	 * @param peerAddress
	 *            the peer's address.
	 */
	private void cancelPreviousFlight(EndpointAddress peerAddress) {
		DTLSFlight previousFlight = flights.get(peerAddress.toString());
		if (previousFlight != null) {
			previousFlight.getRetransmitTask().cancel();
			previousFlight.setRetransmitTask(null);
			flights.remove(peerAddress.toString());
		}
	}

	@Override
	public String getName() {
		return "DTLS";
	}
	
	private class RetransmitTask extends TimerTask {

		private DTLSFlight flight;

		RetransmitTask(DTLSFlight flight) {
			this.flight = flight;
		}

		@Override
		public void run() {
			handleTimeout(flight);
		}
	}

}

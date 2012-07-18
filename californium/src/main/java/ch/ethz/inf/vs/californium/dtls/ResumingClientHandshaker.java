package ch.ethz.inf.vs.californium.dtls;

import java.security.MessageDigest;
import java.security.SecureRandom;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;

public class ResumingClientHandshaker extends ClientHandshaker {

	public ResumingClientHandshaker(EndpointAddress endpointAddress, Message message, DTLSSession session) {
		super(endpointAddress, message, session);
	}

	@Override
	public synchronized DTLSFlight processMessage(Record record) {
		if (lastFlight != null) {
			// we already sent the last flight, but the client did not receive
			// it, since we received its finished message again, so we
			// retransmit our last flight
			LOG.info("Received server's finished message again, retransmit the last flight.");
			return lastFlight;
		}

		DTLSFlight flight = null;

		if (!processMessageNext(record)) {
			return null;
		}

		switch (record.getType()) {
		case ALERT:
			record.getFragment();
			break;

		case CHANGE_CIPHER_SPEC:
			record.getFragment();
			setCurrentReadState();
			session.incrementReadEpoch();
			break;

		case HANDSHAKE:
			HandshakeMessage fragment = (HandshakeMessage) record.getFragment();
			switch (fragment.getMessageType()) {

			case SERVER_HELLO:
				// TODO if server's session ID does not match, make full handshake
				serverHello = (ServerHello) fragment;
				break;

			case FINISHED:
				flight = receivedServerFinished((Finished) fragment);
				break;

			default:
				LOG.severe("Client received not supported resuming handshake message:\n" + fragment.toString());
				break;
			}
			break;

		default:
			LOG.severe("Client received not supported record:\n" + record.toString());
			break;
		}
		if (flight == null) {
			Record nextMessage = null;
			// check queued message, if it is now their turn
			for (Record queuedMessage : queuedMessages) {
				if (processMessageNext(queuedMessage)) {
					// queuedMessages.remove(queuedMessage);
					nextMessage = queuedMessage;
				}
			}
			if (nextMessage != null) {
				flight = processMessage(nextMessage);
			}
		}

		LOG.info("DTLS Message processed.");
		System.out.println(record.toString());
		return flight;
	}

	/**
	 * When the client received the server's finished message, it verifies the
	 * finished message and sends the third and last flight of the short
	 * handshake: it contains the ChangeCipherSpec and the Finished message.
	 * 
	 * @param message
	 *            the server's finished message.
	 * @return the last flight of the short handshake (or a Alert if the
	 *         server's finished message can not be verified).
	 */
	private DTLSFlight receivedServerFinished(Finished message) {
		if (lastFlight != null) {
			// the server retransmitted its last flight, therefore retransmit
			// this last flight
			return null;
		}
		DTLSFlight flight = new DTLSFlight();

		// update the handshake hash
		md.update(clientHello.toByteArray());
		md.update(serverHello.toByteArray());

		MessageDigest mdWithServerFinish = null;
		try {
			// the client's finished verify_data must also contain the server's
			// finished message
			mdWithServerFinish = (MessageDigest) md.clone();
		} catch (Exception e) {
			LOG.severe("Clone not supported.");
			e.printStackTrace();
		}
		mdWithServerFinish.update(message.toByteArray());

		// the handshake hash to check the server's verify_data (without the
		// server's finished message included)
		handshakeHash = md.digest();
		if (!message.verifyData(getMasterSecret(), false, handshakeHash)) {

			AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
			flight.addMessage(wrapMessage(alert));
			flight.setRetransmissionNeeded(false);

			return flight;
		}
		
		clientRandom = clientHello.getRandom();
		serverRandom = serverHello.getRandom();
		generateKeys(session.getMasterSecret());

		ChangeCipherSpecMessage changeCipherSpecMessage = new ChangeCipherSpecMessage();
		flight.addMessage(wrapMessage(changeCipherSpecMessage));
		setCurrentWriteState();
		session.incrementWriteEpoch();

		handshakeHash = mdWithServerFinish.digest();
		Finished finished = new Finished(getMasterSecret(), isClient, handshakeHash);
		setSequenceNumber(finished);
		flight.addMessage(wrapMessage(finished));

		state = HandshakeType.FINISHED.getCode();
		session.setActive(true);

		flight.setRetransmissionNeeded(false);
		// store, if we need to retransmit this flight, see
		// http://tools.ietf.org/html/rfc6347#section-4.2.4
		lastFlight = flight;
		return flight;
	}

	@Override
	public DTLSFlight getStartHandshakeMessage() {
		ClientHello message = new ClientHello(new ProtocolVersion(), new SecureRandom(), session);

		message.addCipherSuite(session.getCipherSuite());
		message.addCompressionMethod(session.getCompressionMethod());
		setSequenceNumber(message);

		state = message.getMessageType().getCode();
		clientHello = message;

		DTLSFlight flight = new DTLSFlight();
		flight.addMessage(wrapMessage(message));

		return flight;
	}

}

package ch.ethz.inf.vs.californium.dtls;

import java.security.MessageDigest;
import java.security.SecureRandom;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertDescription;
import ch.ethz.inf.vs.californium.dtls.AlertMessage.AlertLevel;

public class ResumingServerHandshaker extends ServerHandshaker {
	
	byte[] handshakeHash;

	public ResumingServerHandshaker(EndpointAddress endpointAddress, DTLSSession session) {
		super(endpointAddress, session);
		setSessionToResume(session);
	}
	
	/**
	 * Resets the state of a session, such that it can be used to resume it.
	 * 
	 * @param session
	 *            the session to be resumed.
	 */
	private void setSessionToResume(DTLSSession session) {
		session.setActive(false);
		session.setWriteEpoch(0);
		session.setReadEpoch(0);
	}
	
	@Override
	public synchronized DTLSFlight processMessage(Record record) {
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
			case CLIENT_HELLO:
				flight = receivedClientHello((ClientHello) fragment);
				break;

			case FINISHED:
				flight = receivedClientFinished((Finished) fragment);
				break;

			default:
				LOG.severe("Server received not supported resuming handshake message:\n" + fragment.toString());
				break;
			}

			break;

		default:
			LOG.severe("Server received not supported record:\n" + record.toString());
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
	
	private DTLSFlight receivedClientHello(ClientHello message) {
		DTLSFlight flight = new DTLSFlight();
		clientHello = message;
		
		md.update(clientHello.toByteArray());

		clientRandom = clientHello.getRandom();
		serverRandom = new Random(new SecureRandom());

		ServerHello serverHello = new ServerHello(clientHello.getClientVersion(), serverRandom, session.getSessionIdentifier(), session.getCipherSuite(), session.getCompressionMethod(), null);
		setSequenceNumber(serverHello);
		flight.addMessage(wrapMessage(serverHello));
		md.update(serverHello.toByteArray());

		generateKeys(session.getMasterSecret());

		ChangeCipherSpecMessage changeCipherSpecMessage = new ChangeCipherSpecMessage();
		flight.addMessage(wrapMessage(changeCipherSpecMessage));
		setCurrentWriteState();
		session.incrementWriteEpoch();

		MessageDigest mdWithServerFinished = null;
		try {
			mdWithServerFinished = (MessageDigest) md.clone();
		} catch (Exception e) {
			LOG.severe("Clone not supported.");
			e.printStackTrace();
		}

		handshakeHash = md.digest();
		Finished finished = new Finished(getMasterSecret(), isClient, handshakeHash);
		setSequenceNumber(finished);
		flight.addMessage(wrapMessage(finished));

		mdWithServerFinished.update(finished.toByteArray());
		handshakeHash = mdWithServerFinished.digest();
			
		return flight;
	}
	
	private DTLSFlight receivedClientFinished(Finished message) {
		
		
		DTLSFlight flight = new DTLSFlight();
		clientFinished = message;
		
		if (!message.verifyData(getMasterSecret(), false, handshakeHash)) {

			AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.HANDSHAKE_FAILURE);
			flight.addMessage(wrapMessage(alert));
			flight.setRetransmissionNeeded(false);

			return flight;
		}
		
		return flight;

	}

}

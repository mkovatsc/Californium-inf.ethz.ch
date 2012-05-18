package ch.ethz.inf.vs.californium.dtls;

/**
 * This message is always sent by the client. It MUST immediately follow the
 * client certificate message, if it is sent. Otherwise, it MUST be the first
 * message sent by the client after it receives the {@link ServerHelloDone}
 * message.
 * 
 * @author Stefan Jucker
 * 
 */
public abstract class ClientKeyExchange extends HandshakeMessage {

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.CLIENT_KEY_EXCHANGE;
	}

}

package ch.ethz.inf.vs.californium.dtls;

/**
 * This message will be sent immediately after the server
 * {@link CertificateMessage} (or the {@link ServerHello} message, if this is an
 * anonymous negotiation).
 * 
 * @author Stefan Jucker
 * 
 */
public abstract class ServerKeyExchange extends HandshakeMessage {

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.SERVER_KEY_EXCHANGE;
	}
}

package ch.ethz.inf.vs.californium.dtls;

/**
 * The ServerHelloDone message is sent by the server to indicate the end of the
 * {@link ServerHello} and associated messages. After sending this message, the server
 * will wait for a client response.
 * 
 * @author Stefan Jucker
 * 
 */
public class ServerHelloDone extends HandshakeMessage {

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.SERVER_HELLO_DONE;
	}

	@Override
	public int getMessageLength() {
		return 0;
	}

}

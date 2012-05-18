package ch.ethz.inf.vs.californium.dtls;

/**
 * HelloRequest is a simple notification that the client should begin the
 * negotiation process anew. In response, the client should send a
 * {@link ClientHello} message when convenient. This message is not intended to
 * establish which side is the client or server but merely to initiate a new
 * negotiation.
 * 
 * @author Stefan Jucker
 */
public class HelloRequest extends HandshakeMessage {
	
	public HelloRequest() {
		// do nothing
	}
	
	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.HELLO_REQUEST;
	}

	@Override
	public int getMessageLength() {
		return 0;
	}

}

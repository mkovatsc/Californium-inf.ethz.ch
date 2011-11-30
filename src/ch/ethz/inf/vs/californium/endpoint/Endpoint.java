package ch.ethz.inf.vs.californium.endpoint;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.*;


public abstract class Endpoint implements MessageReceiver, MessageHandler {

	public abstract void execute(Request request) throws IOException;

	public int resourceCount() {
		return rootResource != null ? rootResource.subResourceCount() + 1 : 0;
	}

	@Override
	public void receiveMessage(Message msg) {
		msg.handleBy(this);
	}
	
	public int port() {
		return communicator != null ? communicator.port() : -1;
	}

	protected Communicator communicator;
	protected Resource rootResource;

}

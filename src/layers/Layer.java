package layers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import coap.Message;
import coap.MessageReceiver;

public abstract class Layer implements MessageReceiver {

	public void sendMessage(Message msg) throws IOException {

		if (msg != null) {
			doSendMessage(msg);
			++numMessagesSent;
		}
	}

	@Override
	public void receiveMessage(Message msg) {

		if (msg != null) {
			++numMessagesReceived;
			doReceiveMessage(msg);
		}
	}

	protected abstract void doSendMessage(Message msg) throws IOException;

	protected abstract void doReceiveMessage(Message msg);

	protected void deliverMessage(Message msg) {

		// pass message to registered receivers
		if (receivers != null) {
			for (MessageReceiver receiver : receivers) {
				receiver.receiveMessage(msg);
			}
		}
	}

	public void registerReceiver(MessageReceiver receiver) {

		// check for valid receiver
		if (receiver != null && receiver != this) {

			// lazy creation of receiver list
			if (receivers == null) {
				receivers = new ArrayList<MessageReceiver>();
			}

			// add receiver to list
			receivers.add(receiver);
		}
	}

	public void unregisterReceiver(MessageReceiver receiver) {

		// remove receiver from list
		if (receivers != null) {
			receivers.remove(receiver);
		}
	}

	public int getNumMessagesSent() {
		return numMessagesSent;
	}

	public int getNumMessagesReceived() {
		return numMessagesReceived;
	}

	private List<MessageReceiver> receivers;
	private int numMessagesSent;
	private int numMessagesReceived;

}

package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.EndpointAddress;

public class DTLSFlight {
	private List<Record> messages;
	public EndpointAddress peerAddress;
	public DTLSSession session;
	public int tries;
	public int timeout;
	public boolean needsRetransmission = true;
	public TimerTask retransmitTask;
	
	public DTLSFlight() {
		this.messages = new ArrayList<Record>();
		this.tries = 0;
		this.timeout = 0;
	}
	
	public void addMessage(Record message) {
		messages.add(message);
	}

	public List<Record> getMessages() {
		return messages;
	}
	
}

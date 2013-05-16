package ch.inf.vs.californium.network;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class EndpointInformation {

	private final InetAddress address;
	
	private final int port;
	
	private final AtomicInteger currentMID = new AtomicInteger();
	
	public EndpointInformation(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	public int getNewMessageID() {
		return currentMID.getAndIncrement() & 0xFFFF; // 16-bit MID
	}
	
	@Override
	public int hashCode() {
		return address.hashCode() * 70039 + port;
	}
	
}

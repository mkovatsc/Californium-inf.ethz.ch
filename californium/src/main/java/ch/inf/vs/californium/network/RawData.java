package ch.inf.vs.californium.network;

import java.net.InetAddress;

/**
 * Serves as container for the primitive bytes we retrieve or send over a
 * connector.
 */
public class RawData {

	private final byte[] bytes;
	
	private InetAddress address;
	private int port;
	
	public RawData(byte[] bytes) {
		if (bytes == null)
			throw new NullPointerException();
		this.bytes = bytes;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public int getSize() {
		return bytes.length;
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}

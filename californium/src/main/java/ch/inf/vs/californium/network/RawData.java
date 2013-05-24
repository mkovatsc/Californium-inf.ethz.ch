package ch.inf.vs.californium.network;

import java.net.InetAddress;

/**
 * Serves as container for the primitive bytes we retrieve or send over a
 * connector. The RawData consists of the serialized message and the source or
 * destination address and port.
 */
public class RawData {

	/** The serialized message. */
	private final byte[] bytes;
	
	/** The address. */
	private InetAddress address;
	
	/** The port. */
	private int port;
	
	/**
	 * Instantiates a new raw data.
	 *
	 * @param bytes the bytes
	 */
	public RawData(byte[] bytes) {
		this(bytes, null, 0);
	}
	
	/**
	 * Instantiates a new raw data.
	 *
	 * @param bytes the bytes
	 * @param address the address
	 * @param port the port
	 */
	public RawData(byte[] bytes, InetAddress address, int port) {
		if (bytes == null)
			throw new NullPointerException();
		this.bytes = bytes;
		this.address = address;
		this.port = port;
	}
	
	/**
	 * Gets the serialized message.
	 *
	 * @return the bytes
	 */
	public byte[] getBytes() {
		return bytes;
	}
	
	/**
	 * Gets the length of the serialized message
	 *
	 * @return the size
	 */
	public int getSize() {
		return bytes.length;
	}

	/**
	 * Gets the address.
	 *
	 * @return the address
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * Sets the address.
	 *
	 * @param address the new address
	 */
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * Gets the port.
	 *
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Sets the port.
	 *
	 * @param port the new port
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * Gets the address as {@link EndpointAddress}.
	 *
	 * @return the endpoint address
	 */
	public EndpointAddress getEndpointAddress() {
		return new EndpointAddress(address, port);
	}
}

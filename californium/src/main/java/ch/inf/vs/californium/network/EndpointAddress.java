package ch.inf.vs.californium.network;

import java.net.InetAddress;

/**
 * EndpointAddress consists of a network interface address and a port. Together
 * with the security protocol used, an EndpointAddress identifies an endpoint.
 */
public class EndpointAddress {

	/** The address. */
	private InetAddress address;
	
	/*
	 * If the port initially is 0 the system will chose an unbound port and
	 * change this value.
	 */
	/** The port. */
	private int port;

	/**
	 * Instantiates a new endpoint address.
	 *
	 * @param address can be null
	 * @param port the port
	 */
	public EndpointAddress(InetAddress address, int port) {
		this.address = address;
		this.port = port;
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof EndpointAddress))
			return false;
		EndpointAddress ea = (EndpointAddress) o;
		return (address==null && ea.address==null ||
				address!=null && address.equals(ea.address))
				&& port == ea.port;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (address==null) return port;
		else return address.hashCode()*31 + port;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "("+address+":"+port+")";
	}
	
}

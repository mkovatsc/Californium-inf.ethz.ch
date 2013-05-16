package ch.inf.vs.californium.network;

import java.net.InetAddress;

public class EndpointAddress {

	private InetAddress address;
	
	/*
	 * If the port initially is 0 the system will chose an unbound port and
	 * change this value.
	 */
	private int port;

	/**
	 * 
	 * @param address can be null
	 * @param port
	 */
	public EndpointAddress(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}

	public InetAddress getInetAddress() {
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
	
	@Override
	public int hashCode() {
		if (address==null) return port;
		else return address.hashCode()*31 + port;
	}
	
	@Override
	public String toString() {
		return "("+address+":"+port+")";
	}
	
}

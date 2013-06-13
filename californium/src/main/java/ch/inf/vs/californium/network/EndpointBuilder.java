package ch.inf.vs.californium.network;

import java.net.InetAddress;

import ch.inf.vs.californium.network.connector.Connector;
import ch.inf.vs.californium.network.connector.UDPConnector;

public class EndpointBuilder {

	// TODO: Under construction...
	
	private InetAddress address;
	private int port;
	private Connector connector;
	private NetworkConfig config;
	
	public EndpointBuilder() { }
	
	public EndpointBuilder setAddress(InetAddress address) {
		this.address = address;
		return this;
	}
	
	public EndpointBuilder setPort(int port) {
		this.port = port;
		return this;
	}
	
	public EndpointBuilder setConnector(Connector connector) {
		this.connector = connector;
		return this;
	}
	
	public EndpointBuilder setNetworkConfiguration(NetworkConfig config) {
		this.config = config;
		return this;
	}
	
	public Endpoint create() {
		EndpointAddress eaddr = new EndpointAddress(address, port);
		if (config==null) config = new NetworkConfig();
		if (connector==null) connector = new UDPConnector(eaddr);
		return new Endpoint(connector, eaddr, config);
	}
}

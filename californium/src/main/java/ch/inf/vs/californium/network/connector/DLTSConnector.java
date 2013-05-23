package ch.inf.vs.californium.network.connector;

import ch.inf.vs.californium.network.EndpointAddress;

public class DLTSConnector extends ConnectorBase {

	public DLTSConnector(EndpointAddress address) {
		super(address);
	}
	
	@Override
	protected void receive() throws Exception {

	}

	@Override
	protected void send() throws Exception {

	}

	@Override
	public String getName() {
		return "DLTS";
	}

}

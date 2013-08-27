package ch.ethz.inf.vs.californium.resources;

import ch.ethz.inf.vs.californium.network.Exchange;

public interface RequestProcessor {

	public void processRequest(Exchange exchange);
	
}

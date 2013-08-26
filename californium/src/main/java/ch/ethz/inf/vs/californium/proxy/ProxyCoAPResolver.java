package ch.ethz.inf.vs.californium.proxy;

import ch.ethz.inf.vs.californium.network.Exchange;

public interface ProxyCoAPResolver {

	public void forwardRequest(Exchange exchange);
	
}

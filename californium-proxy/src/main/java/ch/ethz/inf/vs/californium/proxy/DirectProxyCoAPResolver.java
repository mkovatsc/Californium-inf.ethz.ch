package ch.ethz.inf.vs.californium.proxy;

import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.resources.proxy.ForwardingResource;

public class DirectProxyCoAPResolver implements ProxyCoAPResolver {

	private final static Logger LOGGER = Logger.getLogger(DirectProxyCoAPResolver.class.getCanonicalName());
	
	private ForwardingResource proxyCoapClientResource;
	
	public DirectProxyCoAPResolver() { }
	
	public DirectProxyCoAPResolver(ForwardingResource proxyCoapClientResource) {
		this.proxyCoapClientResource = proxyCoapClientResource;
	}

	public ForwardingResource getProxyCoapClientResource() {
		return proxyCoapClientResource;
	}

	public void setProxyCoapClientResource(ForwardingResource proxyCoapClientResource) {
		this.proxyCoapClientResource = proxyCoapClientResource;
	}

	@Override
	public void forwardRequest(Exchange exchange) {
		LOGGER.fine("Forward CoAP request to ProxyCoap2Coap: "+exchange.getRequest());
		proxyCoapClientResource.handleRequest(exchange);
	}
}

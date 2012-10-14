package ch.ethz.inf.vs.californium.endpoint;

public class VirtualNode{
	
	protected String endpointIdentifier;
	protected String context;
	protected String domain;
	protected String endpointType;
	
	
	public VirtualNode(String endpointIdentifier, String domain, String endpointType, String context){
		this.endpointIdentifier = endpointIdentifier;
		this.domain = domain;
		this.endpointType = endpointType;
		this.context = context;
	}
	
	public String getEndpointIdentifier() {
		return endpointIdentifier;
	}


	public String getContext() {
		return context;
	}


	public String getDomain() {
		return domain;
	}


	public String getEndpointType() {
		return endpointType;
	}
	
}

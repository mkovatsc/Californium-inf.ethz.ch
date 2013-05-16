package ch.inf.vs.californium.network;

public interface EndpointObserver {

	public void started(Endpoint endpoint);
	
	public void stopped(Endpoint endpoint);
	
	public void destroyed(Endpoint endpoint);
	
}

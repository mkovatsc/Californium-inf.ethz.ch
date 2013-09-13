package ch.ethz.inf.vs.californium.server.resources;

public interface ResourceObserver {

	public void titleChanged(Resource resource, String old);
	
	public void nameChanged(Resource resource, String old);
	
	public void pathChanged(Resource resource, String old);
	
}

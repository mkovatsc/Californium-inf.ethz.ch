package ch.inf.vs.californium.resources;

public interface ResourceObserver {

	public void titleChanged(Resource resource, String old);
	
	public void nameChanged(Resource resource, String old);
	
	public void pathChanged(Resource resource, String old);
	
}

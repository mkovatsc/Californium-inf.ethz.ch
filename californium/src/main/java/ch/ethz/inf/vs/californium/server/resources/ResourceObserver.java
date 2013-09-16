package ch.ethz.inf.vs.californium.server.resources;

import ch.ethz.inf.vs.californium.observe.ObserveRelation;

/**
 * This interface implements Java's observe design pattern for a resource.
 * Notice that it has nothing to do with CoAP's observe relations. A
 * ResourceObserver can be added to a {@link Resource} which should invoke the
 * specified methods if a corresponding event occurs.
 */
public interface ResourceObserver {

	/**
	 * Invoked when the name of the resource has changed.
	 * 
	 * @param old the old name
	 */
	public void changedName(String old);
	
	/**
	 * Invoked when the Path of the resource has changed.
	 *
	 * @param old the old path
	 */
	public void changedPath(String old);
	
	/**
	 *Invoked when a child has been added to the resource.
	 *
	 * @param child the child
	 */
	public void addedChild(Resource child);
	
	/**
	 * Invoked when a child has been removed from the resource.
	 *
	 * @param child the child
	 */
	public void removedChild(Resource child);
	
	/**
	 * Invoked when a CoAP observe relation has been established with the
	 * resource.
	 * 
	 * @param relation the relation
	 */
	public void addedObserveRelation(ObserveRelation relation);
	
	/**
	 * Invoked when a CoAP observe relation has been canceled with the
	 * resource
	 *  
	 * @param relation the relation
	 */
	public void removedObserveRelation(ObserveRelation relation);
}

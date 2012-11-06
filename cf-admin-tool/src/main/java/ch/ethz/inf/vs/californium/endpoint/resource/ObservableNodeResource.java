package ch.ethz.inf.vs.californium.endpoint.resource;

import java.util.Date;
import java.util.LinkedList;

import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;

public class ObservableNodeResource extends LocalResource{
	
	
	private int receivedActual;
	private int receivedIdeal;
	private Date lastHeardOf;
	private ObserveTopResource parent = null;
	private String context;

	public ObservableNodeResource(String resourceIdentifier,String con ,ObserveTopResource top ) {
		super(resourceIdentifier, true);
		context=con;
		receivedActual=0;
		receivedIdeal=0;
		lastHeardOf = new Date(0);
		parent = top;
		add(new LastHeardOfResource(this));
		add(new LossRateResource(this));
		// TODO Auto-generated constructor stub
	}
	
	
	public void resetLastHeardOf(){
		lastHeardOf = new Date(0);
	}
	
	public void resetLossRate(){
		receivedActual=0;
		receivedIdeal=0;
	}
	
	public Date getLastHeardOf(){
		return lastHeardOf;
	}
	
	public double getLossRate(){
		if (receivedIdeal<0 || receivedActual<10){
			return -1;
		}
		return (double)(receivedIdeal-receivedActual)/(double) receivedIdeal*100;
	}
	
	
	public void setLastHeardOf(){
		if(lastHeardOf.getTime()<new Date().getTime()-600*1000){
			lastHeardOf = new Date();
			LinkedList<Resource> todo = new LinkedList<Resource>();
			todo.addAll(getSubResources());
			while(!todo.isEmpty()){
				Resource next = todo.pop();
				if(next.getClass() == ObservableResource.class){
					((ObservableResource) next).resendObserveRegistration(true);
				}
				todo.addAll(next.getSubResources());
			}
		}
		else{
			lastHeardOf = new Date();
		}
	}
	
	public void receivedIdealAdd(int p){
		receivedIdeal+=p;
	}
	
	public void receivedActualAdd(int p){
		receivedActual+=p;
	}
	
	public boolean hasPersisting(){
		return parent.hasPersisting();
	}
	
	public String getPsUri(){
		return parent.getPsUri();
	}
	
	public String getContext(){
		return context;
	}
	
}

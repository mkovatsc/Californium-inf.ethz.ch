package ch.ethz.inf.vs.californium.endpoint.resource;

import java.util.Date;

import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;

public class ObservableNodeResource extends LocalResource{
	
	
	private int receivedActual;
	private int receivedIdeal;
	private Date lastHeardOf;
	private ObserveTopResource parent = null;

	public ObservableNodeResource(String resourceIdentifier, ObserveTopResource top ) {
		super(resourceIdentifier, true);
		receivedActual=0;
		receivedIdeal=0;
		lastHeardOf = new Date(0);
		parent = top;
		add(new LastHeardOfResource(this));
		add(new LossRateResource(this));
		// TODO Auto-generated constructor stub
	}
	
	
	
	
	public Date getLastHeardOf(){
		return lastHeardOf;
	}
	
	public double getLossRate(){
		if (receivedIdeal<1){
			return -1;
		}
		return (double)(receivedIdeal-receivedActual)/(double) receivedIdeal*100;
	}
	
	
	public void setLastHeardOf(){
		lastHeardOf = new Date();
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
	
}

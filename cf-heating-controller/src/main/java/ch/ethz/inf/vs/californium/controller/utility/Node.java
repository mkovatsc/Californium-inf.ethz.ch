package ch.ethz.inf.vs.californium.controller.utility;

import java.util.Date;
import java.util.HashMap;

import ch.ethz.inf.vs.californium.coap.GETRequest;

public class Node {
	
	
	private String address;
	private String identifier;
	
	private Date receivedLastHeatBeat;
	
	
	
	public Node(String address, String id){
		this.setIdentifier(id);
		this.setAddress(address);
		receivedLastHeatBeat = new Date(0);
		
	}



	public String getAddress() {
		return address;
	}



	public void setAddress(String address) {
		this.address = address;
	}



	public String getIdentifier() {
		return identifier;
	}



	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}




	public Date getReceivedLastHeatBeat() {
		return receivedLastHeatBeat;
	}



	public void setReceivedLastHeatBeat() {
		this.receivedLastHeatBeat = new Date();
	}

}

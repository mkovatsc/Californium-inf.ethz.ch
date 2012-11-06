package ch.ethz.inf.vs.californium.controller.utility;

import java.io.IOException;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.controller.Controller;

public class Node {
	
	private static Logger logger = Logger.getLogger(Node.class);

	
	private String address;
	private String identifier;
	
	private Date receivedLastHeatBeat;
	private HeartBeatTask task;
	private Timer manual;
		
	
	public Node(String address, String id, String wi){
		this.setIdentifier(id);
		this.setAddress(address);
		receivedLastHeatBeat = new Date(0);
		task=null;
		manual=new Timer();
		manual.schedule(task = new HeartBeatTask(this, wi), 30*1000, 300*1000);
	
		
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



	public void setReceivedLastHeatBeat(Date date) {
		this.receivedLastHeatBeat = date;
	}

}

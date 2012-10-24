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
	private HeartBeatResource heartBeatResource;
	private Timer manual;
	
	
	public Node(String address, String id){
		this.setIdentifier(id);
		this.setAddress(address);
		receivedLastHeatBeat = new Date(0);
		heartBeatResource=null;
		manual=new Timer();
		createHeartBeatResource();
		
	}

	public void createHeartBeatResource(){
	
		GETRequest rdLookup = new GETRequest();
		rdLookup.setURI("coap://"+Properties.std.getStr("RD_ADDRESS")+"/rd-lookup/res");
				
		rdLookup.addOption(new Option("rt=heartbeat*", OptionNumberRegistry.URI_QUERY));
		rdLookup.addOption(new Option("ep=\""+this.getIdentifier()+"\"", OptionNumberRegistry.URI_QUERY));

		rdLookup.enableResponseQueue(true);
		String resourcePath = "";
		Response rdResponse = null;		
		try {
			rdLookup.execute();
			
			rdResponse = rdLookup.receiveResponse();
			if(rdResponse !=null && rdResponse.getCode() == CodeRegistry.RESP_CONTENT){
				String uri = "";
				String payload = rdResponse.getPayloadString();
				if(payload.matches("<coap://.*>.*")){
					uri = payload.substring(payload.indexOf("<")+1,payload.indexOf(">"));
					String completePath = uri.substring(uri.indexOf("//")+2);
					resourcePath = completePath.substring(completePath.indexOf("/"));
				}
				
			}
		}
		catch(IOException e){
			logger.error("Retrieve HeartBeat for " + getAddress());
		}
		catch(InterruptedException e){
			logger.error("Retrieve HeartBeat for " + getAddress());
		}
		if(!resourcePath.isEmpty()){
			this.heartBeatResource = new HeartBeatResource(resourcePath, this);
		}
		else{
			logger.warn(getIdentifier()+ " does not support HeartBeats, do it manually");
			manual.schedule(new HeartBeatTaskManual(this), 30*1000, 300*1000);
		}
		
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

	public void restartHeartBeat(){
		if(heartBeatResource!=null && (receivedLastHeatBeat.getTime() <  new Date().getTime()-900*1000)){
			heartBeatResource.register();
		}
	}
}

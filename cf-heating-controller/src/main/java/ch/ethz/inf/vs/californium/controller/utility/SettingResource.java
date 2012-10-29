package ch.ethz.inf.vs.californium.controller.utility;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.controller.Controller;

public class SettingResource{
	
	private static Logger logger = Logger.getLogger(SettingResource.class);

	private String path;
	private String newestValue;
	private String context;
	private String type;
	private Date timestamp;
	private HashMap<String,String> tags;
	private boolean alive;
	private Controller controller;

	private GETRequest getRequest;
	private PUTRequest putRequest;
	
	public SettingResource(String path, String context, String type, Controller controller) {
		
		getRequest = new GETRequest();
		getRequest.setURI(context + path);
		getRequest.enableResponseQueue(true);
		newestValue="";
		alive=true;
		this.path=path;
		this.type=type;
		this.context=context;
		this.controller = controller;
		
		putRequest = new PUTRequest();
		putRequest.setURI(context + path);
		putRequest.enableResponseQueue(true);
		
		retrieveTags();
		getSettings();
			
	}
	
	public void retrieveTags(){
		GETRequest tagGetter = new GETRequest();
		tagGetter.addOption(new Option("res=\""+path+"\"", OptionNumberRegistry.URI_QUERY));
		tagGetter.addOption(new Option("ep=\""+controller.getIdFromContext(context)+"\"", OptionNumberRegistry.URI_QUERY));
		tagGetter.setURI(controller.getRdUriBase()+"/tags");
		tagGetter.enableResponseQueue(true);
		Response tagResponse = null;
		try {
			tagGetter.execute();
			tagResponse = tagGetter.receiveResponse();
		} catch (IOException e) {
			logger.error("retrieving Tags for " + context+path);
		} catch (InterruptedException e) {
			logger.error("Retrieving Tags for " + context+path);
		}
		if(tagResponse != null && tagResponse.getCode() == CodeRegistry.RESP_CONTENT){
			String payload = tagResponse.getPayloadString();
			tags.clear();
			for(String tag : payload.split(",")){
				if(tag.isEmpty()){continue;}
				tags.put(tag.substring(0,tag.indexOf("=")),tag.substring(tag.indexOf("=")+1));
			}
			if(tags.containsKey("room")){
				controller.addRoom(tags.get("room"));
			}
			
		}
		
	}
	
	
	
	public String getSettings(){
		Response getResponse = null;
		try {
			getRequest.execute();
			getResponse = getRequest.receiveResponse();
			
		} catch (IOException e) {
			logger.error("Retrieving settings at " + context+path);
		} catch (InterruptedException e) {
			logger.error("Retrieving settings at " + context+path);
		}
		if(getResponse!=null && getResponse.getCode()==CodeRegistry.RESP_CONTENT){
			newestValue = getResponse.getPayloadString();
			timestamp = new Date();
			alive=true;
		}
		
		return newestValue;
		
	}
	
	
	public boolean updateSettings(String value){
		Response putResponse = null;
		putRequest.setPayload(value);
		try {
			putRequest.execute();
			putResponse = putRequest.receiveResponse();
			
		} catch (IOException e) {
			logger.error("Change settings: "+context+path);
			return false;
		} catch (InterruptedException e) {
			logger.error("Change settings: "+context+path);
			return false;
		}
		if(putResponse!=null && ( putResponse.getCode()==CodeRegistry.RESP_CHANGED 
				|| putResponse.getCode()==CodeRegistry.RESP_CONTENT || putResponse.getCode()==CodeRegistry.RESP_VALID)){
			newestValue = value;
			timestamp = new Date();
			alive=true;
			return true;
		}
		
		return false;
	}

	
	public boolean containsExactTag(String name, String value){
		if (tags.containsKey(name.toLowerCase())){
			return tags.get(name.toLowerCase()).equals(value.toLowerCase());
		}
		return false;
	}
	
	public boolean containsTag(String name){
		return tags.containsKey(name.toLowerCase());
	}
	
	public String getTag(String name){
		return tags.get(name.toLowerCase());
	}
	
	public String getNewestValue(){
		return newestValue;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public String getContext() {
		return context;
	}
	
	public boolean isAlive(){
		return alive;
	}

	public void setAlive(boolean alive){
		this.alive = alive;
	}
}

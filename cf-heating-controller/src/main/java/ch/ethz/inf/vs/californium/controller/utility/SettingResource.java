package ch.ethz.inf.vs.californium.controller.utility;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

public class SettingResource{
	
	private static Logger logger = Logger.getLogger(SettingResource.class);

	private String path;
	private String newestValue;
	private String context;
	private String type;
	private Date timestamp;
	private boolean alive;

	private GETRequest getRequest;
	private PUTRequest putRequest;
	
	public SettingResource(String path, String context, String type) {
		
		getRequest = new GETRequest();
		getRequest.setURI(context + path);
		getRequest.enableResponseQueue(true);
		newestValue="";
		alive=true;
		this.path=path;
		this.type=type;
		this.context=context;
		
		putRequest = new PUTRequest();
		putRequest.setURI(context + path);
		putRequest.enableResponseQueue(true);
		
		getSettings();
			
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

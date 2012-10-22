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
	private String lastValue;
	private String context;
	private String type;
	private Date timestamp;

	private GETRequest getRequest;
	private PUTRequest putRequest;
	
	public SettingResource(String path, String context, String type) {
		
		getRequest = new GETRequest();
		getRequest.setURI(context + path);
		getRequest.enableResponseQueue(true);
		lastValue="";
		
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
			lastValue = getResponse.getPayloadString();
			timestamp = new Date();
		}
	
		return lastValue;
		
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
		if(putResponse!=null && putResponse.getCode()==CodeRegistry.RESP_CHANGED){
			lastValue = value;
			timestamp = new Date();
			return true;
		}
		return false;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	

}

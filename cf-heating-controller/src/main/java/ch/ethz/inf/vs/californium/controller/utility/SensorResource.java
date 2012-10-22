package ch.ethz.inf.vs.californium.controller.utility;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.controller.Controller;

public class SensorResource{
	
	private static Logger logger = Logger.getLogger(SensorResource.class);
	
	private String path;
	private String lastValue;
	private String context;
	private Date timestamp; 
	
	private String type;
	private boolean observable;
	
	private HashSet<String> tags;

	private ResponseHandler receiver;
	private Controller controller;
	
	
	public SensorResource(String path, String context, String type, boolean observable, Controller controller){
		setPath(path);
		setType(type);
		this.context = context;
		this.observable = observable;
		receiver = new GETReceiver(this);
		this.controller = controller;
		
		retrieveTags();
		register();	
	
	}
	
	public void retrieveTags(){
		GETRequest tagGetter = new GETRequest();
		tagGetter.addOption(new Option("res="+path, OptionNumberRegistry.URI_QUERY));
		tagGetter.addOption(new Option("ep="+controller.getIdFromContext(context), OptionNumberRegistry.URI_QUERY));
		tagGetter.setURI(controller.getRdUriBase()+"/tags");
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
			Collections.addAll(tags,payload.split(","));
		}
		
	}
	
	public boolean hasTag(String name){
		return tags.contains(name.toLowerCase());
	}
	
	public void register(){
		GETRequest getRequest = new GETRequest();
		getRequest.setURI(context + path);
		if (observable){
			getRequest.addOption(new Option(0, OptionNumberRegistry.OBSERVE));
			getRequest.setToken(TokenManager.getInstance().acquireToken());
		}
		getRequest.registerResponseHandler(receiver);
		tags = new HashSet<String>();
		try {
			getRequest.execute();
		} catch (IOException e) {
			logger.error("Register at " + context+path);
		}
	}
	
	
	private class GETReceiver implements ResponseHandler{
		
		private SensorResource parent;
		
		public GETReceiver(SensorResource parent){
			this.parent = parent;
		}

		@Override
		public void handleResponse(Response response) {
			if (response.getCode() == CodeRegistry.RESP_CONTENT){
				String oldValue = lastValue;
				lastValue = response.getPayloadString();
				parent.timestamp = new Date();
				if(lastValue != oldValue){
					controller.processChange(parent);
				}
			}
		}		
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getLastValue() {
		return lastValue;
	}

	public void setLastValue(String lastValue) {
		this.lastValue = lastValue;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}

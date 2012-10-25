package ch.ethz.inf.vs.californium.controller.utility;

import java.io.IOException;
import java.util.Timer;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

public class HeartBeatResource {
	
	private static Logger logger = Logger.getLogger(HeartBeatResource.class);
	
	private String path;
	private Node node;

	private ResponseHandler heartBeatHandler;
	
	public HeartBeatResource(String path, Node node){
		this.node = node;
		this.path=path;
		this.heartBeatHandler = new HeartBeatReceiver(this.node);
		register();
	}
	
	public void register(){
		GETRequest getRequest = new GETRequest();
		getRequest.setURI(node.getAddress() + path);
		getRequest.addOption(new Option(0, OptionNumberRegistry.OBSERVE));
		getRequest.setToken(TokenManager.getInstance().acquireToken());
		getRequest.registerResponseHandler(heartBeatHandler);
		try {
			getRequest.execute();
		} catch (IOException e) {
			logger.error(node.getAddress() + "Registration: "+e.getMessage());
		}
	}
	
	private class HeartBeatReceiver implements ResponseHandler{

		private Node node;
			
		public HeartBeatReceiver(Node node){
			this.node = node;
		}
		@Override
		public void handleResponse(Response response) {
			node.setReceivedLastHeatBeat();
		}
		
	}


}

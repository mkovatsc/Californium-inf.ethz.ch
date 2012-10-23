package ch.ethz.inf.vs.californium.controller.utility;

import java.io.IOException;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

public class HeartBeatTaskManual extends TimerTask {

	
	private static Logger logger = Logger.getLogger(HeartBeatTaskManual.class);
	
	private Node main;
	
	public HeartBeatTaskManual(Node node){
		this.main = node;
	}
	
	@Override
	public void run() {
		GETRequest rdLookup = new GETRequest();
		rdLookup.setURI(main.getAddress()+"/.well-known/core");
		rdLookup.addOption(new Option("rt=heartbeat*", OptionNumberRegistry.URI_QUERY));
		
		rdLookup.enableResponseQueue(true);
		Response rdResponse = null;		
		try {
			rdLookup.execute();
			
			rdResponse = rdLookup.receiveResponse();
			if(rdResponse !=null){
				main.getReceivedLastHeatBeat();
			}
		}
		catch(IOException e){
			logger.warn("Can not send manual HeartBeat for " + main.getAddress());
		}
		catch(InterruptedException e){
			logger.error("Can not retrieve manual HeartBeat for " + main.getAddress());
		}
		
	}

}

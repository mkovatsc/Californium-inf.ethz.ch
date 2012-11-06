package ch.ethz.inf.vs.californium.controller.utility;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

public class HeartBeatTask extends TimerTask {

	
	private static Logger logger = Logger.getLogger(HeartBeatTask.class);
	
	private Node main;
	private String wiResource;
	
	public HeartBeatTask(Node node, String wi){
		this.main = node;
		this.wiResource = wi;
	}
	
	@Override
	public void run() {
		GETRequest  heartBeatRequest = new GETRequest();
		heartBeatRequest.setURI(wiResource+"/observable/"+main.getIdentifier()+"/lastheardof");
		heartBeatRequest.enableResponseQueue(true);
		Response response = null;		
		try {
			heartBeatRequest.execute();
			
			response = heartBeatRequest.receiveResponse();
			if(response !=null && response.getCode() == CodeRegistry.RESP_CONTENT){
				String payload = response.getPayloadString();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date last=null;
				last = dateFormat.parse(payload);
				if(last!=null){
					main.setReceivedLastHeatBeat(last);
				}
			}
		}
		catch(IOException e){
			logger.warn("Can not send HeartBeatRequest for " + main.getAddress());
		}
		catch(InterruptedException e){
			logger.error("Can not retrieve HeartBeatRequest for " + main.getAddress());
		} catch (ParseException e) {
			logger.error("Can not parse HeartBeat Date for " + main.getAddress());
		}
		
	}

}

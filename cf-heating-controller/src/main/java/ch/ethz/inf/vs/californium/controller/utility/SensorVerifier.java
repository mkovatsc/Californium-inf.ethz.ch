package ch.ethz.inf.vs.californium.controller.utility;

import java.util.Date;
import java.util.Set;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.controller.Controller;

public class SensorVerifier extends TimerTask{

	private static Logger logger = Logger.getLogger(SensorVerifier.class);
	private Controller main;
	
	public SensorVerifier(Controller controller){
		this.main=controller;		
	}
	
	
	@Override
	public void run() {
		Set<String> addresses = main.getSensors().keySet();
		for(String address : addresses){
			SensorResource sensor = main.getSensors().get(address);
			Date timestamp = sensor.getTimeStamp();
			Node node = main.getNode(sensor.getContext());
			boolean nodeHeartBeat;
			if(node.getReceivedLastHeatBeat().getTime() < new Date().getTime()-3600*1000){
				nodeHeartBeat=false;
			}
			else if (node.getReceivedLastHeatBeat().getTime() < new Date().getTime()-1800*1000){
				logger.warn("Last Heartbeat long ago, reregister: "+sensor.getContext());
				nodeHeartBeat=false;
				node.restartHeartBeat();
			}
			else{
				nodeHeartBeat=true;
			}
			if((timestamp.getTime() < node.getReceivedLastHeatBeat().getTime()-1800*1000) && nodeHeartBeat ){
				sensor.setAlive(false);
				logger.warn("We probably missed events "+sensor.getContext()+sensor.getPath());
				logger.warn("Resubscribe for resource: "+sensor.getContext()+sensor.getPath());
				sensor.register();
			}
		}
	}

}

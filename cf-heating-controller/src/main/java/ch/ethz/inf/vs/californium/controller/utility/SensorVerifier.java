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
			if(timestamp.getTime()<new Date().getTime()-1800*1000){
				sensor.setAlive(false);
				if(main.getNode(sensor.getContext())!=null){
					Node node = main.getNode(sensor.getContext());
					if(node.getReceivedLastHeatBeat().getTime() < new Date().getTime()-1800*1000){
						logger.warn("No Heartbeat, reregister: "+sensor.getContext());
						node.restartHeartBeat();
						logger.warn("Subscribe for resource: "+sensor.getContext()+sensor.getPath());
						sensor.register();
					}
					else{
						logger.warn("We missed events "+sensor.getContext()+sensor.getPath());
						logger.warn("Resubscribe for resource: "+sensor.getContext()+sensor.getPath());
						sensor.register();
					}
				}
				else{
					logger.warn("Resubscribe for resource: "+sensor.getContext()+sensor.getPath());
					sensor.register();
				}
			}
		}
	}

}

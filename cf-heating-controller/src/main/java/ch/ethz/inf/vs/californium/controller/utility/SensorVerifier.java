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
			Node node = main.getNode(sensor.getContext());
			if(node.getReceivedLastHeatBeat().getTime() < new Date().getTime()-3600*1000){
				logger.error("Sensor Dead no Heartbeat: "+sensor.getContext());
				sensor.setAlive(false);
			}
			else if(!sensor.isAlive() || sensor.getTimeStamp().getTime()<node.getReceivedLastHeatBeat().getTime()-7200*1000){
				sensor.register();
			}
		}
	}

}

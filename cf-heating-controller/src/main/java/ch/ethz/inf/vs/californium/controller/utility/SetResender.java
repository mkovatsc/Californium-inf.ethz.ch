package ch.ethz.inf.vs.californium.controller.utility;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.controller.Controller;

public class SetResender extends TimerTask {

	private static Logger logger = Logger.getLogger(SetResender.class);
	
	private Controller main;
	
	public SetResender(Controller controller){
		main = controller;
	}
	
	@Override
	public void run() {
		
		Set<SettingResource> tasks = main.getTasksToDo().keySet();
		for(SettingResource task : tasks){
			if(task.updateSettings(main.getTasksToDo().get(task))){
				logger.info("Task succesfull " +task.getContext()+task.getPath());
				main.getTasksToDo().remove(task);
			}
			else{
				Node node = main.getNodes().get(task.getContext());
				if (node.getReceivedLastHeatBeat().getTime() < new Date().getTime()-3600*1000){
					logger.error("Node is dead, stop trying to update the settings" +node.getIdentifier()+":"+node.getAddress());
					task.setAlive(false);
					main.getTasksToDo().remove(task);
				}
			}
		}
		// TODO Auto-generated method stub

	}

}

package ch.ethz.inf.vs.californium.controller.utility;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.controller.Controller;



public class ScheduleManager extends TimerTask{

	private static Logger logger = Logger.getLogger(ScheduleManager.class);
	
	private Controller main;
	
//	private HashSet<HeatingPoint> dailyEvents;
//	private HashSet<HeatingPoint> weeklyEvents;
	
	private PriorityQueue<HeatingEvent> nextEvent;
	
	
	
	public ScheduleManager(Controller controller){
		this.main = controller;
	}
	
	
	
	@Override
	public void run() {
		
		nextEvent = new PriorityQueue<HeatingEvent>(24, new TimeToStartComparator());
		
		
		
		

		HeatingEvent current = nextEvent.peek();
		if(current!=null){
			main.getTemperatures().put("PREHEAT",current.getTemperature());
		}
		else{
			main.getTemperatures().remove("PREHEAT");
		}
		
	}
	

}

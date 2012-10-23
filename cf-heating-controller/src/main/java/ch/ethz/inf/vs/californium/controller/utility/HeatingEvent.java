package ch.ethz.inf.vs.californium.controller.utility;

import java.util.Calendar;
import java.util.Date;

public class HeatingEvent {
	
	private int hour;
	private int minute;
	private double temperature;
	private long start;
	private long duration;
	private long day;
	
	
	public HeatingEvent(long start, long duration, double temperature){
		this.start = start;
		this.duration = duration;
		this.temperature = temperature;
	}
	
	
	public double getTemperature(){
		return temperature;
	}
	
	public long getStart(){
		return start;
	}
	
}

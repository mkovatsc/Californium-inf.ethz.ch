package ch.ethz.inf.vs.californium.controller.utility;

import java.util.Calendar;
import java.util.Date;

public class HeatingPoint {
	
	private double temperature;
	private long start;

	
	public HeatingPoint(long start, double temperature){
		this.start = start;
		this.temperature = temperature;
	}
	
	
	public double getTemperature(){
		return temperature;
	}
	
	public long getStart(){
		return start;
	}
	
}

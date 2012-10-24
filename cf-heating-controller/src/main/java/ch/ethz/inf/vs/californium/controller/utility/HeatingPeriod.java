package ch.ethz.inf.vs.californium.controller.utility;

import java.util.Calendar;

public class HeatingPeriod {
	
	
	private int startHour;
	private int startMin;
	private int endHour;
	private int endMin;
	private int startDay;
	private int endDay;
	private double temperature;
	
	
	public HeatingPeriod(int startHour, int startMin, int endHour, int endMin, int startDay, int endDay, double temperature){
		this.startHour=startHour;
		this.startMin=startMin;
		this.endHour=endHour;
		this.endMin=endMin;
		this.startDay=startDay;
		this.endDay=endDay;
		this.temperature=temperature;
		
	}
	
	
	
	public double getTemperature(){
		return temperature;
	}
	
	public boolean isActive(int hour, int min, int day){
		if(startDay==7){
			return inTimeDaily(hour,min);
		}
		return inTime(hour,min,day);
	}
	
	private boolean inTimeDaily(int hour, int min){
		int start = startHour*60+startMin;
		int end = endHour*60+endMin;
		int now = hour*60+min;
		if(start <= now && now<= end){
			return true;
		}
		else if((start>end) && (start <= now && now<=24*60 || 0<=now && now<=end )){
			return true;
		}
			
		return false;
	}
	
	private boolean inTime(int hour, int min, int day){
		int start = startDay*60*24+startHour*60+startMin;
		int end = endDay*60*24+endHour*60+endMin;
		int now = day*60*24+hour*60+min;
		if(start <= now && now<= end){
			return true;
		}
		else if((start>end) && (start <= now && now<=24*60*7 || 0<=now && now<=end )){
			return true;
		}
			
		return false;
	}
	
	public int minutesToStart(int hour, int min, int day){
		if(startDay==7){
			return ((startHour*60+startMin)-(hour*60+min)+24*60)%(24*60);
		}
		return ((startDay*24*60+startHour*60+startMin)-(day*24*60+hour*60+min)+7*24*60)%(7*24*60);
	}

}

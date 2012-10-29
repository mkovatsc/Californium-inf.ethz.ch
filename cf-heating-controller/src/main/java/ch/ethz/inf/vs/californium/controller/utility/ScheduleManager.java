package ch.ethz.inf.vs.californium.controller.utility;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.controller.Controller;



public class ScheduleManager extends TimerTask{

	private static Logger logger = Logger.getLogger(ScheduleManager.class);
	
	private static final Pattern daily = Pattern.compile("(\\D+) (\\d+):(\\d+) - (\\d+):(\\d+) ((\\d+)(\\.\\d+){0,1})");
	private static final Pattern weekly = Pattern.compile("(\\D{3}) (\\d+):(\\d+) - (\\D{3}) (\\d+):(\\d+) ((\\d+)(\\.\\d+){0,1})");
	
	private Controller main;
	private double heatingCoefficient=10.0;
	
	 
	private HashSet<HeatingPeriod> periods;
		 
	private PriorityQueue<HeatingPoint> nextEvent;
	
	
	
	public ScheduleManager(Controller controller){
		this.main = controller;
		periods = new HashSet<HeatingPeriod>();
		try{
			FileInputStream fstream = new FileInputStream(Properties.std.getStr("SCHEDULE_FILE"));
			DataInputStream in = new DataInputStream(fstream);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        String strLine;
	        while ((strLine = br.readLine()) != null)   {
	        	if(strLine.startsWith("daily")){
	        		Matcher values = daily.matcher(strLine.trim());
	        		if(!values.matches()){
	        			logger.error("Schedule Line Error: "+strLine);
	        			continue;
	        		}
	        		boolean correct;
	        		int startHour=Integer.parseInt(values.group(2));
	        		int startMin=Integer.parseInt(values.group(3));
	        		int endHour=Integer.parseInt(values.group(4));
	        		int endMin=Integer.parseInt(values.group(5));
	        		double temperature= Double.parseDouble(values.group(6));
	        		correct = correctHour(startHour) && correctHour(endHour) && correctMinute(startMin) && correctMinute(endMin);
	        		if(!correct){
	        			logger.error("Schedule Line Error: "+strLine);
	        			continue;
	        		}
	        		periods.add(new HeatingPeriod(startHour, startMin, endHour, endMin, 7, 7, temperature));
	        		
	        	}
	        	else{
	        		Matcher values = weekly.matcher(strLine);
	        		if(!values.matches()){
	        			logger.error("Schedule Line Error: "+strLine);
	        			continue;
	        		}
	        		boolean correct;
	        		int startDay = parseDay(values.group(1));
	        		int startHour=Integer.parseInt(values.group(2));
	        		int startMin=Integer.parseInt(values.group(3));
	        		int endDay = parseDay(values.group(4));
	        		int endHour=Integer.parseInt(values.group(5));
	        		int endMin=Integer.parseInt(values.group(6));
	        		
	        		double temperature= Double.parseDouble(values.group(7));
	        		correct = correctHour(startHour) && correctHour(endHour) && correctMinute(startMin) && correctMinute(endMin) && startDay>-1 && endDay>-1;
	        		if(!correct){
	        			logger.error("Schedule Line Error: "+strLine);
	        			continue;
	        		}
	        		periods.add(new HeatingPeriod(startHour, startMin, endHour, endMin, startDay, endDay, temperature));
	        	}
	        }
	        in.close();
	    
		}
	    catch (Exception e){
	    	logger.error("Reading Schedule File");
	    	e.printStackTrace();
	    	System.exit(-1);
	    }
	}
	
	
	
	@Override
	public void run() {
		nextEvent = new PriorityQueue<HeatingPoint>(24, new TimeToStartComparator());
		Calendar cal = Calendar.getInstance();
		int minutes = cal.get(Calendar.MINUTE);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int day = (cal.get(Calendar.DAY_OF_WEEK)+5)%7; //Monday is 0
		
		for(HeatingPeriod period : periods){
			long minutesToTarget = computeHeatingTime(period.getTemperature(), main.getCurrentTemperature());
			if(period.isActive(hour, minutes, day)){
				nextEvent.add(new HeatingPoint(0-minutesToTarget, period.getTemperature()));
				continue;
			}
			long startIn = period.minutesToStart(hour, minutes, day)-minutesToTarget;
			nextEvent.add(new HeatingPoint(startIn, period.getTemperature()));
		}
				

		HeatingPoint current = nextEvent.peek();
		if(current!=null){
			
			if(current.getStart()<6){
				main.getTemperatures().put("PREHEAT",current.getTemperature());
				logger.info("Schedule active");
			}
			else{
				main.getTemperatures().remove("PREHEAT");
			}
		}
		else{
			main.getTemperatures().remove("PREHEAT");
		}
		main.adaptValve();
	}
	
	
	private long computeHeatingTime(double target, double is){
		double minutes = (target-is)/heatingCoefficient;
		if(minutes<0){
			return 0;
		}
		return (long) minutes;
	}

	private boolean correctHour(int h){
		return (0<=h && h<24);
	}
	
	private boolean correctMinute(int m){
		return (0<=m && m<60);
	}
	
	private int parseDay(String s){
		if(s.equalsIgnoreCase("mon")){
			return 0;
		}
		else if(s.equalsIgnoreCase("tue")){
			return 1;
		}
		else if(s.equalsIgnoreCase("wed")){
			return 2;
		}
		else if(s.equalsIgnoreCase("thu")){
			return 3;
		}
		else if(s.equalsIgnoreCase("fri")){
			return 4;
		}
		else if(s.equalsIgnoreCase("sat")){
			return 5;
		}
		else if(s.equalsIgnoreCase("sun")){
			return 6;
		}
		else{
			return -1;
		}
	}
}

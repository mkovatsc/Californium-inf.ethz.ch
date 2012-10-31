package ch.ethz.inf.vs.californium.controller.utility;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
	
	private static final Pattern daily = Pattern.compile("(\\D+) (\\d+):(\\d+) - (\\d+):(\\d+) ((\\d+)(\\.\\d+){0,1}) (\\p{Graph}{3,})");
	private static final Pattern weekly = Pattern.compile("(\\D{3}) (\\d+):(\\d+) - (\\D{3}) (\\d+):(\\d+) ((\\d+)(\\.\\d+){0,1}) (\\p{Graph}{3,})");
	private static final Pattern coefLine = Pattern.compile("(\\p{Graph}{3,}):\\{(\\d{1,2}=\\d{1,6},){0,}(\\d{1,2}=\\d{1,6}){1}\\}");
	
	private Controller main;
	
	private double heatingCoefficientDefault;
	
	private HashMap<String, HashSet<HeatingPeriod>> roomPeriodsMap;
	private HashMap<String, HashMap<Integer, Integer>> heatingCoefficientMap;

	
	public ScheduleManager(Controller controller){
		this.main = controller;
		heatingCoefficientDefault = Properties.std.getDbl("COEFFICIENT_DEFAULT");
		roomPeriodsMap = new HashMap<String,HashSet<HeatingPeriod>>();
		heatingCoefficientMap = new HashMap<String, HashMap<Integer,Integer>>();
		
		try{
			FileInputStream fstream = new FileInputStream(Properties.std.getStr("SCHEDULE_FILE"));
			DataInputStream in = new DataInputStream(fstream);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        String strLine;
	        int startHour;
    		int startMin;
    		int endHour;
    		int endMin;
    		double temperature;
    		int startDay;
    		int endDay;
	        while ((strLine = br.readLine()) != null)   {
	        	String room;
	        	if(strLine.startsWith("daily")){
	        		Matcher values = daily.matcher(strLine.trim());
	        		if(!values.matches()){
	        			logger.error("Schedule Line Error: "+strLine);
	        			System.exit(-1);
	        		}
	        		boolean correct;
	        		startHour=Integer.parseInt(values.group(2));
	        		startMin=Integer.parseInt(values.group(3));
	        		endHour=Integer.parseInt(values.group(4));
	        		endMin=Integer.parseInt(values.group(5));
	        		temperature= Double.parseDouble(values.group(6));
	        		room = values.group(9).toLowerCase();
	        		startDay = 7;
	        		endDay=7;
	        		correct = correctHour(startHour) && correctHour(endHour) && correctMinute(startMin) && correctMinute(endMin);
	        		if(!correct){
	        			logger.error("Schedule Line Error: "+strLine);
	        			System.exit(-1);
	        		}
	        		        		
	        	}
	        	else{
	        		Matcher values = weekly.matcher(strLine);
	        		if(!values.matches()){
	        			logger.error("Schedule Line Error: "+strLine);
	        			System.exit(-1);
	        		}
	        		boolean correct;
	        		startDay = parseDay(values.group(1));
	        		startHour=Integer.parseInt(values.group(2));
	        		startMin=Integer.parseInt(values.group(3));
	        		endDay = parseDay(values.group(4));
	        		endHour=Integer.parseInt(values.group(5));
	        		endMin=Integer.parseInt(values.group(6));
	        		
	        		temperature= Double.parseDouble(values.group(7));
	        		room = values.group(10).toLowerCase();
	        		correct = correctHour(startHour) && correctHour(endHour) && correctMinute(startMin) && correctMinute(endMin) && startDay>-1 && endDay>-1;
	        		if(!correct){
	        			logger.error("Schedule Line Error: "+strLine);
	        			System.exit(-1);
	        		}
	        	}
	        	if(roomPeriodsMap.containsKey(room)){
	        		HashSet<HeatingPeriod> current = roomPeriodsMap.get(room);
	        		current.add(new HeatingPeriod(startHour, startMin, endHour, endMin, startDay, endDay, temperature));
	        		roomPeriodsMap.put(room,current);
	        	}
	        	else{
	        		HashSet<HeatingPeriod> current = new HashSet<HeatingPeriod>();
	        		current.add(new HeatingPeriod(startHour, startMin, endHour, endMin, startDay, endDay, temperature));
	        		roomPeriodsMap.put(room,current);
	        	}
	        }
	        in.close();
	    
		}
	    catch (Exception e){
	    	logger.error("Reading Schedule File");
	    	e.printStackTrace();
	    	System.exit(-1);
	    }
		
		try{
			FileInputStream fstream = new FileInputStream(Properties.std.getStr("COEFFICIENT_FILE"));
			DataInputStream in = new DataInputStream(fstream);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in));
	        String strLine;
	        int degree;
	        int time;
	        while ((strLine = br.readLine()) != null)   {
	        	Matcher values = coefLine.matcher(strLine);
	        	if(!values.matches()){
        			logger.error("Coefficient Line Error: "+strLine);
        			System.exit(-1);
        		}
	          	String room = values.group(1).toLowerCase();
	        	HashMap<Integer,Integer> roomCoefMap = new HashMap<Integer, Integer>(40);
	        	String valueString = strLine.substring(strLine.indexOf("{")+1,strLine.indexOf("}"));
	        	String valueArray[] = valueString.split(",");
	        	for(String pair : valueArray){
	        		if (null!=roomCoefMap.put(Integer.parseInt(pair.substring(0, pair.indexOf("="))),Integer.parseInt(pair.substring(pair.indexOf("=")+1)))){
	           			logger.error("Coefficient Line Error: "+strLine);
	        			System.exit(-1);
	        		}
	        	}
	        	if(null!=heatingCoefficientMap.put(room,roomCoefMap)){
	        		System.exit(-1);
	        	}
	        }
	        in.close();
	    
		}
	    catch (Exception e){
	    	logger.error("Reading Coefficient File");
	    	e.printStackTrace();
	    	System.exit(-1);
	    }
		
	}
	
	
	
	@Override
	public void run() {
		
		for(String roomID : main.getRooms()){
			RoomInfo currentRoom = main.getRoomInfo(roomID);
			HashSet<HeatingPeriod> roomPeriod = roomPeriodsMap.get(roomID);
			if(currentRoom==null || roomPeriod==null){continue;}
		
			PriorityQueue<HeatingPoint> nextEvent = new PriorityQueue<HeatingPoint>(24, new TimeToStartComparator());
			Calendar cal = Calendar.getInstance();
			int minutes = cal.get(Calendar.MINUTE);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			int day = (cal.get(Calendar.DAY_OF_WEEK)+5)%7; //Monday is 0
		
			for(HeatingPeriod period : roomPeriod){
				long minutesToTarget = computeHeatingTime(roomID, period.getTemperature(), currentRoom.getCurrentTemperature());
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
					currentRoom.addTemperature("PREHEAT",current.getTemperature());
					logger.info("Schedule active");
				}
				else{
					currentRoom.removeTemperature("PREHEAT");
				}
			}
			else{
				currentRoom.removeTemperature("PREHEAT");
			}
		}
		main.adaptValve();
	}
	
	
	private long computeHeatingTime(String room, double target, double is){
		if(target<is){return 0;}
		if(heatingCoefficientMap.isEmpty() || !heatingCoefficientMap.containsKey(room)){
			double minutes = (target-is)/heatingCoefficientDefault;
			return (long) minutes;
		}
		long time = 0;
		HashMap<Integer,Integer> coefficients = heatingCoefficientMap.get(room);
		int startTemp = (int) Math.floor(is);
		int endTemp = (int) Math.ceil(target);
		int lastTime = -1;
		if(coefficients.containsKey(startTemp)){
			lastTime = coefficients.get(startTemp);
		}
		else{
			int toFind = startTemp-1;
			while(toFind>=0){
				if(coefficients.containsKey(toFind)){
					lastTime = coefficients.get(toFind);
					break;
				}
				toFind--;
			}
		}
		if(lastTime<0){
			int toFind = startTemp+1;
			while(toFind<=99){
				if(coefficients.containsKey(toFind)){
					lastTime = coefficients.get(toFind);
					break;
				}
				toFind++;
			}
		}
		while(startTemp<endTemp){
			if(coefficients.containsKey(startTemp)){
				lastTime = coefficients.get(startTemp);
			}
			time+=lastTime;
			startTemp++;
		}
		return (long) time;
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

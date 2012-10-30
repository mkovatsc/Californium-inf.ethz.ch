package ch.ethz.inf.vs.californium.controller.utility;

import java.util.HashMap;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.controller.Controller;

public class RoomInfo {
	
	private static Logger logger = Logger.getLogger(RoomInfo.class);
	
	private HashMap<String,Double> temperatures;
	private double currentTemperature;
	
	private int valveTarget=0;
	private int valveOldPostion=-1;
	
	private boolean windowOpen;
	
	private int temperatureDown;
	
	
	public RoomInfo(){
		
		setValveTarget(0);
		setValveOldPostion(-1);
		setWindowOpen(false);
		temperatures = new HashMap<String, Double>();
		setCurrentTemperature(0);
		setTemperatureDown(0);
		
	}


	public double getCurrentTemperature() {
		return currentTemperature;
	}


	public void setCurrentTemperature(double currentTemperature) {
		this.currentTemperature = currentTemperature;
	}


	public int getValveTarget() {
		return valveTarget;
	}


	public void setValveTarget(int valveTarget) {
		this.valveTarget = valveTarget;
	}


	public int getValveOldPostion() {
		return valveOldPostion;
	}


	public void setValveOldPostion(int valveOldPostion) {
		this.valveOldPostion = valveOldPostion;
	}


	public boolean isWindowOpen() {
		return windowOpen;
	}


	public void setWindowOpen(boolean windowOpen) {
		this.windowOpen = windowOpen;
	}

	public int getNextValve(){
		double targetTemperature = Properties.std.getDbl("MIN_TEMPERATURE");
		for(double temp : temperatures.values()){
			targetTemperature = targetTemperature > temp ? targetTemperature : temp; 
		}
		//We set a new target with the wheel
		if(temperatures.get("WHEEL")!=null){
			targetTemperature = temperatures.get("WHEEL");
		}
		
		//We need to heat
		if(targetTemperature>currentTemperature+Properties.std.getDbl("TOLERANCE")){
			valveTarget=100;
		}
		//We can stop heating
		else if(targetTemperature<currentTemperature-Properties.std.getDbl("TOLERANCE")){
			valveTarget=0;
		}
		//We need to keep Temperature, slowly adapt valve
		else{
			if(targetTemperature<currentTemperature-Properties.std.getDbl("TOLERANCE")/2){
				valveTarget = (valveOldPostion)/2;
			}
			else if(targetTemperature>currentTemperature+Properties.std.getDbl("TOLERANCE")/2){
				valveTarget = (valveOldPostion+100)/2;
			}
		}
		if(windowOpen){
			logger.info("Windwos are open");
			valveTarget=0;
		}
		
		return valveTarget;
	}
	
	public void addTemperature(String id, double temperature){
		temperatures.put(id, temperature);
	}
	
	public void removeTemperature(String id){
		temperatures.remove(id);
	}
	
	public double currentHighestTemperature(){
		double highestTemperature = Properties.std.getDbl("MIN_TEMPERATURE");
		for(double temp : temperatures.values()){
			highestTemperature = highestTemperature > temp ? highestTemperature : temp; 
		}
		return highestTemperature;
	}


	public int getTemperatureDown() {
		return temperatureDown;
	}

	public void increaseTemperatureDown(){
		temperatureDown++;
	}

	public void setTemperatureDown(int temperatureDown) {
		this.temperatureDown = temperatureDown;
	}
}

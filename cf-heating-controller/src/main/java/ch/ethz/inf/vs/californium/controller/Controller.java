package ch.ethz.inf.vs.californium.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.controller.utility.Node;
import ch.ethz.inf.vs.californium.controller.utility.Properties;
import ch.ethz.inf.vs.californium.controller.utility.ScheduleManager;
import ch.ethz.inf.vs.californium.controller.utility.SensorResource;
import ch.ethz.inf.vs.californium.controller.utility.SensorVerifier;
import ch.ethz.inf.vs.californium.controller.utility.SetResender;
import ch.ethz.inf.vs.californium.controller.utility.SettingResource;

public class Controller {

	
	private static Logger logger = Logger.getLogger(Controller.class);
	private HashMap<String, SensorResource> sensors;
	private HashMap<String, SettingResource> setters;
	private HashMap<String, Node> nodes;
	private String rdUriBase;
	private HashSet<String> types;
	
	private HashMap<String, Double> temperatures;
	private HashMap<SettingResource, String> tasksToDo;
	
	private double currentTemperature;
	
	private int valveTarget=0;
	private int valveOldPostion=0;
	private int valvePosition;
	
	private ScheduleManager schedule;
	
	private boolean windowOpen;
	
	private Timer timers;
	
	
	public Controller(){
		schedule = new ScheduleManager(this);
		sensors =  new HashMap<String, SensorResource>();
		setters = new HashMap<String, SettingResource>();
		tasksToDo = new HashMap<SettingResource, String>();
		temperatures = new HashMap<String, Double>();
		nodes=new HashMap<String, Node>();
		types= Properties.std.getSensorTypes();
		windowOpen=false;
		timers = new Timer();

		if(Properties.std.containsKey("RD_ADDRESS")){
			String rdHost = Properties.std.getStr("RD_ADDRESS");
			rdUriBase = "coap://"+rdHost+"";
			GETRequest tmpReq  = new GETRequest();
			if(!tmpReq.setURI(rdUriBase)){
				logger.error("RD address not valid");
				System.exit(-1);
			}
		}
		else{
			logger.error("RD not specified");
			System.exit(-1);
		}
		timers.schedule(new SetResender(this), 300000, 300000);
		timers.schedule(new SensorVerifier(this), 300000, 300000);
		timers.schedule(schedule, 60*1000, 60*1000);
		timers.schedule(new RDTask(),10*1000,3600*1000);
	}
	
	public void reactOnTemperatureChange(){
		int count=0;
		double sum=0;
		for(SensorResource sensor: sensors.values()){
			if(!sensor.getType().contains("temperature") || !sensor.isAlive()){continue;}
			count++;
			sum += Double.parseDouble(sensor.getNewestValue());
		}
		currentTemperature =  sum/(double) count;
		logger.info("New Average Temperature: " +currentTemperature);
	}
	
	
	public void reactOnWindowChange(){
		boolean open = false;
		for(SensorResource sensor: sensors.values()){
			if(!sensor.getType().contains("reed") || !sensor.containsTagKey("window") || !sensor.isAlive()){continue;}
			open = open || (Integer.parseInt(sensor.getNewestValue()) % 2 == 1);
		}
		windowOpen = open;
		logger.info("New Window Status: " +open);
	}

	
	public void reactOnMovementChange(){
		boolean movement = false;
		for(SensorResource sensor: sensors.values()){
			if(!sensor.getType().contains("pir") || !sensor.isAlive()){continue;}
			movement = movement || (Integer.parseInt(sensor.getNewestValue()) % 2 == 1);
		}
		if(movement){
			temperatures.put("MOVEMENT", Properties.std.getDbl("PIR_TEMPERATURE"));
			logger.info("Movement in the room");
		}
		else{
			temperatures.remove("MOVEMENT");
			//The person has left remove an eventually set wheel change
			temperatures.remove("WHEEL");
			logger.info("Movement stopped");
		}
		logger.info("New Movement Status: "+movement);
	}
	
	
	public void reactOnWheelChange(int change){
		double highestTemperature = Properties.std.getDbl("MIN_TEMPERATURE");
		for(double temp : temperatures.values()){
			highestTemperature = highestTemperature > temp ? highestTemperature : temp; 
		}
		if(change>0){
			if(currentTemperature>highestTemperature-2*Properties.std.getDbl("TOLERANCE")){
				temperatures.put("WHEEL", highestTemperature+(double) change);
			}
			//The room is sill heating up so we do not react on the input
		}
		else{
			temperatures.put("WHEEL", currentTemperature+(double) change);
		}
		logger.info("New Wheel Change: "+change);
		
	}
	
	
	public void adaptValve(){
		
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
		//We need to keep Temperature
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
		valveOldPostion = valveTarget;			
		for(SettingResource setter: setters.values()){
			if(!setter.getType().equals("valve") || !setter.isAlive()){continue;}
			if(! setter.updateSettings(String.valueOf(valveTarget))){
				logger.warn("Change failed on: "+setter.getContext()+setter.getPath());
				tasksToDo.put(setter, String.valueOf(valveTarget));
			}
		}		
	}
	
	public void processChange(SensorResource sensor){
		if(sensor.getType().contains("temperature")){
			reactOnTemperatureChange();
		}
		else if(sensor.getType().contains("reed")){
			reactOnWindowChange();
		}
		else if(sensor.getType().contains("pir")){
			reactOnMovementChange();
		}
		else if(sensor.getType().contains("wheel")){
			reactOnWheelChange(Integer.parseInt(sensor.getNewestValue())-Integer.parseInt(sensor.getOldValue()));
		}
		adaptValve();
		
	}
	
	public void getResourcesFromRD(){
		
		for(String type : types){
			GETRequest rdLookup = new GETRequest();
			rdLookup.setURI(rdUriBase+"/rd-lookup/res");
			
			rdLookup.addOption(new Option("rt="+type+"*", OptionNumberRegistry.URI_QUERY));

			rdLookup.enableResponseQueue(true);
			Response rdResponse = null;		
			
			try {
				rdLookup.execute();
				
				rdResponse = rdLookup.receiveResponse();
				if(rdResponse !=null && rdResponse.getCode() == CodeRegistry.RESP_CONTENT){
					
					Scanner scanner = new Scanner(rdResponse.getPayloadString());
					scanner.useDelimiter(",");
					ArrayList<String> pathResources = new ArrayList<String>();
					while (scanner.hasNext()) {
						pathResources.add(scanner.next());
					}
									
					for (String p : pathResources) {
						scanner = new Scanner(p);
	
						String uri = "", pathTemp = "";
						while ((pathTemp = scanner.findInLine("<coap://.*?>")) != null) {
							uri = pathTemp.substring(1, pathTemp.length() - 1);
						}
						if (uri==""){
							continue;
						}
						String completePath = uri.substring(uri.indexOf("//")+2);
						String context = uri.substring(0,uri.indexOf("/", 9));

						
						//String host = completePath.substring(0,completePath.indexOf("/"));
						String resourcePath = completePath.substring(completePath.indexOf("/"));
						String id = "";
						
						List<LinkAttribute> linkAttributes = new ArrayList<LinkAttribute>();
						boolean isObs = false;
					
						scanner.useDelimiter(";");
						
						while (scanner.hasNext()) {
							LinkAttribute attrib=LinkAttribute.parse(scanner.next());
								//System.out.println(attrib.serialize());
								if(attrib.getName().equals(LinkFormat.RESOURCE_TYPE)){
									linkAttributes.add(attrib);
								}
								if(attrib.getName().equals(LinkFormat.OBSERVABLE)){
									linkAttributes.add(attrib);
									isObs=true;
								}
								if(attrib.getName().equals(LinkFormat.END_POINT)){
									id = attrib.getStringValue();
								}
						}
						Node node = nodes.get(context);
						if(node == null){
							node = new Node(context, id);
							nodes.put(context,node);
							logger.info("Added Node: "+context);
						}
						else{
							node.restartHeartBeat();
						}
						if(resourcePath.contains("sensor")){
							SensorResource sensor = sensors.get(context+resourcePath);
							if(sensor == null){
								sensor = new SensorResource(resourcePath, context, type, isObs, this);
								sensors.put(context+resourcePath, sensor);
								logger.info("Added Sensor: "+context+resourcePath);
							}
							if(!sensor.isAlive()){
								sensor.register();
							}
						}
						else if(resourcePath.contains("set")){
							SettingResource setter = setters.get(context+resourcePath);
							if(setter == null){
								setter = new SettingResource(resourcePath, context, type);
								setters.put(context+resourcePath, setter);
								logger.info("Added Setter: "+context+resourcePath);
							}
							else{
								if(!setter.isAlive()){
									setter.getSettings();
								}
							}
							
						}
	
					}
				}
			} catch (IOException e) {
					logger.error("Retrieving Resources for type: "+type);
			} catch (InterruptedException e) {
					logger.error("Retrieving Resources for type: "+type);
			}
			
		}
	}
	
	

	public String getIdFromContext(String context){
		if(nodes.get(context)==null){return "";}
		return nodes.get(context).getIdentifier();
	}
	
	public String getRdUriBase(){
		return rdUriBase;
	}

	
	public HashMap<String, Double> getTemperatures() {
		return temperatures;
	}

	public HashMap<SettingResource, String> getTasksToDo() {
		return tasksToDo;
	}

	public HashMap<String, Node> getNodes() {
		return nodes;
	}

	public Timer getTimers() {
		return timers;
	}

	public HashMap<String, SensorResource> getSensors(){
		return sensors;
	}
	
	public Node getNode(String address){
		return nodes.get(address);
		
	}
	
	public double getCurrentTemperature(){
		return currentTemperature;
	}
	
	
	private class RDTask extends TimerTask{
		@Override
		public void run() {
			getResourcesFromRD();			
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controller controller = new Controller();


		
		// TODO Auto-generated method stub

	}

	


}

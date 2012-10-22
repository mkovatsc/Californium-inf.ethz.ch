package ch.ethz.inf.vs.californium.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.controller.utility.EventManager;
import ch.ethz.inf.vs.californium.controller.utility.HeartBeatResource;
import ch.ethz.inf.vs.californium.controller.utility.Node;
import ch.ethz.inf.vs.californium.controller.utility.SensorResource;
import ch.ethz.inf.vs.californium.controller.utility.SettingResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;
import ch.ethz.inf.vs.californium.util.Properties;


public class Controller {

	
	private static Logger logger = Logger.getLogger(Controller.class);
	private HashMap<String, SensorResource> sensors;
	private HashMap<String, SettingResource> setters;
	private HashMap<String, Node> nodes;
	private String rdUriBase;
	private HashSet<String> types;
	private double targetTemperature;
	private double currentTemperature;
	private int valveTarget;
	private int valvePosition;
	private PriorityQueue<Mode> currentMode;
	
	public Controller(){
		sensors =  new HashMap<String, SensorResource>();
		setters = new HashMap<String, SettingResource>();
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
		types=new HashSet<String>();
		types.add("temperature");
		types.add("reed");
		types.add("pir");
		types.add("valve");
		nodes=new HashMap<String, Node>();
		getResourcesFromRD();
	}
	
	public void reactOnTemperatureChange(){
		int count=0;
		double sum=0;
		for(SensorResource sensor: sensors.values()){
			if(!sensor.getType().contains("temperature")){continue;}
			count++;
			sum += Double.parseDouble(sensor.getLastValue());
		}
		currentTemperature =  sum/(double) count;
		adaptValve();
		
	}
	
	public void reactOnWindowChange(){
		boolean open = false;
		for(SensorResource sensor: sensors.values()){
			if(!sensor.getType().contains("reed") || !sensor.hasTag("window")){continue;}
			open = open || (Integer.parseInt(sensor.getLastValue()) % 2 == 1);
		}
		if(open){
			if(currentMode.contains(Mode.WINDOW)){
				currentMode.add(Mode.WINDOW);
			}
			
		}
		else{
			currentMode.remove(Mode.WINDOW);
		}
	}

	
	public void reactOnMovementChange(){
		
		
	}
	
	
	
	public void adaptValve(){
		
		Mode mode = currentMode.peek();
		if(mode == null){
			mode = Mode.TARGET;
		}
		
		switch(mode){
		case WINDOW:
			valveTarget=0;
			break;
		case MOVEMENT:
			valveTarget=50;
			break;
		case PREHEAT:
			valveTarget=100;
			break;
		case TARGET:
			valveTarget=80;
			break;
		}
		
		
		for(SettingResource setter: setters.values()){
			if(!setter.getType().equals("valve")){continue;}
			setter.updateSettings(String.valueOf(valveTarget));
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
						if(resourcePath.contains("sensor")){
							SensorResource sensor = sensors.get(context+resourcePath);
							if(sensor == null){
								sensor = new SensorResource(resourcePath, context, type, isObs, this);
								sensors.put(context+resourcePath, sensor);
							}
							logger.info("Added Sensor: "+context+resourcePath);
						}
						else if(resourcePath.contains("set")){
							SettingResource setter = setters.get(context+resourcePath);
							if(setter == null){
								setter = new SettingResource(resourcePath, context, type);
								setters.put(context+resourcePath, setter);
							}
							logger.info("Added Setter: "+context+resourcePath);
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controller controller = new Controller();


		
		// TODO Auto-generated method stub

	}

	
	
	public enum Mode{
		WINDOW, PREHEAT, MOVEMENT, TARGET
	
	}
}

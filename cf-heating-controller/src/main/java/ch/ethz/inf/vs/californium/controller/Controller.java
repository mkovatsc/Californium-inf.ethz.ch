package ch.ethz.inf.vs.californium.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.controller.utility.Node;
import ch.ethz.inf.vs.californium.controller.utility.Properties;
import ch.ethz.inf.vs.californium.controller.utility.RoomInfo;
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
	
	private HashMap<String, RoomInfo> rooms;
	
	private HashMap<SettingResource, String> tasksToDo;
	
	private ScheduleManager schedule;
	
	private Timer timers;
	
	
	public Controller(){
		CommunicatorFactory.getInstance().setUdpPort(Properties.std.getInt("DEFAULT_PORT"));
		logger.info("Controller started on Port: "+ CommunicatorFactory.getInstance().getCommunicator().getPort());
		schedule = new ScheduleManager(this);
		sensors =  new HashMap<String, SensorResource>();
		setters = new HashMap<String, SettingResource>();
		tasksToDo = new HashMap<SettingResource, String>();
		rooms = new HashMap<String, RoomInfo>();
		nodes=new HashMap<String, Node>();
		types= Properties.std.getSensorTypes();
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
		if(types.isEmpty()){
			logger.error("No Sensors specified");
		}
		timers.schedule(new RDTask(),10*1000,3600*1000);
		timers.schedule(new SetResender(this), 300000, 300000);
		timers.schedule(new SensorVerifier(this), 300000, 300000);
		timers.schedule(schedule, 300*1000, 180*1000);

	}
	
	public void reactOnTemperatureChange(String room){
		double count=0;
		double sum=0;
		for(SensorResource sensor: sensors.values()){
			if(!sensor.getType().contains("temperature") || !sensor.isAlive() || !sensor.containsExactTag("room", room)){continue;}
			
			count++;
			sum += Double.parseDouble(sensor.getNewestValue());
		}
		if(count!=0){
			RoomInfo currentRoom = rooms.get(room);
			if(currentRoom!=null){
				currentRoom.setCurrentTemperature(sum/count);
				rooms.put(room,currentRoom);
			}			
			logger.info(room+" New Average Temperature: " +(sum/count));
		}
		else{
			logger.warn("No Temperature Sensors Alive in Room " +room);
		}
		
	}
	
	
	public void reactOnWindowChange(String room){
		RoomInfo currentRoom = rooms.get(room);
		if(currentRoom==null){return;}
		boolean old = currentRoom.isWindowOpen();
		boolean open = false;
		for(SensorResource sensor: sensors.values()){
			if(!sensor.getType().contains("reed") || !sensor.containsTag("window") || !sensor.containsExactTag("room", room) || !sensor.isAlive()){continue;}
			open = open || (Integer.parseInt(sensor.getNewestValue()) % 2 == 1);
		}
		currentRoom.setWindowOpen(open);
		if(old!=open){
			logger.info(room+ "New Window Status: " +open);
		}
		rooms.put(room,currentRoom);
	}

	
	public void reactOnMovementChange(String room){
		RoomInfo currentRoom = rooms.get(room);
		if(currentRoom==null){return;}
		boolean movement = false;
		for(SensorResource sensor: sensors.values()){
			if(!sensor.getType().contains("pir") || !sensor.isAlive() || !sensor.containsExactTag("room", room)){continue;}
			movement = movement || (Integer.parseInt(sensor.getNewestValue()) % 2 == 1);
		}
		if(movement){
			currentRoom.addTemperature("MOVEMENT", Properties.std.getDbl("PIR_TEMPERATURE"));
			logger.info("Movement in the room "+room);
		}
		else{
			currentRoom.removeTemperature("MOVEMENT");
			//The person has left remove an eventually set wheel change
			currentRoom.removeTemperature("WHEEL");
			logger.info("Movement stopped in Room "+room);
		}
		rooms.put(room,currentRoom);
	}
	
	
	public void reactOnWheelChange(String room, int change){
		RoomInfo currentRoom = rooms.get(room);
		if(currentRoom==null){return;}
		double highestTemperature = currentRoom.currentHighestTemperature();
		double currentTemperature =  currentRoom.getCurrentTemperature();
		if(change>0){
			if(currentTemperature>highestTemperature-2*Properties.std.getDbl("TOLERANCE")){
				currentRoom.addTemperature("WHEEL", highestTemperature+(double) change);
			}
			//The room is sill heating up so we do not react on the input
		}
		else{
			currentRoom.addTemperature("WHEEL", currentTemperature+(double) change);
		}
		rooms.put(room,currentRoom);
		logger.info(room +" New Wheel Change: "+change);
		
	}
	
	
	public void adaptValve(){
		logger.debug("Compute new Valve");
		Set<String> roomsIDs= rooms.keySet();
		for(String roomID : roomsIDs){
			RoomInfo currentRoom = rooms.get(roomID);
			if(currentRoom == null){continue;}
			int valveTarget =currentRoom.getNextValve();
			int valveOldPostion = currentRoom.getValveOldPostion();
		
			if(valveOldPostion!=valveTarget){	
				for(SettingResource setter: setters.values()){
					if(!setter.getType().equals("valve") || !setter.isAlive() || !setter.containsExactTag("room",roomID)){continue;}
					if(! setter.updateSettings(String.valueOf(valveTarget))){
						logger.warn("Valve changed failed: "+setter.getContext()+setter.getPath());
						tasksToDo.put(setter, String.valueOf(valveTarget));
					}
					else{
						tasksToDo.remove(setter);
						logger.info("Valve set to "+valveTarget+": "+setter.getContext()+setter.getPath());
					}
				}
			}
			currentRoom.setValveOldPostion(valveTarget);
			rooms.put(roomID,currentRoom);
		}
	}
	
	public void processChange(SensorResource sensor){
		String room=sensor.getTag("room");
		if(room==null ){return;}
		RoomInfo currentRoom = rooms.get(room);
		if(currentRoom==null){return;}
		if(sensor.getType().contains("temperature")){
			double newest = Double.parseDouble(sensor.getNewestValue());
			if(Math.abs(newest-currentRoom.getCurrentTemperature())>5 && !sensor.getOldValue().isEmpty()){
				if(Math.abs(newest-Double.parseDouble(sensor.getOldValue()))>5){
					logger.warn("Strange Temperature: "+newest+ " from "+sensor.getContext()+sensor.getPath());
					sensor.ignoreNewest();
					
					return;
				}
			}
			reactOnTemperatureChange(room);
		}
		else if(sensor.getType().contains("reed")){
			reactOnWindowChange(room);
		}
		else if(sensor.getType().contains("pir")){
			reactOnMovementChange(room);
		}
		else if(sensor.getType().contains("wheel")){
			if(!sensor.getOldValue().isEmpty()){
				reactOnWheelChange(room,Integer.parseInt(sensor.getNewestValue())-Integer.parseInt(sensor.getOldValue()));
			}
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
						else{
							node.restartHeartBeat();
						}
						if(resourcePath.contains("/sensor")){
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
						else if(resourcePath.contains("/set")){
							SettingResource setter = setters.get(context+resourcePath);
							if(setter == null){
								setter = new SettingResource(resourcePath, context, type, this);
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
	
	public void addRoom(String roomID){
		if(!rooms.containsKey(roomID)){
			rooms.put(roomID, new RoomInfo());
		}
	}
	
	public void updateRoom(String roomID, RoomInfo room){
		rooms.put(roomID, room);
	}
	
	public RoomInfo getRoomInfo(String roomID){
		return rooms.get(roomID);
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

	public Set<String> getRooms(){
		return rooms.keySet();
	}

	


}

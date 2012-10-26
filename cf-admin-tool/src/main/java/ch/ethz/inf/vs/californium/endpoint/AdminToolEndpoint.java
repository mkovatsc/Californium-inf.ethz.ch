package ch.ethz.inf.vs.californium.endpoint;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import ch.ethz.inf.vs.californium.coap.CommunicatorFactory;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resource.ObservableNodeResource;
import ch.ethz.inf.vs.californium.endpoint.resource.ObservableResource;
import ch.ethz.inf.vs.californium.endpoint.resource.ObserveTopResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;
import ch.ethz.inf.vs.californium.util.Properties;

public class AdminToolEndpoint extends LocalEndpoint {
	
		private int udpPort = 0;
		private boolean runAsDaemon = false;
		private int transferBlockSize = 0;
		private int requestPerSecond = 0;
//		private RDResource rdResource = null;
		private ObserveTopResource obsResource = null;
		private HashMap<String, VirtualNode> allEndpoints = null;
		private HashMap<String, VirtualNode> aliveEndpoints = null;
		private Date lastEnpointCheck;

		/**
		 * Instantiates a new Admin Tools endpoint from the default ports.
		 * 
		 * @throws SocketException
		 *             the socket exception
		 */
		public AdminToolEndpoint() throws SocketException {
			this(Properties.std.getInt("DEFAULT_PORT"));
		}

		/**
		 * Instantiates a new resource directory endpoint.
		 * 
		 * @param udpPort
		 *            the udp port
		 * @throws SocketException
		 *             the socket exception
		 */
		public AdminToolEndpoint(int udpPort) throws SocketException {
			this(udpPort, 0, false, 0);
		}

		/**
		 * Instantiates a new resource directory endpoint.
		 * 
		 * @param udpPort
		 *            the udp port
		 * @param defaultBlockSze
		 *            the default block sze
		 * @param daemon
		 *            the daemon
		 * @param requestPerSecond
		 *            the request per second
		 * @throws SocketException
		 *             the socket exception
		 */
		public AdminToolEndpoint(int udpPort, int transferBlockSize, boolean runAsDaemon, int requestPerSecond) throws SocketException {
			super();

			this.udpPort = udpPort;
			this.transferBlockSize = transferBlockSize;
			this.runAsDaemon = runAsDaemon;
			this.requestPerSecond = requestPerSecond;
			this.allEndpoints = new HashMap<String,VirtualNode>();
			this.aliveEndpoints = new HashMap<String, VirtualNode>();
			this.lastEnpointCheck = new Date(0);

			// add Resource Directory resource
			
//			addResource(rdResource = new RDResource());
//			addResource(new RDLookUpTopResource(rdResource));
			addResource(obsResource = new ObserveTopResource());
			


		}

		
	
		/**
		 * Gets the port.
		 * 
		 * @return the port
		 */
		public int getPort() {
			return CommunicatorFactory.getInstance().getCommunicator().getPort();
		}


		@Override
		protected void createCommunicator() {
			// get the communicator factory
			CommunicatorFactory factory = CommunicatorFactory.getInstance();

			// set the parameters of the communicator
			factory.setUdpPort(udpPort);
			factory.setTransferBlockSize(transferBlockSize);
			factory.setRunAsDaemon(runAsDaemon);
			factory.setRequestPerSecond(requestPerSecond);

			// initialize communicator
			Communicator communicator = factory.getCommunicator();

			// register the endpoint as a receiver of the communicator
			communicator.registerReceiver(this);
		}

	    @Override
	    public void handleRequest(Request request) {
	        
	        // dispatch to requested resource
	        super.handleRequest(request);
	    }

		@Override
		protected void responseProduced(Response response) {
			// Do Nothing
			
		}
		
		
		public HashMap<String,VirtualNode> getEveryKnownEndpoint(){
			return allEndpoints;
		}
		
		public HashMap<String,VirtualNode> getAliveEndpoint(){
			
			//HashMap<String, VirtualNode> aliveEndpoints = new HashMap<String, VirtualNode>();
			if(lastEnpointCheck.getTime()>new Date().getTime()-1800*1000){
				return aliveEndpoints;
			}
			
			aliveEndpoints.clear();
			
			GETRequest rdLookup = new GETRequest();
			
			rdLookup.setURI("coap://"+Properties.std.getStr("RD_ADDRESS")+"/rd-lookup/ep");

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
						
						String context = uri.substring(uri.indexOf("//")+2);
						String domain ="";
						String endpointIdentifier = "";
						String endpointType = "";
						
						
						scanner.useDelimiter(";");
						
						while (scanner.hasNext()) {
							LinkAttribute attrib=LinkAttribute.parse(scanner.next());
							//System.out.println(attrib.serialize());
							if(attrib.getName().equals(LinkFormat.RESOURCE_TYPE)){
								endpointType=attrib.getStringValue();
							}
							else if(attrib.getName().equals(LinkFormat.END_POINT)){
								endpointIdentifier = attrib.getStringValue();
							}
							else if(attrib.getName().equals(LinkFormat.DOMAIN)){
								domain = attrib.getStringValue();
							}

						}
						
						VirtualNode ep = new VirtualNode(endpointIdentifier, domain, endpointType, context);
						aliveEndpoints.put(endpointIdentifier,ep);
												
					}
				
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			allEndpoints.putAll(aliveEndpoints);
			return aliveEndpoints;
			
		}
		
		
		/*
		public Set<RDNodeResource> getEndpointObjects(){
			TreeSet<RDNodeResource> result = new TreeSet<RDNodeResource>();
			for(Resource res : rdResource.getSubResources()){
				result.add((RDNodeResource) res);
			}
			return result;
		}
		
		public RDNodeResource getEndpoint(String id){
			for(Resource res : rdResource.getSubResources()){
				RDNodeResource cur = (RDNodeResource) res;
				if(cur.getEndpointIdentifier().equals(id)){
					return cur;
				}
			}
			return null;
		}
		
		public List<String> getInitalResources(){
			ArrayList<String> result = new ArrayList<String>();
			for(Resource res : this.getRootResource().getSubResources()){
				result.add(res.getName());
			}
			return result;
		}
		*/
		
		public VirtualNode getVirtualNode(String id){
			return allEndpoints.get(id);
		}
				
			
		public double getLossRateId(String id){
			return obsResource.getLossRateId(id);
		}
		
	
		public Date getLastHeardOfId(String id){
			return obsResource.getLastHeardOfId(id);
		}
		
		public ArrayList<String> getAllEndpointSubResourcesId(String id){
			
			ArrayList<String> subs = new ArrayList<String>();
			
			GETRequest rdLookup = new GETRequest();
			
			rdLookup.setURI("coap://"+Properties.std.getStr("RD_ADDRESS")+"/rd-lookup/res");
			rdLookup.setOption(new Option("ep=\""+id+"\"",OptionNumberRegistry.URI_QUERY));

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
							uri = pathTemp.substring(8, pathTemp.length() - 1);
						}
						if (uri==""){
							continue;
						}
						subs.add(uri.substring(uri.indexOf("/")));
												
					}
				
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return subs;
			
		}
		
		public String getLastValueRes(String resourcePath){
			Resource res = obsResource.getResource(resourcePath);
			if (res ==null || res.getClass() != ObservableResource.class){
				return null;
			}
			ObservableResource obsRes = (ObservableResource) res;
			return obsRes.getLastPayload();
		}
		
		public void reregisterObserveId(String id){
			Resource node = obsResource.getResource(id);
			if (node == null || node.getClass() != ObservableNodeResource.class){
				return;
			}
			LinkedList<Resource> todo = new LinkedList<Resource>();
			todo.add(node);						
			while(!todo.isEmpty()){
				Resource current = todo.pop();
				if(current.getClass() == ObservableResource.class){
					((ObservableResource) current).resendObserveRegistration(true);
				}
				for(Resource res : current.getSubResources()){
					todo.add(res);
				}
			}
		}
		
		public String getEndpointDebugId(String id, String type){
			Resource res = obsResource.getResource(id+"/debug/heartbeat");
			if (res ==null || res.getClass() != ObservableResource.class){
				return null;
			}
			ObservableResource obsRes = (ObservableResource) res;
			String payload = obsRes.getLastPayload();
			HashMap<String,String> debugInfo = new HashMap<String,String>();
			for(String pair : payload.split(",")){
				if (pair.contains(":")){
					debugInfo.put(pair.substring(0, pair.indexOf(":")).trim(), pair.substring(pair.indexOf(":")+1).trim());
				}
			}
			if(type.equalsIgnoreCase("uptime") && debugInfo.containsKey(type)){
				String result;
				int runtime = Integer.parseInt(debugInfo.get(type));
				int day, hour, min, sec;
				sec = runtime % 60;
				min = (runtime/60) % 60;
				hour = (runtime/3600) % 60;
				day = (runtime/86400) % 60;
				result =day+ "d "+hour+"h "+min+"m "+sec+"s";
				return result;
				
			}
			else if(debugInfo.containsKey(type)){
				return debugInfo.get(type);
			}
			else {
				return null;
			}
		}
		
}

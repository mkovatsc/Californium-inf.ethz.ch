package ch.ethz.inf.vs.californium.endpoint;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import ch.ethz.inf.vs.californium.coap.CommunicatorFactory;
import ch.ethz.inf.vs.californium.coap.CommunicatorFactory.Communicator;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.resource.ObservableResource;
import ch.ethz.inf.vs.californium.endpoint.resource.ObserveTopResource;
import ch.ethz.inf.vs.californium.endpoint.resources.RDLookUpTopResource;
import ch.ethz.inf.vs.californium.endpoint.resources.RDNodeResource;
import ch.ethz.inf.vs.californium.endpoint.resources.RDResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;
import ch.ethz.inf.vs.californium.util.Properties;

public class AdminToolEndpoint extends LocalEndpoint {
	
		private int udpPort = 0;
		private boolean runAsDaemon = false;
		private int transferBlockSize = 0;
		private int requestPerSecond = 0;
		private RDResource rdResource = null;
		private ObserveTopResource obsResource = null;
		private Timer keepEpInRD = null;;

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

			// add Resource Directory resource
			
			addResource(rdResource = new RDResource());
			addResource(new RDLookUpTopResource(rdResource));
			addResource(obsResource = new ObserveTopResource());
			
			keepEpInRD = new Timer();
			keepEpInRD.schedule(new LifeTimeTask(this), 3*3600*1000);

		}

		
		class LifeTimeTask extends TimerTask{
			
			private AdminToolEndpoint ate;
			
			public LifeTimeTask(AdminToolEndpoint a) {
					ate=a;
			}
			
			@Override
			public void run() {
				ate.updateLifeTime();
				// TODO Auto-generated method stub
			}
			
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
		
		
		public void updateLifeTime(){
			for(RDNodeResource res : getEndpointObjects()){
				
				Date last = getLastHeardOf(res.getContext());
				if(last.getTime()> (new Date().getTime())-3600*1000){
					res.setLifeTime(Properties.std.getInt("DEFAULT_LIFE_TIME"));
				}
			}
		}
		
		public Set<String> listEndpoints(){
			TreeSet<String> result = new TreeSet<String>();
			for(Resource res : rdResource.getSubResources()){
				RDNodeResource nodeRes = (RDNodeResource) res;
				result.add(nodeRes.getEndpointIdentifier());
			}
			return result;
		}
		
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
		
		public int getPacketsRecivedActual(String id){
			RDNodeResource node = null;
			for(Resource res : rdResource.getSubResources()){
				RDNodeResource cur = (RDNodeResource) res;
				if(cur.getEndpointIdentifier().equals(id)){
					node=cur;
				}
			}
			if(node!=null){
				return getPacketsReceivedActualEp(node.getContext().substring(node.getContext().indexOf("//")+2));
			}
			return 0;
		}
		
		public int getPacketsReceivedActualEp(String ep){
			return obsResource.getPacketsReceivedActual(ep.replace("[","").replace("]",""));
		}
		
		public int getPacketsRecivedIdeal(String id){
			RDNodeResource node = null;
			for(Resource res : rdResource.getSubResources()){
				RDNodeResource cur = (RDNodeResource) res;
				if(cur.getEndpointIdentifier().equals(id)){
					node=cur;
				}
			}
			if(node!=null){
				return getPacketsReceivedIdealEp(node.getContext().substring(node.getContext().indexOf("//")+2));
			}
			return 0;
		}
		
		public int getPacketsReceivedIdealEp(String ep){
			return obsResource.getPacketsReceivedIdeal(ep.replace("[","").replace("]",""));
		}
		
		public Date getLastHeardOf(String id){
			RDNodeResource node = null;
			for(Resource res : rdResource.getSubResources()){
				RDNodeResource cur = (RDNodeResource) res;
				if(cur.getEndpointIdentifier().equals(id)){
					node=cur;
				}
			}
			if(node!=null){	
				return getLastHeardOfEp(node.getContext().substring(node.getContext().indexOf("//")+2));
			}
			return new Date(0);
		}
		
		public Date getLastHeardOfEp(String ep){
			return obsResource.getLastHeardOf(ep.replace("[","").replace("]",""));
		}
		
		
		public Set<Resource> getEPResources(String ep){
			LinkedList<Resource> todo = new LinkedList<Resource>();
			TreeSet<Resource> result = new TreeSet<Resource>();
			for(Resource res : rdResource.getSubResources()){
				if(((RDNodeResource) res).getEndpointIdentifier().equals(ep)){
					todo.add(res);						
				}
			}
			while(!todo.isEmpty()){
				Resource current = todo.pop();
				for(Resource res : current.getSubResources()){
					if(res.subResourceCount()!=0){
						todo.add(res);
					}
					else {
						result.add(res);
					}
				}
				
			}
			return result;		
		}
		
		public String getLastValue(String resource){
			Resource res = obsResource.getResource(resource.replace("[", "").replace("]",""));
			if (res ==null){
				return null;
			}
			ObservableResource obsRes = (ObservableResource) res;
			return obsRes.getLastPayload();
		}
		
		public String getEndpointDebug(String ep, String type){
			Resource res = obsResource.getResource(ep.replace("[", "").replace("]", "")+"/debug/heartbeat");
			if (res ==null){
				return null;
			}
			ObservableResource obsRes = (ObservableResource) res;
			String payload = obsRes.getLastPayload();
			HashMap<String,String> debugInfo = new HashMap<String,String>();
			for(String pair : payload.split("\n")){
				debugInfo.put(pair.substring(0, pair.indexOf(":")).trim(), pair.substring(pair.indexOf(":")+1).trim());
			}
			if(debugInfo.containsKey(type)){
				return debugInfo.get(type);
			}
			else {
				return null;
			}
		}
		
}

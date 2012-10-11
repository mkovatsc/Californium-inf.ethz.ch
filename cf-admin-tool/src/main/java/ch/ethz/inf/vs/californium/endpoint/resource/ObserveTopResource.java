
package ch.ethz.inf.vs.californium.endpoint.resource;

import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.resources.RDNodeResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;
import ch.ethz.inf.vs.californium.util.Properties;

public class ObserveTopResource extends LocalResource {
	
	private boolean hasRd;
	private boolean hasPersisting;
	private String rdUri;
	private String psUri;
	private Timer getEpTimer;
	private TreeSet<String> alreadyTestedResources;

	public ObserveTopResource() {
		this("observable");
	}

	public ObserveTopResource(String resourceIdentifier) {
		super(resourceIdentifier);
		hasRd=false;
		hasPersisting=false;
		GETRequest tmpReq = new GETRequest();
		if(Properties.std.containsKey("RD_ADDRESS")){
			String rdHost = Properties.std.getStr("RD_ADDRESS");
			rdUri = "coap://"+rdHost+"/rd-lookup/res";
			hasRd=tmpReq.setURI(rdUri);
			
		}
		if(Properties.std.containsKey("PS_ADDRESS")){
			String psHost = Properties.std.getStr("PS_ADDRESS");
			psUri = "coap://"+psHost+"/persistingservice/tasks";
			hasPersisting=tmpReq.setURI(psUri);
			
		}
		
		if(hasRd){
			getEpTimer = new Timer();
			getEpTimer.schedule(new getEpTask(this), 120*1000, 300*1000);
		}
		else{
			LOG.severe("ObserveManager: No Resource Directory specified");
		}
		if(!hasPersisting){
			LOG.severe("ObserveManager: No Persisting Service specified");
		}
		alreadyTestedResources = new TreeSet<String>();

	}

	/*
	 * Start observing the Resources specified in payload
	 * 
	 */
	@Override
	public void performPOST(POSTRequest request) {

		Response response;
		boolean error = false;
		ArrayList<ObservableResource> createdRessource = new ArrayList<ObservableResource>();
		
		Scanner scanner = new Scanner(request.getPayloadString());
		scanner.useDelimiter(",");
		ArrayList<String> pathResources = new ArrayList<String>();
		while (scanner.hasNext()) {
			pathResources.add(scanner.next());
		}
		TreeSet<String> hosts = new TreeSet<String>();
		
		for (String p : pathResources) {
			scanner = new Scanner(p);

			String uri = "", pathTemp = "";
			while ((pathTemp = scanner.findInLine("<coap://.*?>")) != null) {
				uri = pathTemp.substring(1, pathTemp.length() - 1);
			}
			if (uri==""){
				while ((pathTemp = scanner.findInLine("</.*?>")) != null) {
					uri = "coap://"+request.getUriPath()+pathTemp.substring(1, pathTemp.length() - 1);
				}
			}
			if(uri==""){
				error=true;
				break;
			}
			
			String identifier = uri.substring(uri.indexOf("//")+2);
			System.out.println(identifier);
		
			hosts.add(identifier.substring(0, identifier.indexOf("/")));
			
			if(alreadyTestedResources.contains(identifier)){
				continue;
			}
			alreadyTestedResources.add(identifier);	
			
			Resource existing = getResource(identifier);
			
			if (existing != null){
				continue;
			}
			
			
			
			ObservableResource resource = new ObservableResource(identifier, uri, this);
						
			scanner.useDelimiter(";");
			while (scanner.hasNext()) {
				LinkAttribute attrib=LinkAttribute.parse(scanner.next());
				if(attrib.getName()==LinkFormat.RESOURCE_TYPE){
					resource.setAttribute(attrib);
				}
			}
			
			
			createdRessource.add(resource);
		
		}
		for(String host: hosts){
			
			String identifier = host+"/debug/heartbeat";
			Resource existing = getResource(identifier);
			
			if(alreadyTestedResources.contains(identifier)){
				continue;
			}
			alreadyTestedResources.add(identifier);	
			
			if (existing != null){
				continue;
			}
			else{
				ObservableResource debug = new ObservableResource(identifier, "coap://"+identifier, this);
				createdRessource.add(debug);
			}
			
		}
		
		if (error){
			response = new Response(CodeRegistry.RESP_BAD_REQUEST);
		}
		else{
			for(ObservableResource res : createdRessource){
				add(res);
			}
			response = new Response(CodeRegistry.RESP_CREATED);
		}		
		// complete the request
		request.respond(response);
	}
	
	public boolean hasPersisting(){
		return hasPersisting;
	}
	
	public String getPsUri(){
		return psUri;
	}
	
	public void getResFromRd(){
		GETRequest rdLookup = new GETRequest();
		if(!rdLookup.setURI(rdUri)){
			getEpTimer.cancel();
			hasRd=false;
			return;
		}
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
						String identifier = uri.substring(uri.indexOf("//")+2);
						//System.out.println(identifier);
						
						if(alreadyTestedResources.contains(identifier)){
							continue;
						}
						alreadyTestedResources.add(identifier);	
						
		
						scanner.useDelimiter(";");
						
						List<LinkAttribute> linkAttributes = new ArrayList<LinkAttribute>();
						boolean isObs = false;
						
						while (scanner.hasNext()) {
							LinkAttribute attrib=LinkAttribute.parse(scanner.next());
								System.out.println(attrib.serialize());
								if(attrib.getName().equals(LinkFormat.RESOURCE_TYPE)){
									linkAttributes.add(attrib);
								}
								if(attrib.getName().equals(LinkFormat.OBSERVABLE) && (attrib.getStringValue().equals("1") || attrib.getStringValue().equalsIgnoreCase("true") )){
									linkAttributes.add(attrib);
									isObs=true;
									
								}
						}
						
						if(!isObs){
							continue;
						}
												
						Resource existing = getResource(identifier);
						
						if (existing != null){
							continue;
						}
						
											
						ObservableResource resource = new ObservableResource(identifier, uri, this);
						for(LinkAttribute attr : linkAttributes){
							resource.setAttribute(attr);
						}
						add(resource);
					
					}
				
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
	}
	
	
	
	
	
	
	class getEpTask extends TimerTask {
		ObserveTopResource resource;

		public getEpTask(ObserveTopResource res) {
			super();
			this.resource = res;
		}

		@Override
		public void run() {
			resource.getResFromRd();
			
		}
	}
	
	
	
	
	
	
	
	/*
	 * get for AdminTool
	 */
	
	private Set<ObservableResource> getObseravbleSubResources(){
		TreeSet<ObservableResource> result = new TreeSet<ObservableResource>();
		LinkedList<Resource> todo = new LinkedList<Resource>();
		todo.add(this);
		while(!todo.isEmpty()){
			Resource current = todo.pop();
			if(current.subResourceCount()>0){
				for(Resource res : current.getSubResources()){
					todo.add(res);
				}
			}
			if(current.getClass()==ObservableResource.class){
				result.add((ObservableResource) current);
			}
			
		}
		return result;
	}
	
	
	public int getPacketsReceivedActual(String ep){
		int count = 0;
		for (ObservableResource res : getObseravbleSubResources()){
			if (res.getHost().equals(ep)){
				count += res.getPacketsReceivedActual();
			}
		}
		return count;
	}
	
	public int getPacketsReceivedIdeal(String ep){
		int count = 0;
		for (ObservableResource res : getObseravbleSubResources()){
			if (res.getHost().equals(ep)){
				count += res.getPacketsReceivedIdeal();
			}
		}
		return count;
	}
	
	public Date getLastHeardOf(String ep){
		Date newest = new Date(0);
		for (ObservableResource res : getObseravbleSubResources()){
			if (res.getHost().equals(ep) && res.getLastHeardOf()!=null){
				if(newest.compareTo(res.getLastHeardOf()) < 0){
					newest = res.getLastHeardOf();
				}
			}
		}
		return newest;
	}
	
	

}

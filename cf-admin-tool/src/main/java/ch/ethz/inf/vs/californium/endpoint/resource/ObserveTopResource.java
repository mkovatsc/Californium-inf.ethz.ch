
package ch.ethz.inf.vs.californium.endpoint.resource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
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
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;
import ch.ethz.inf.vs.californium.util.Properties;

public class ObserveTopResource extends LocalResource {
	
	private boolean hasRd;
	private boolean hasPersisting;
	private String rdUri;
	private String psUri;
	private Timer getEpTimer;
	private InetAddress hostAddress;


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
			getEpTimer.schedule(new getEpTask(this), 20*1000, 300*1000);
		}
		else{
			LOG.severe("ObserveManager: No Resource Directory specified");
			System.exit(-1);
		}
		if(!hasPersisting){
			LOG.severe("ObserveManager: No Persisting Service specified");
		}
		try {
			hostAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * Start observing the Resources specified in payload
	 * 
	 */
	@Override
	public void performPOST(POSTRequest request) {

		Response response;
		
		Scanner scanner = new Scanner(request.getPayloadString());
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
				while ((pathTemp = scanner.findInLine("</.*?>")) != null) {
					uri = "coap://"+request.getUriPath()+pathTemp.substring(1, pathTemp.length() - 1);
				}
			}
			if(uri==""){
				continue;
			}
			
			String completePath = uri.substring(uri.indexOf("//")+2);
			//System.out.println(identifier);
			
			String host = completePath.substring(0,completePath.indexOf("/"));
			String resourcePath = completePath.substring(completePath.indexOf("/")+1);
			
			//Check is host already existing
			Resource existingHost = getResource(host);
			if(existingHost == null){
				existingHost = new ObservableNodeResource(host,this);
				add(existingHost);
			}
			
			
			scanner.useDelimiter(";");
			
			List<LinkAttribute> linkAttributes = new ArrayList<LinkAttribute>();
			boolean isObs = false;
			
			while (scanner.hasNext()) {
				LinkAttribute attrib=LinkAttribute.parse(scanner.next());
					//System.out.println(attrib.serialize());
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
									
			Resource existing = existingHost.getResource(resourcePath);
			
			if (existing != null){
				if(existing.getClass() == ObservableResource.class){
					((ObservableResource) existing).resendObserveRegistration();
					continue;
				}
			}
		
								
			ObservableResource resource = new ObservableResource(resourcePath, uri, (ObservableNodeResource) existingHost);
			for(LinkAttribute attr : linkAttributes){
				resource.setAttribute(attr);
			}
			existingHost.add(resource);
		
		}
		response = new Response(CodeRegistry.RESP_CREATED);
		
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
		rdLookup.setURI(rdUri);

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
						//System.out.println(identifier);
						
						String host = completePath.substring(0,completePath.indexOf("/"));
						String resourcePath = completePath.substring(completePath.indexOf("/")+1);
						
						//Check is host already existing
						Resource existingHost = getResource(host);
						if(existingHost == null){
							existingHost = new ObservableNodeResource(host,this);
							add(existingHost);
						}
						
						
						scanner.useDelimiter(";");
						
						List<LinkAttribute> linkAttributes = new ArrayList<LinkAttribute>();
						boolean isObs = false;
						
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
						}
						
						if(!isObs){
							continue;
						}
												
						Resource existing = existingHost.getResource(resourcePath);
						
						if (existing != null){
							if(existing.getClass() == ObservableResource.class){
								((ObservableResource) existing).resendObserveRegistration();
								continue;
							}
						}
					
											
						ObservableResource resource = new ObservableResource(resourcePath, uri, (ObservableNodeResource) existingHost);
						for(LinkAttribute attr : linkAttributes){
							resource.setAttribute(attr);
						}
						existingHost.add(resource);
					
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
	
	public InetAddress getHostAddress(){
		return hostAddress;
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
	
	public double getLossRate(String ep){
		for(Resource current: getSubResources()){
			if(current.getName().equals(ep)){
				return ((ObservableNodeResource) current).getLossRate();
			}
		}
		return -1;
	}
	
	public Date getLastHeardOf(String ep){
		for(Resource current: getSubResources()){
			if(current.getName().equals(ep)){
				return ((ObservableNodeResource) current).getLastHeardOf();
			}
		}
		return new Date(0);
	}
		
	

}

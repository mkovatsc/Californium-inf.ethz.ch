
package ch.ethz.inf.vs.californium.endpoint.resource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
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
						String context = uri.substring(uri.indexOf("//")+2);
						String completePath = context;
						//System.out.println(identifier);
						
						//String host = completePath.substring(0,completePath.indexOf("/"));
						String resourcePath = completePath.substring(completePath.indexOf("/")+1);
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
						

						if(!isObs || id.isEmpty()){
							continue;
						}
						
						//Check is host already existing
						Resource existingHost = getResource(id);
						if(existingHost == null){
							existingHost = new ObservableNodeResource(id, context, this);
							add(existingHost);
						}
						
						Resource existing = existingHost.getResource(resourcePath);
						
						if (existing != null){
							if(existing.getClass() == ObservableResource.class){
								((ObservableResource) existing).resendObserveRegistration(false);
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
	
	public double getLossRateId(String id){
		for(Resource current: getSubResources()){
			if(current.getName().equals(id)){
				return ((ObservableNodeResource) current).getLossRate();
			}
		}
		return -1;
	}
	
	public Date getLastHeardOfId(String id){
		for(Resource current: getSubResources()){
			if(current.getName().equals(id)){
				return ((ObservableNodeResource) current).getLastHeardOf();
			}
		}
		return new Date(0);
	}
		
	

}

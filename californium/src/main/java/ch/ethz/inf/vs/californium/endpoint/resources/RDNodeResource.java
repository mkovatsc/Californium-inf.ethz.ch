package ch.ethz.inf.vs.californium.endpoint.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.Properties;

public class RDNodeResource extends LocalResource {

	// Timer for lifeTime, remove current subResource if expired.
	private Timer removeTimer;
	private Timer validationTimer;
	private String endpointIdentifier;
	private String domain;
	private String endpointType;
	private String context;
	private int etag;
	
	
	public RDNodeResource(String identifier, int lifeTime, String endpointID, String dom, String type, String con) {
		super(identifier);
		setLifeTime(lifeTime);		
		setEndpointIdentifier(endpointID);
		setDomain(dom);
		setEndpointType(type);
		setContext(con);
		//Start Validation Timer (12 hour) 
		this.validationTimer = new Timer();
		validationTimer.schedule(new ValidationTask(this), 30*1000, 6*3600*1000);
				
	}

	/*
	 * add a new resource to the node. E.g. the resource temperature or
	 * humidity. If the path is /readings/temp, temp will be a subResource
	 * of readings, which is a subResource of the node.
	 */
	public LocalResource addNodeResource(String path) {
		Scanner scanner = new Scanner(path);
		scanner.useDelimiter("/");
		String next = "";
		boolean resourceExist = false;
		Resource resource = this; // It's the resource that represents the
									// Node
		LocalResource subResource = null;
		while (scanner.hasNext()) {
			resourceExist = false;
			next = scanner.next();
			for (Resource res : resource.getSubResources()) {
				if (res.getName().equals(next)) {
					subResource = (LocalResource) res;
					resourceExist = true;
				}
			}
			if (!resourceExist) {
				subResource = new LocalResource(next,true);
				resource.add(subResource);
			}
			resource = subResource;
		}
		subResource.setResourcesPath(this.getResourceIdentifier(false) + path);
		return subResource;
	}

	/*
	 * DELETEs this node resource
	 */
	@Override
	public void performDELETE(DELETERequest request) {
		// remove this resource
		if (removeTimer != null) {
			removeTimer.cancel();// delete the previous timer before it expire.
		}
		if (validationTimer != null){
			validationTimer.cancel();
		}
		remove();
		request.respond(CodeRegistry.RESP_DELETED);
	}

	/*
	 * PUTs content to this resource. PUT is a periodic request from the
	 * node to update the lifetime.
	 */
	@Override
	public void performPUT(PUTRequest request) {
		// System.out.println("PUT	"+this.getResourceIdentifier());
		setParameters(request.getPayloadString(), request.getOptions(OptionNumberRegistry.URI_QUERY));
		
		// complete the request
		request.respond(CodeRegistry.RESP_CHANGED);
	}

	/*
	 * set either a new lifetime (for new resources, POST request) or update
	 * the lifetime (for PUT request)
	 */
	public void setLifeTime(int lifeTime) {
		if (removeTimer != null) {
			removeTimer.cancel();// delete the previous timer before it expire.
		}
		removeTimer = new Timer();
		removeTimer.schedule(new RemoveTask(this), lifeTime * 1000);// from sec to ms
//		timer.schedule(new RemoveTask(this), 60 * 1000);// from sec to ms
	}

	/*
	 * This method is performed both for POST and for PUT request. set the
	 * Attribute of the resource (the node)
	 */
	public void setParameters(String payload, List<Option> query) {
		// scannering of the payload for setting parameters
		//attributes.clear();
		Scanner scanner = new Scanner(payload);
		LinkAttribute attr;
		int lifeTime = Properties.std.getInt("DEFAULT_LIFE_TIME");

		/*
		 * get lifetime from option query - only for PUT request.
		 */
		if (query != null) {
			for (Option opt : query) {
				attr = LinkAttribute.parse(opt.getStringValue());
				if (attr.getName().equals(LinkFormat.LIFE_TIME)) {
					lifeTime = attr.getIntValue();
					
				}
				if (attr.getName().equals(LinkFormat.CONTEXT)){
					setContext(attr.getStringValue());
				}
				if (attr.getName().equals(LinkFormat.END_POINT_TYPE)){
					setEndpointType(attr.getStringValue());
				}
			}
			//renew LifeTime
			setLifeTime(lifeTime);
		}

		/*
		 * Create a new subResource for each resource the node wants
		 * register. Each resource is separated by ",". E.g. A node can
		 * register a resource for reading the temperature and another one
		 * for reading the humidity.
		 */
		scanner.useDelimiter(",");
		List<String> pathResources = new ArrayList<String>();
		while (scanner.hasNext()) {
			pathResources.add(scanner.next());
		}

		for (String p : pathResources) {
			scanner = new Scanner(p);

			/*
			 * get the path of the endpoint's resource. E.g. from
			 * </readings/temp> it will select /readings/temp.
			 */
			String path = "", pathTemp = "";
			while ((pathTemp = scanner.findInLine("</.*?>")) != null) {
				path = pathTemp.substring(1, pathTemp.length() - 1);
			}
			LocalResource resource = addNodeResource(path);
			resource.attributes.clear();
			/*
			 * Since created the subResource, get all the attributes from
			 * the payload. Each parameter is separated by a ";".
			 */
			scanner.useDelimiter(";");
			while (scanner.hasNext()) {
				resource.setAttribute(LinkAttribute.parse(scanner.next()));
				// attr = LinkAttribute.parse(scanner.next());
				// String name = attr.getName();
				// if(name.equals(LinkFormat.RESOURCE_TYPE)){resource.setResourceType(attr.getStringValue());}
				// if(name.equals(LinkFormat.INSTANCE)){resource.setInstance(attr.getStringValue());}
				// if(name.equals(LinkFormat.DOMAIN)){resource.setDomain(attr.getStringValue());}
				// if(name.equals(LinkFormat.CONTEXT)){resource.setContext(attr.getStringValue());}
			}
			resource.setAttribute(new LinkAttribute(LinkFormat.END_POINT, getEndpointIdentifier()));
		}
	}

	
	/*
	 * the following three methods are used to print the right string to put in
	 * the payload to respond to the GET request.
	 */
		
	public String toLinkFormat(List<Option> query) {

		// Create new StringBuilder
		StringBuilder builder = new StringBuilder();
		
		// Build the link format
		buildLinkFormat(this, builder, query);

		// Remove last delimiter
		if (builder.length() > 0) {
			builder.deleteCharAt(builder.length() - 1);
		}

		return builder.toString();
	}

	public String toLinkFormatItem(Resource resource) {
		StringBuilder linkFormat = new StringBuilder();

		
		//linkFormat.append("<coap://");
		linkFormat.append("<"+getContext());
		linkFormat.append(resource.getPath().substring(this.getPath().length()));
		linkFormat.append(">");
		

		for (LinkAttribute attrib : resource.getAttributes()) {
			linkFormat.append(';');
			linkFormat.append(attrib.serialize());
			
		}

		return linkFormat.toString();
	}
	

	private void buildLinkFormat(Resource resource, StringBuilder builder, List<Option> query) {
		if (resource.totalSubResourceCount() > 0) {

			// Loop over all sub-resources
			for (Resource res : resource.getSubResources()) {
				// System.out.println(resource.getSubResources().size());
				// System.out.println(res.getName());
				if (LinkFormat.matches(res, query) && !res.getAttributes().isEmpty()) {

					// Convert Resource to string representation and add
					// delimiter
					builder.append(toLinkFormatItem(res));
					builder.append(',');
				}
				// Recurse
				buildLinkFormat(res, builder, query);
			}
		}
	}
	
	
	
	/*
	 * Setter And Getter
	 */

	public String getEndpointIdentifier() {
		return endpointIdentifier;
	}

	public void setEndpointIdentifier(String endpointIdentifier) {
		this.endpointIdentifier = endpointIdentifier;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getEndpointType() {
		return endpointType;
	}

	public void setEndpointType(String endpointType) {
		this.endpointType = endpointType;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public int getEtag() {
		return etag;
	}

	public void setEtag(int etag) {
		this.etag = etag;
	}

	
	
	public void validate(){
		GETRequest validationRequest = new GETRequest();
		validationRequest.setURI(getContext()+"/.well-known/core");
		validationRequest.setOption(new Option(getEtag(),OptionNumberRegistry.ETAG));
		validationRequest.enableResponseQueue(true);
		validationRequest.registerResponseHandler(new ValidationHandler());
		
		try {
			validationRequest.execute();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	class ValidationHandler implements ResponseHandler{

		@Override
		public void handleResponse(Response response) {
			if(response == null || response.getCode()==CodeRegistry.RESP_BAD_REQUEST){
				
			}
			else if(response.getCode() == CodeRegistry.RESP_VALID){
				LOG.finest("Resources up-to-date: "+getContext());
				
			}
			else if(response.getCode() == CodeRegistry.RESP_CONTENT){
				
				Option etagOption = null;
				etagOption = response.getFirstOption(OptionNumberRegistry.ETAG);
				if (etagOption == null){
					LOG.severe("Validation Not Supported by Endpoint: "+getContext()+"\nStop Validation Task");
					// If endpoint doesn't support etag validation on .well-known/core we can stop the validation
					if(validationTimer!=null ){
						validationTimer.cancel();
					}
					setParameters(response.getPayloadString(), null);
					LOG.fine("Updated Resources: "+getContext());
				}
				else {
					setParameters(response.getPayloadString(), null);
					setEtag(etagOption.getIntValue());
					LOG.fine("Updated Resources: "+getContext());
				}
			}
	
			
		}
		
	}
	
	
		
	
	class RemoveTask extends TimerTask {
		RDNodeResource resource;

		public RemoveTask(RDNodeResource resource) {
			super();
			this.resource = resource;
		}

		@Override
		public void run() {
			resource.remove();
			
		}
	}
	
	
	class ValidationTask extends TimerTask {
		RDNodeResource resource;

		public ValidationTask(RDNodeResource resource) {
			super();
			this.resource = resource;
		}

		@Override
		public void run() {
			resource.validate();
			
		}
	}
	
}

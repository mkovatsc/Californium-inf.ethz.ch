
package ch.ethz.inf.vs.californium.endpoint.resources;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.Properties;

public class RDResource extends LocalResource {

	public RDResource() {
		this("rd");
	}

	public RDResource(String resourceIdentifier) {
		super(resourceIdentifier);
		setResourceType("core.rd");
	}

	/*
	 * Creates a new sub-resource with the given identifier in this resource, or
	 * get it from the Option-query. Finally set the other parameters.
	 */
	@Override
	public void createSubResource(Request request, String newIdentifier) {

		// get name and lifetime from option query
		LinkAttribute attr;
		String endpointIdentifier = "";
		String domain= "default";
		String endType= "";
		String context= "";
		boolean mandatory = false;
		Response response;
		int lifeTime = Properties.std.getInt("DEFAULT_LIFE_TIME"); // default
																	// life time
																	// if not
																	// specified
		List<Option> query = request.getOptions(OptionNumberRegistry.URI_QUERY);
		if (query != null) {
			for (Option opt : query) {
				attr = LinkAttribute.parse(opt.getStringValue());
				// Mandatory Option
				if (attr.getName().equals(LinkFormat.DOMAIN)){
					domain = attr.getStringValue();
				}
				if (attr.getName().equals(LinkFormat.CONTEXT)){
					context = attr.getStringValue();
				}
				if (attr.getName().equals(LinkFormat.END_POINT_TYPE)){
					endType = attr.getStringValue();
				}
				if (attr.getName().equals(LinkFormat.END_POINT)) {
					endpointIdentifier = attr.getStringValue();
					mandatory = true;
					
				}
				// System.out.println(newIdentifier);}
				if (attr.getName().equals(LinkFormat.LIFE_TIME)) {
					lifeTime = attr.getIntValue();
				}
			}
		}

		RDNodeResource resource = null;
		
		for(Resource node : getSubResources()){
			if (((RDNodeResource) node).getDomain().equals(domain) && ((RDNodeResource) node).getEndpointIdentifier().equals(endpointIdentifier)){
				resource = (RDNodeResource) node; 
			}
		}
		
		if(!mandatory){
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
			return;
			
		}

		if (context==""){
			context = "coap://"+request.getPeerAddress().toString();
			
		}
			
		if (resource == null){
			resource = new RDNodeResource(newIdentifier, lifeTime, endpointIdentifier,domain, endType, context);
			add(resource);
			// create new response
			response = new Response(CodeRegistry.RESP_CREATED);
		}
		else{
			resource.setLifeTime(lifeTime);
			resource.setContext(context);
			
			response = new Response(CodeRegistry.RESP_CHANGED);
			
		}
		// set resourse's Parameters
		resource.setParameters(request.getPayloadString(), null);

		// TODO retrieve Address for DNS
		// request.getAddress();

		// inform client about the location of the new resource
		response.setLocationPath(resource.getPath());

		// complete the request
		request.respond(response);
	}


	/*
	 * POSTs a new sub-resource to this resource. The name of the new
	 * sub-resource is a random number if not specified in the Option-query.
	 */
	@Override
	public void performPOST(POSTRequest request) {
		// System.out.print("POST	");
		// get a random ID if no name specified
		int rndName;
		do {// while it found a subResource with the same name.
			rndName = (int) (Math.random() * 1000);
		} while (getResource(Integer.toString(rndName)) != null);

		// create the new resource(One for each node)
		createSubResource(request, Integer.toString(rndName));
	}
}

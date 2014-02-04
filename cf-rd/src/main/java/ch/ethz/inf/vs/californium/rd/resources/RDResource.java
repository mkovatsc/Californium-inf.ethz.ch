
package ch.ethz.inf.vs.californium.rd.resources;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.network.config.NetworkConfig;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class RDResource extends ResourceBase {

	public RDResource() {
		this("rd");
		getAttributes().addResourceType("core.rd");
	}

	public RDResource(String resourceIdentifier) {
		super(resourceIdentifier);
		getAttributes().addResourceType("core.rd");
	}

	/*
	 * POSTs a new sub-resource to this resource. The name of the new
	 * sub-resource is a random number if not specified in the Option-query.
	 */
	@Override
	public void handlePOST(CoapExchange exchange) {
		

		// get name and lifetime from option query
		LinkAttribute attr;
		String endpointIdentifier = "";
		String domain = NetworkConfig.getStandard().getString("RD_DEFAULT_DOMAIN");
		RDNodeResource resource = null;
		
		ResponseCode responseCode;
		
		List<String> query = exchange.getRequestOptions().getURIQueries();
		for (String q:query) {
			// FIXME Do not use Link attributes for URI template variables
			attr = LinkAttribute.parse(q);
			
			if (attr.getName().equals(LinkFormat.END_POINT)) {
				endpointIdentifier = attr.getValue();
			}
			
			if (attr.getName().equals(LinkFormat.DOMAIN)) {
				domain = attr.getValue();
			}
		}

		if (endpointIdentifier.equals("")) {
			exchange.respond(ResponseCode.BAD_REQUEST, "Missing endpoint (?ep)");
			return;
		}
		
		for (Resource node : getChildren()) {
			if (((RDNodeResource) node).getEndpointIdentifier().equals(endpointIdentifier) && ((RDNodeResource) node).getDomain().equals(domain)) {
				resource = (RDNodeResource) node;
			}
		}
		
		if (resource==null) {
			
			String randomName;
			do {
				randomName = Integer.toString((int) (Math.random() * 10000));
			} while (getChild(randomName) != null);
			
			resource = new RDNodeResource(randomName, endpointIdentifier, domain);
			add(resource);
			
			responseCode = ResponseCode.CREATED;
		} else {
			responseCode = ResponseCode.CHANGED;
		}
		
		// set parameters of resource
		if (!resource.setParameters(exchange.advanced().getRequest())) {
			resource.delete();
			exchange.respond(ResponseCode.BAD_REQUEST);
			return;
		}

		// inform client about the location of the new resource
		exchange.setLocationPath(resource.getURI());

		// complete the request
		exchange.respond(responseCode);
	}

}

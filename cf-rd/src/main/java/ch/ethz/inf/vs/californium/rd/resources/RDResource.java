
package ch.ethz.inf.vs.californium.endpoint.resources;

import java.util.List;

import org.apache.http.client.utils.URIBuilder;

import ch.ethz.inf.vs.californium.coap.GETRequest;
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
		setResourceType("core.rd");
	}

	public RDResource(String resourceIdentifier) {
		super(resourceIdentifier);
		setResourceType("core.rd");
	}

	/*
	 * POSTs a new sub-resource to this resource. The name of the new
	 * sub-resource is a random number if not specified in the Option-query.
	 */
	@Override
	public void performPOST(POSTRequest request) {
		

		// get name and lifetime from option query
		LinkAttribute attr;
		String endpointIdentifier = "";
		String domain = Properties.std.getStr("RD_DEFAULT_DOMAIN");
		RDNodeResource resource = null;
		
		Response response;
		
		List<Option> query = request.getOptions(OptionNumberRegistry.URI_QUERY);
		if (query != null) {
			for (Option opt : query) {
				
				// FIXME Do not use Link attributes for URI template variables
				attr = LinkAttribute.parse(opt.getStringValue());
				
				if (attr.getName().equals(LinkFormat.END_POINT)) {
					endpointIdentifier = attr.getValue();
				}
				
				if (attr.getName().equals(LinkFormat.DOMAIN)) {
					domain = attr.getValue();
				}
			}
		}

		if (endpointIdentifier.equals("")) {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Missing endpoint (?ep)");
			return;
		}
		
		for (Resource node : getSubResources()) {
			if (((RDNodeResource) node).getEndpointIdentifier().equals(endpointIdentifier) && ((RDNodeResource) node).getDomain().equals(domain)) {
				resource = (RDNodeResource) node;
			}
		}
		
		if (resource==null) {
			
			String randomName;
			do {
				randomName = Integer.toString((int) (Math.random() * 10000));
			} while (getResource(randomName) != null);
			
			resource = new RDNodeResource(randomName, endpointIdentifier, domain);
			add(resource);
			
			response = new Response(CodeRegistry.RESP_CREATED);
		} else {
			response = new Response(CodeRegistry.RESP_CHANGED);
		}
		
		// set resourse's Parameters
		if (!resource.setParameters(request)) {
			resource.remove();
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
			return;
		}

		// inform client about the location of the new resource
		response.setLocationPath(resource.getPath());

		// complete the request
		request.respond(response);
	}

}

package ch.ethz.inf.vs.californium.endpoint.resources;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

public class RDLookUpEPResource extends LocalResource {

	private RDResource rdResource = null;
	
	public RDLookUpEPResource(String resourceIdentifier, RDResource rd) {
		super(resourceIdentifier);
		this.rdResource = rd;
	}

	
	@Override
	public void performGET(GETRequest request) {
		Set<Resource> resources = rdResource.getSubResources();
		List<Option> query = request.getOptions(OptionNumberRegistry.URI_QUERY);
		String result = "";
		String domainQuery = "";
		String endpointQuery = "";
		TreeSet<String> endpointTypeQuery = new TreeSet<String>();
		Response response = null;
		
		if (query != null) {
			LinkAttribute attr;
			for (Option opt : query) {
				attr = LinkAttribute.parse(opt.getStringValue());
				if(attr.getName().equals(LinkFormat.DOMAIN)){
					domainQuery = attr.getStringValue();
				}
				if(attr.getName().equals(LinkFormat.END_POINT)){
					endpointQuery = attr.getStringValue();
					
				}
				if(attr.getName().equals(LinkFormat.END_POINT_TYPE)){
					Collections.addAll(endpointTypeQuery, attr.getStringValue().split(" "));
				}
			}
		}
		
		Iterator<Resource>  resIt = resources.iterator();
		
		while (resIt.hasNext()){
			Resource res = resIt.next();
			if (res.getClass() == RDNodeResource.class){
				RDNodeResource node = (RDNodeResource) res;
				if ( (domainQuery.isEmpty() || domainQuery.equals(node.getDomain())) && 
					 (endpointQuery.isEmpty() || endpointQuery.equals(node.getEndpointIdentifier())) &&
					 (endpointTypeQuery.isEmpty() || endpointTypeQuery.contains(node.getEndpointType())) &&
					 node.isActive()) {
				
					result += "<"+node.getContext()+">;"+LinkFormat.END_POINT+"=\""+node.getEndpointIdentifier()+"\"";
					if(!node.getLocation().isEmpty()){
						result += ";loc=\""+node.getLocation()+"\"";
					}
					result += ",";
				}
			}
		}
		if(result.isEmpty()){
			response = new Response(CodeRegistry.RESP_NOT_FOUND);
		}
		else{
			response = new Response(CodeRegistry.RESP_CONTENT);
			response.setPayload(result.substring(0,result.length()-1),MediaTypeRegistry.APPLICATION_LINK_FORMAT);
		}
		
		request.respond(response);
	}
}
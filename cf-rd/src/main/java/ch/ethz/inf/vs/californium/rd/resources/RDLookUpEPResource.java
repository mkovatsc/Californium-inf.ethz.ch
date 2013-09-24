package ch.ethz.inf.vs.californium.rd.resources;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class RDLookUpEPResource extends ResourceBase {

	private RDResource rdResource = null;
	
	public RDLookUpEPResource(String resourceIdentifier, RDResource rd) {
		super(resourceIdentifier);
		this.rdResource = rd;
	}

	
	@Override
	public void handleGET(CoapExchange exchange) {
		Collection<Resource> resources = rdResource.getChildren();
		List<String> query = exchange.getRequestOptions().getURIQueries();
		String result = "";
		String domainQuery = "";
		String endpointQuery = "";
		TreeSet<String> endpointTypeQuery = new TreeSet<String>();
		
		for (String q:query) {
			LinkAttribute attr = LinkAttribute.parse(q);
			if(attr.getName().equals(LinkFormat.DOMAIN)){
				domainQuery = attr.getValue();
			}
			if(attr.getName().equals(LinkFormat.END_POINT)){
				endpointQuery = attr.getValue();
				
			}
			if(attr.getName().equals(LinkFormat.END_POINT_TYPE)){
				Collections.addAll(endpointTypeQuery, attr.getValue().split(" "));
			}
		}
		
		Iterator<Resource>  resIt = resources.iterator();
		
		while (resIt.hasNext()){
			Resource res = resIt.next();
			if (res.getClass() == RDNodeResource.class){
				RDNodeResource node = (RDNodeResource) res;
				if ( (domainQuery.isEmpty() || domainQuery.equals(node.getDomain())) && 
					 (endpointQuery.isEmpty() || endpointQuery.equals(node.getEndpointIdentifier())) &&
					 (endpointTypeQuery.isEmpty() || endpointTypeQuery.contains(node.getEndpointType()))) {
				
					result += "<"+node.getContext()+">;"+LinkFormat.END_POINT+"=\""+node.getEndpointIdentifier()+"\"";
					result += ";"+LinkFormat.DOMAIN+"=\""+node.getDomain()+"\"";
					if(!node.getEndpointType().isEmpty()){
						result += ";"+LinkFormat.RESOURCE_TYPE+"=\""+node.getEndpointType()+"\"";
					}
							
					result += ",";
				}
			}
		}
		if(result.isEmpty()){
			exchange.respond(ResponseCode.NOT_FOUND);
		}
		else{
			exchange.respond(ResponseCode.CONTENT, result.substring(0,result.length()-1), MediaTypeRegistry.APPLICATION_LINK_FORMAT);
		}
		
	}
}
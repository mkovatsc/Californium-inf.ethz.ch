package ch.ethz.inf.vs.californium.rd.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class RDLookUpResResource extends ResourceBase {

	private RDResource rdResource = null;
	
	public RDLookUpResResource(String resourceIdentifier, RDResource rd) {
		super(resourceIdentifier);
		this.rdResource = rd;
	}

	
	@Override
	public void handleGET(CoapExchange exchange) {
		Collection<Resource> resources = rdResource.getChildren();
		String result = "";
		String domainQuery = "";
		String endpointQuery = "";
		List<String> toRemove = new ArrayList<String>(); 
		
		List<String> query = exchange.getRequestOptions().getURIQueries();
		
		for (String q : query) {
			LinkAttribute attr = LinkAttribute.parse(q);
			if(attr.getName().equals(LinkFormat.DOMAIN)){
				domainQuery=attr.getValue();
				if(domainQuery==null){
					exchange.respond(ResponseCode.BAD_REQUEST);
					return;
				}
				toRemove.add(q);
			}
			if(attr.getName().equals(LinkFormat.END_POINT)){
				endpointQuery = attr.getValue();
				if(endpointQuery==null){
					exchange.respond(ResponseCode.BAD_REQUEST);
					return;
				}
				toRemove.add(q);
			}
		}
		
		
		Iterator<Resource>  resIt = resources.iterator();
		System.out.println(endpointQuery);
				
		query.removeAll(toRemove);
		
		while (resIt.hasNext()){
			Resource res = resIt.next();
			if (res.getClass() == RDNodeResource.class){
				RDNodeResource node = (RDNodeResource) res;
				if ( (domainQuery.isEmpty() || domainQuery.equals(node.getDomain())) && 
					 (endpointQuery.isEmpty() || endpointQuery.equals(node.getEndpointIdentifier())) ) {
					String link = node.toLinkFormat(query);
					result += (!link.isEmpty()) ? link+"," : ""; 
				}
			}
		}
		if(result.isEmpty()){
			exchange.respond(ResponseCode.NOT_FOUND);
		}
		else{
			exchange.respond(ResponseCode.CONTENT, result.substring(0,result.length()-1),MediaTypeRegistry.APPLICATION_LINK_FORMAT);
		}
		
	}
}
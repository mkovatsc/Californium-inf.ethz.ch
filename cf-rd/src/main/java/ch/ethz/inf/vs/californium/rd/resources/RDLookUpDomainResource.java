package ch.ethz.inf.vs.californium.rd.resources;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class RDLookUpDomainResource extends ResourceBase {

	private RDResource rdResource = null;
	
	public RDLookUpDomainResource(String resourceIdentifier, RDResource rd) {
		super(resourceIdentifier);
		this.rdResource = rd;
	}
	
	
	
	@Override
	public void handleGET(CoapExchange exchange) {
		
		Collection<Resource> resources = rdResource.getChildren();
		TreeSet<String> availableDomains = new TreeSet<String>(); 
		String domainQuery = ""; 
		Iterator<Resource>  resIt = resources.iterator();
		String result = "";
		
		List<String> queries = exchange.getRequestOptions().getURIQueries();
		for (String query:queries) {
			LinkAttribute attr = LinkAttribute.parse(query);
			if (attr.getName().equals(LinkFormat.DOMAIN))
				domainQuery = attr.getValue();
		}
		
		while (resIt.hasNext()){
			Resource res = resIt.next();
			if (res.getClass() == RDNodeResource.class){
				RDNodeResource node = (RDNodeResource) res;
				if ((domainQuery.isEmpty() || domainQuery.equals(node.getDomain()))){
					availableDomains.add(node.getDomain());
				}
			}
		}
		if(availableDomains.isEmpty()){
			exchange.respond(ResponseCode.NOT_FOUND);
			
		} else{
			Iterator<String>  domIt = availableDomains.iterator();
						
			while (domIt.hasNext()){
				String dom = domIt.next();
				result += "</rd>;"+LinkFormat.DOMAIN+"=\""+dom+"\",";
			}

			exchange.respond(ResponseCode.CONTENT, result.substring(0, result.length()-1), MediaTypeRegistry.APPLICATION_LINK_FORMAT);
		}
				
	}

}

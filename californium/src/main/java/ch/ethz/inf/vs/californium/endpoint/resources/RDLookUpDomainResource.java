package ch.ethz.inf.vs.californium.endpoint.resources;

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

public class RDLookUpDomainResource extends LocalResource {

	private RDResource rdResource = null;
	
	public RDLookUpDomainResource(String resourceIdentifier, RDResource rd) {
		super(resourceIdentifier);
		this.rdResource = rd;
	}
	
	
	
	@Override
	public void performGET(GETRequest request) {
		
		Set<Resource> resources = rdResource.getSubResources();
		TreeSet<String> availableDomains = new TreeSet<String>(); 
		String domainQuery = ""; 
		Iterator<Resource>  resIt = resources.iterator();
		String result = "";
		
		Response response = null;
	
		List<Option> query = request.getOptions(OptionNumberRegistry.URI_QUERY);
		if (query != null) {
			LinkAttribute attr;
			for (Option opt : query) {
				attr = LinkAttribute.parse(opt.getStringValue());
				if(attr.getName().equals(LinkFormat.DOMAIN)){
					domainQuery = attr.getStringValue();
				}
			}
		}
		
		while (resIt.hasNext()){
			Resource res = resIt.next();
			if (res.getClass() == RDNodeResource.class){
				RDNodeResource node = (RDNodeResource) res;
				if ((domainQuery.isEmpty() || domainQuery.equals(node.getDomain())) && node.isActive()){
					availableDomains.add(node.getDomain());
				}
			}
		}
		if(availableDomains.isEmpty()){
			response = new Response(CodeRegistry.RESP_NOT_FOUND);
		}
		else{
			Iterator<String>  domIt = availableDomains.iterator();
						
			while (domIt.hasNext()){
				String dom = domIt.next();
				result += "</rd>;"+LinkFormat.DOMAIN+"=\""+dom+"\",";
			}
			response =  new Response(CodeRegistry.RESP_CONTENT);
			response.setPayload(result.substring(0, result.length()-1), MediaTypeRegistry.APPLICATION_LINK_FORMAT);
			
		}
				
		request.respond(response);
	}

}

package ch.ethz.inf.vs.californium.endpoint.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

public class RDLookUpResResource extends LocalResource {

	private RDResource rdResource = null;
	
	public RDLookUpResResource(String resourceIdentifier, RDResource rd) {
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
		List<Option> toRemove = new ArrayList<Option>(); 
		
		
		if (query != null) {
			LinkAttribute attr;
			for (Option opt : query) {
				attr = LinkAttribute.parse(opt.getStringValue());
				if(attr.getName().equals(LinkFormat.DOMAIN)){
					domainQuery=attr.getStringValue();
					toRemove.add(opt);
				}
				if(attr.getName().equals(LinkFormat.END_POINT)){
					endpointQuery = attr.getStringValue();
					toRemove.add(opt);
				}
			}
		}
		
		
		Iterator<Resource>  resIt = resources.iterator();
		System.out.println(endpointQuery);
				
		Response response = null;
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
			response = new Response(CodeRegistry.RESP_NOT_FOUND);
		}
		else{
			response = new Response(CodeRegistry.RESP_CONTENT);
			response.setPayload(result.substring(0,result.length()-1),MediaTypeRegistry.APPLICATION_LINK_FORMAT);
		}
		
		request.respond(response);
	}
}
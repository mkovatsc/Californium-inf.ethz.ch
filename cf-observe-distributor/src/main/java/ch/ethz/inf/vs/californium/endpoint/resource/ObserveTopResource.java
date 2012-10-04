
package ch.ethz.inf.vs.californium.endpoint.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;

import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;
import ch.ethz.inf.vs.californium.util.Properties;

public class ObserveTopResource extends LocalResource {

	public ObserveTopResource() {
		this("observable");
	}

	public ObserveTopResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	/*
	 * Creates a new sub-resource with the given identifier in this resource, or
	 * get it from the Option-query. Finally set the other parameters.
	 */
	@Override
	public void performPOST(POSTRequest request) {

		// get name and lifetime from option query

		Response response;
		boolean error = false;
		ArrayList<ObservableResource> createdRessource = new ArrayList<ObservableResource>();
		
		/*
		LinkAttribute attr;
		String context = "";
		List<Option> query = request.getOptions(OptionNumberRegistry.URI_QUERY);
		if (query != null) {
			for (Option opt : query) {
				attr = LinkAttribute.parse(opt.getStringValue());
				if (attr.getName().equals(LinkFormat.CONTEXT)) {
					context = attr.getStringValue();
				}
			}
		}
		*/
		
		Scanner scanner = new Scanner(request.getPayloadString());
		scanner.useDelimiter(",");
		ArrayList<String> pathResources = new ArrayList<String>();
		while (scanner.hasNext()) {
			pathResources.add(scanner.next());
		}

		for (String p : pathResources) {
			scanner = new Scanner(p);

			String uri = "", pathTemp = "";
			while ((pathTemp = scanner.findInLine("<coap://.*?>")) != null) {
				uri = pathTemp.substring(1, pathTemp.length() - 1);
			}
			if (uri==""){
				while ((pathTemp = scanner.findInLine("</.*?>")) != null) {
					uri = "coap://"+request.getUriPath()+pathTemp.substring(1, pathTemp.length() - 1);
				}
			}
			if(uri==""){
				error=true;
				break;
			}
			
			String identifier = uri.substring(uri.indexOf("//")+1);
		
			Resource existing = getResource(identifier);
			
			if (existing != null){
				continue;
			}
			
			ObservableResource resource = new ObservableResource(identifier, uri);
						
			/* Currently not adding LinkAttributes			
			scanner.useDelimiter(";");
			while (scanner.hasNext()) {
				resource.setAttribute(LinkAttribute.parse(scanner.next()));
			}
			*/
			
			createdRessource.add(resource);
		
		}
		
		if (error){
			response = new Response(CodeRegistry.RESP_BAD_REQUEST);
		}
		else{
			for(ObservableResource res : createdRessource){
				add(res);
			}
			response = new Response(CodeRegistry.RESP_CREATED);
		}		
		// complete the request
		request.respond(response);
	}

}

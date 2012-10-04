package ch.ethz.inf.vs.californium.endpoint.resources;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.LocalEndpoint;

public class RDLookUpTopResource extends LocalResource {
	
	private RDResource rdResource = null;
	
	public RDLookUpTopResource(RDResource rd){
		this("rd-lookup", rd);
	}

	public RDLookUpTopResource(String resourceIdentifier, RDResource rd) {
		super(resourceIdentifier);
		this.rdResource = rd;
		setResourceType("core.rd-lookup");
		add(new RDLookUpDomainResource("domain", rd));
		add(new RDLookUpEPResource("ep", rd));
		add(new RDLookUpResResource("res", rd));
	}
	

}

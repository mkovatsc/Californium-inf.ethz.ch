package ch.ethz.inf.vs.californium.rd.resources;

import ch.ethz.inf.vs.californium.server.resources.ResourceBase;


public class RDLookUpTopResource extends ResourceBase {
	
//	private RDResource rdResource = null;
	
	public RDLookUpTopResource(RDResource rd){
		this("rd-lookup", rd);
	}

	public RDLookUpTopResource(String resourceIdentifier, RDResource rd) {
		super(resourceIdentifier);
//		this.rdResource = rd;
		getAttributes().addResourceType("core.rd-lookup");
		add(new RDLookUpDomainResource("d", rd));
		add(new RDLookUpEPResource("ep", rd));
		add(new RDLookUpResResource("res", rd));
	}
}

package ch.ethz.inf.vs.californium.endpoint.resources;


public class RDLookUpTopResource extends LocalResource {
	
	private RDResource rdResource = null;
	
	public RDLookUpTopResource(RDResource rd){
		this("rd-lookup", rd);
	}

	public RDLookUpTopResource(String resourceIdentifier, RDResource rd) {
		super(resourceIdentifier);
		this.rdResource = rd;
		setResourceType("core.rd-lookup");
		add(new RDLookUpDomainResource("d", rd));
		add(new RDLookUpEPResource("ep", rd));
		add(new RDLookUpResResource("res", rd));
	}
}

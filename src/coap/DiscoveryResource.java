package coap;

/*
 * This class describes the functionality of a CoAP discovery entry point.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class DiscoveryResource extends ReadOnlyResource {

	// Constants ///////////////////////////////////////////////////////////////

	// the default resource identifier for resource discovery
	public static final String DEFAULT_IDENTIFIER = "core";

	// Constructors ////////////////////////////////////////////////////////////

	/*
	 * Constructor for a new DiscoveryResource
	 * 
	 * @param resources The resources used for the discovery
	 */
	public DiscoveryResource(Resource root) {
		super(DEFAULT_IDENTIFIER);

		this.root = root;

		setContentTypeCode(MediaTypeRegistry.LINK_FORMAT);
	}

	// REST Operations /////////////////////////////////////////////////////////

	@Override
	public void performGET(GETRequest request) {

		// TODO filtering etc.

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);

		// return resources in link-format
		response.setPayload(root.toLinkFormat(), getContentTypeCode());

		// complete the request
		request.respond(response);
	}

	// Attributes //////////////////////////////////////////////////////////////

	// the root resource used for the discovery
	private Resource root;
}

package endpoint;

import java.util.List;

import coap.CodeRegistry;
import coap.GETRequest;
import coap.MediaTypeRegistry;
import coap.Option;
import coap.OptionNumberRegistry;
import coap.Response;


/*
 * This class describes the functionality of a CoAP discovery entry point.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class DiscoveryResource extends LocalResource {

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

		setContentTypeCode(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
	}

	// REST Operations /////////////////////////////////////////////////////////

	@Override
	public void performGET(GETRequest request) {

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		
		// get filter query
		List<Option> query = request.getOptions(OptionNumberRegistry.URI_QUERY);

		// return resources in link-format
		response.setPayload(root.toLinkFormat(query), getContentTypeCode());

		// complete the request
		request.respond(response);
	}

	// Attributes //////////////////////////////////////////////////////////////

	// the root resource used for the discovery
	private Resource root;
}

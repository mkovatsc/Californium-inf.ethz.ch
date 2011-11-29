package endpoint;

import java.net.SocketException;

import util.Properties;
import coap.CodeRegistry;
import coap.Communicator;
import coap.OptionNumberRegistry;
import coap.GETRequest;
import coap.PUTRequest;
import coap.Request;
import coap.Response;

public class LocalEndpoint extends Endpoint {
	
	public static final String ENDPOINT_INFO = 
		"************************************************************\n" +
		"This CoAP endpoint is using the Californium (Cf) framework\n" +
		"developed by Dominique Im Obersteg & Daniel Pauli.\n" +
		"\n" +
		"Institute for Pervasive Computing, ETH Zurich, Switzerland\n" +
		"Contact: Matthias Kovatsch <kovatsch@inf.ethz.ch>\n" +
		"************************************************************";

	private class RootResource extends LocalResource {

		public RootResource() {
			super("");
		}

		@Override
		public void performGET(GETRequest request) {

			// create response
			Response response = new Response(CodeRegistry.RESP_CONTENT);

			response.setPayload(ENDPOINT_INFO);

			// complete the request
			request.respond(response);
		}
	}

	// TODO Constructor with custom root resource; check for resourceIdentifier==""
	
	public LocalEndpoint(int port, int defaultBlockSize) throws SocketException {

		// initialize communicator
		this.communicator = new Communicator(port, false, defaultBlockSize);
		this.communicator.registerReceiver(this);

		// initialize resources
		this.rootResource = new RootResource();

		this.wellKnownResource = new LocalResource(".well-known", true);
		this.wellKnownResource.setResourceType("");

		this.discoveryResource = new DiscoveryResource(rootResource);

		rootResource.addSubResource(wellKnownResource);
		wellKnownResource.addSubResource(discoveryResource);

	}

	public LocalEndpoint(int port) throws SocketException {
		this(port, Properties.std.getInt("DEFAULT_BLOCK_SIZE"));
	}
	
	public LocalEndpoint() throws SocketException {
		this(Properties.std.getInt("DEFAULT_PORT"));
	}

	@Override
	public void execute(Request request) {

		// check if request exists
		if (request != null) {

			// retrieve resource identifier
			String resourceIdentifier = request.getUriPath();

			// lookup resource
			LocalResource resource = getResource(resourceIdentifier);

			// check if resource available
			if (resource != null) {

				// invoke request handler of the resource
				request.dispatch(resource);

				// check if resource is to be observed
				if (request instanceof GETRequest && request.hasOption(OptionNumberRegistry.OBSERVE)) {

					// establish new observation relationship
					resource.addObserveRequest((GETRequest) request);

				} else if (resource.isObserved(request.endpointID())) {

					// terminate observation relationship on that resource
					resource.removeObserveRequest(request.endpointID());
				}
			
			} else if (request instanceof PUTRequest) {
				// allows creation of non-existing resources through PUT
				this.createByPUT((PUTRequest) request);
				
			} else {
				// resource does not exist
				System.out.printf("[%s] Resource not found: '%s'\n", getClass().getName(), resourceIdentifier);

				request.respond(CodeRegistry.RESP_NOT_FOUND);
			}
		}
	}

	// delegate to createNew() of top resource
	private void createByPUT(PUTRequest request) {

		String identifier = request.getUriPath(); // always starts with "/"
		
		// find existing parent up the path
		String parentIdentifier = new String(identifier);
		String newIdentifier = "";
		Resource parent = null;
		// will end at rootResource ("")
		do {
			newIdentifier = identifier.substring(parentIdentifier.lastIndexOf('/')+1);
			parentIdentifier = parentIdentifier.substring(0, parentIdentifier.lastIndexOf('/'));
			System.out.println(parentIdentifier);
			System.out.println(newIdentifier);
		} while ((parent = getResource(parentIdentifier))==null);

		parent.createSubResource(request, newIdentifier);
	}

	public void addResource(LocalResource resource) {
		if (rootResource != null) {
			rootResource.addSubResource(resource);
		}
	}

	public void removeResource(String resourceIdentifier) {
		if (rootResource != null) {
			rootResource.removeSubResource(resourceIdentifier);
		}
	}

	public LocalResource getResource(String resourceIdentifier) {
		if (rootResource != null) {
			return (LocalResource) rootResource.getResource(resourceIdentifier);
		} else {
			return null;
		}
	}

	@Override
	public void handleRequest(Request request) {
		execute(request);
	}

	@Override
	public void handleResponse(Response response) {
		// response.handle();
	}

	private Resource wellKnownResource;
	private DiscoveryResource discoveryResource;
}

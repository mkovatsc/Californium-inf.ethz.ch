package endpoint;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.List;

import layers.TransferLayer;

import coap.*;

public class LocalEndpoint extends Endpoint {

	private class RootResource extends ReadOnlyResource {

		public RootResource() {
			super("");
			setResourceType("root");
		}

		@Override
		public void performGET(GETRequest request) {

			// create response
			Response response = new Response(CodeRegistry.RESP_CONTENT);
			ByteArrayOutputStream data = new ByteArrayOutputStream();
			PrintStream out = new PrintStream(data);

			printEndpointInfo(out);

			response.setPayload(data.toByteArray());

			// complete the request
			request.respond(response);
		}
	}

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
		this(port, TransferLayer.DEFAULT_BLOCK_SIZE);
	}
	
	public LocalEndpoint() throws SocketException {
		this(DEFAULT_PORT);
	}

	@Override
	public void execute(Request request) {

		// check if request exists
		if (request != null) {

			// retrieve resource identifier
			String resourceIdentifier = getResourceIdentifier(request);

			// lookup resource
			LocalResource resource = getResource(resourceIdentifier);

			// check if resource available
			if (resource != null) {

				// invoke request handler of the resource
				request.dispatch(resource);

				// check if resource is to be observed
				if (request instanceof GETRequest
						&& request.hasOption(OptionNumberRegistry.OBSERVE)) {

					// establish new observation relationship
					resource.addObserveRequest((GETRequest) request);

				} else if (resource.isObserved(request.endpointID())) {

					// terminate observation relationship on that resource
					resource.removeObserveRequest(request.endpointID());
				}

			} else if (request instanceof PUTRequest) {

				createByPUT((PUTRequest) request);
			} else {

				// resource does not exist
				System.out.printf("[%s] Resource not found: '%s'\n", getClass()
						.getName(), resourceIdentifier);

				request.respond(CodeRegistry.RESP_NOT_FOUND);
			}
		}
	}

	private void createByPUT(PUTRequest request) {

		String identifier = getResourceIdentifier(request);
		int pos = identifier.lastIndexOf('/');
		if (pos != -1 && pos < identifier.length() - 1) {
			String parentIdentifier = identifier.substring(0, pos);
			String newIdentifier = identifier.substring(pos + 1);
			Resource parent = getResource(parentIdentifier);
			if (parent != null) {
				parent.createNew(request, newIdentifier);
			} else {
				request.respond(
						CodeRegistry.RESP_NOT_FOUND,
						String.format(
								"Unable to create '%s' in '%s': Parent does not exist.",
								newIdentifier, parentIdentifier));
			}
		} else {
			// not allowed to create new root resources
			request.respond(CodeRegistry.RESP_FORBIDDEN);
		}
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

	private static String[] getResourcePath(Request request) {
		List<Option> options = request
				.getOptions(OptionNumberRegistry.URI_PATH);
		if (options != null && options.size() > 0) {
			String first = options.get(0).getStringValue();
			// for backwards compability with draft 3 where
			// there is only one Uri-Path option containing slashes
			if (first.indexOf('/') >= 0) {
				return first.split("/");
			} else {
				String[] path = new String[options.size()];
				for (int i = 0; i < path.length; i++) {
					path[i] = options.get(i).getStringValue();
				}
				return path;
			}
		} else {
			return null;
		}
	}

	private static String getResourceIdentifier(Request request) {

		return Option.join(request.getOptions(OptionNumberRegistry.URI_PATH),
				"/");

		/*
		 * List<Option> uriPaths =
		 * request.getOptions(OptionNumberRegistry.URI_PATH);
		 * 
		 * if (uriPaths == null) return "";
		 * 
		 * StringBuilder builder = new StringBuilder(); for (Option uriPath :
		 * uriPaths) { builder.append('/');
		 * builder.append(uriPath.getStringValue()); } return
		 * builder.toString();
		 */
	}

	private Resource wellKnownResource;
	private DiscoveryResource discoveryResource;

	@Override
	public void handleRequest(Request request) {
		execute(request);
	}

	@Override
	public void handleResponse(Response response) {
		// response.handle();
	}

	protected void printEndpointInfo(PrintStream out) {

		// print disclaimer etc.
		out.println("************************************************************");
		out.println("This CoAP endpoint is using the Californium library");
		out.println("developed by Dominique Im Obersteg & Daniel Pauli");
		out.println();
		out.println("Institute for Pervasive Computing, ETH Zurich, Switzerland");
		out.println("Contact: Matthias Kovatsch <kovatsch@inf.ethz.ch>");
		out.println("************************************************************");
	}

}

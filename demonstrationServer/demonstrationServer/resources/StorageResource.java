package demonstrationServer.resources;

import coap.CodeRegistry;
import coap.DELETERequest;
import coap.GETRequest;
import coap.LocalResource;
import coap.MediaTypeRegistry;
import coap.Option;
import coap.OptionNumberRegistry;
import coap.POSTRequest;
import coap.PUTRequest;
import coap.Request;
import coap.Response;

/*
 * This class implements a 'storage' resource for demonstration purposes.
 * 
 * Defines a resource that stores POSTed data and that creates new
 * sub-resources on PUT request where the Uri-Path doesn't yet point to an
 * existing resource.
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class StorageResource extends LocalResource {

	public StorageResource(String resourceIdentifier) {
		super(resourceIdentifier);
		setResourceType("POST your data here or PUT new resources!");
	}

	public StorageResource() {
		this("storage");
		isRoot = true;
	}

	@Override
	public void performGET(GETRequest request) {

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);

		if (request.hasFormat(MediaTypeRegistry.LINK_FORMAT) || data == null) {

			// respond with list of sub-resources in link format
			response.setPayload(toLinkFormat(), MediaTypeRegistry.LINK_FORMAT);

		} else {

			// load data into payload
			response.setPayload(data);

			// set content type
			response.setOption(contentType);
		}

		// complete the request
		request.respond(response);
	}

	@Override
	public void performPOST(POSTRequest request) {

		String payload = request.getPayloadString();
		if (payload != null && !payload.isEmpty()) {

			// create new sub-resources along the path
			// whenever necessary
			StorageResource resource = this;
			for (String path : payload.split("/")) {
				if (!path.isEmpty()) {
					StorageResource sub = (StorageResource) subResource(path);
					if (sub == null) {
						sub = new StorageResource(path);
						resource.addSubResource(sub);
					}
					resource = sub;
				}
			}

			// store payload
			resource.storeData(request);

			// create new response
			Response response = new Response(CodeRegistry.RESP_CREATED);

			// inform client about the location of the new resource
			response.setLocationPath(resource.getResourcePath());

			// complete the request
			request.respond(response);

		} else {

			// complete the request
			request.respond(CodeRegistry.RESP_BAD_REQUEST,
					"Payload must contain Uri-Path for new sub-resource.");
		}
	}

	@Override
	public void performPUT(PUTRequest request) {

		// store payload
		storeData(request);

		// complete the request
		request.respond(CodeRegistry.RESP_CHANGED);
	}

	@Override
	public void performDELETE(DELETERequest request) {

		// disallow to remove the root "storage" resource
		if (!isRoot) {

			// remove this resource
			remove();

			request.respond(CodeRegistry.RESP_DELETED);
		} else {
			request.respond(CodeRegistry.RESP_FORBIDDEN);
		}
	}

	@Override
	public void createNew(PUTRequest request, String newIdentifier) {

		// create new sub-resource
		StorageResource resource = new StorageResource(newIdentifier);
		addSubResource(resource);

		// store payload
		resource.storeData(request);

		// create new response
		Response response = new Response(CodeRegistry.RESP_CREATED);

		// inform client about the location of the new resource
		response.setLocationPath(resource.getResourcePath());

		// complete the request
		request.respond(response);
	}

	private void storeData(Request request) {

		// set payload and content type
		data = request.getPayload();
		contentType = request.getFirstOption(OptionNumberRegistry.CONTENT_TYPE);

		// signal that resource state changed
		changed();
	}

	private byte[] data;
	private Option contentType;
	private boolean isRoot;
}

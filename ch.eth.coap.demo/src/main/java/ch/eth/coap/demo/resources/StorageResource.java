package ch.eth.coap.demo.resources;

import ch.eth.coap.coap.CodeRegistry;
import ch.eth.coap.coap.DELETERequest;
import ch.eth.coap.coap.GETRequest;
import ch.eth.coap.coap.MediaTypeRegistry;
import ch.eth.coap.coap.POSTRequest;
import ch.eth.coap.coap.PUTRequest;
import ch.eth.coap.coap.Request;
import ch.eth.coap.coap.Response;
import ch.eth.coap.endpoint.LocalResource;

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

	// Constructors ////////////////////////////////////////////////////////////
	
	/*
	 * Default constructor.
	 */
	public StorageResource() {
		this("storage");
	}
	
	/*
	 * Constructs a new storage resource with the given resourceIdentifier.
	 */
	public StorageResource(String resourceIdentifier) {
		super(resourceIdentifier);
		setResourceTitle("PUT your data here or POST new resources!");
		setResourceType("Storage");
		setObservable(true);
	}

	// REST Operations /////////////////////////////////////////////////////////
	
	/*
	 * GETs the content of this storage resource. 
	 * If the content-type of the request is set to application/link-format 
	 * or if the resource does not store any data, the contained sub-resources
	 * are returned in link format.
	 */
	@Override
	public void performGET(GETRequest request) {

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);

		// check if link format requested
		if (request.hasFormat(MediaTypeRegistry.APPLICATION_LINK_FORMAT) || data == null) {

			// respond with list of sub-resources in link format
			response.setPayload(toLinkFormat(), MediaTypeRegistry.APPLICATION_LINK_FORMAT);

		} else {

			// load data into payload
			response.setPayload(data);

			// set content type
			response.setContentType(getContentTypeCode());
		}

		// complete the request
		request.respond(response);
	}
	
	/*
	 * PUTs content to this resource.
	 */
	@Override
	public void performPUT(PUTRequest request) {

		// store payload
		storeData(request);

		// complete the request
		request.respond(CodeRegistry.RESP_CHANGED);
	}

	/*
	 * POSTs a new sub-resource to this resource.
	 * The name of the new sub-resource is retrieved from the request
	 * payload.
	 */
	@Override
	public void performPOST(POSTRequest request) {

		// get request payload as a string
		String payload = request.getPayloadString();
		
		// check if valid Uri-Path specified
		if (payload != null && !payload.isEmpty()) {

			createNew(request, payload);

		} else {

			// complete the request
			request.respond(CodeRegistry.RESP_BAD_REQUEST,
				"Payload must contain Uri-Path for new sub-resource.");
		}
	}

	/*
	 * Creates a new sub-resource with the given identifier in this
	 * resource, recursively creating sub-resources along the Uri-Path
	 * if necessary.
	 */
	@Override
	public void createNew(Request request, String newIdentifier) {
		
		// omit leading and trailing slashes
		if (newIdentifier.startsWith("/")) {
			newIdentifier = newIdentifier.substring(1);
		}
		if (newIdentifier.endsWith("/")) {
			newIdentifier = newIdentifier.substring(0, newIdentifier.length()-1);
		}
		
		int delim = newIdentifier.indexOf('/');
		if (delim < 0) {

			// create new sub-resource
			StorageResource resource = (StorageResource)subResource(newIdentifier);
			if (resource == null) {
				
				resource = new StorageResource(newIdentifier);
				addSubResource(resource);
		
				// store payload
				resource.storeData(request);
		
				// create new response
				Response response = new Response(CodeRegistry.RESP_CREATED);
		
				// inform client about the location of the new resource
				response.setLocationPath(resource.getResourcePath());
		
				// complete the request
				request.respond(response);
			
			} else {
				
				// resource already exists; update data
				storeData(request);
				
				// complete the request
				request.respond(CodeRegistry.RESP_CHANGED);
			}
			
		} else {
			
			// split path in parent and sub identifier
			String parentIdentifier = newIdentifier.substring(0, delim);
			newIdentifier = newIdentifier.substring(delim+1);
			
			// retrieve corresponding sub-resource, create if necessary
			StorageResource sub = (StorageResource) subResource(parentIdentifier);
			if (sub == null) {
				sub = new StorageResource(parentIdentifier);
				addSubResource(sub);
			}
			
			// delegate creation to the sub-resource
			sub.createNew(request, newIdentifier);
		}
	}
	
	/*
	 * DELETEs this storage resource, if it is not root.
	 */
	@Override
	public void performDELETE(DELETERequest request) {

		// disallow to remove the root "storage" resource
		if (parent instanceof StorageResource) {

			// remove this resource
			remove();

			request.respond(CodeRegistry.RESP_DELETED);
		} else {
			request.respond(CodeRegistry.RESP_FORBIDDEN,
				"Root storage resource cannot be deleted");
		}
	}

	// Internal ////////////////////////////////////////////////////////////////
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private void storeData(Request request) {

		// set payload and content type
		data = request.getPayload();
		setContentTypeCode(request.getContentType());

		// signal that resource state changed
		changed();
	}

	private byte[] data; 
}

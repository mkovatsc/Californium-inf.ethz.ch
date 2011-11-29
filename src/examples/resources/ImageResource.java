package examples.resources;

import java.io.File;
import java.io.FileInputStream;

import coap.CodeRegistry;
import coap.GETRequest;
import coap.MediaTypeRegistry;
import coap.Response;
import endpoint.LocalResource;

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
public class ImageResource extends LocalResource {

	// Constructors ////////////////////////////////////////////////////////////
	
	/*
	 * Default constructor.
	 */
	public ImageResource() {
		this("image");
	}
	
	/*
	 * Constructs a new storage resource with the given resourceIdentifier.
	 */
	public ImageResource(String resourceIdentifier) {
		super(resourceIdentifier);
		setResourceTitle("GET an image with different content-types");
		setResourceType("Image");
		setAttributeValue("sz", "46383");
		setAttributeValue("ct", "23");
		setAttributeValue("ct", "22");
		setObservable(false);
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
		
		String filename = "data/image/";
		int ct = MediaTypeRegistry.IMAGE_PNG;
		
		switch (request.getAccept()) {
			case MediaTypeRegistry.IMAGE_GIF:
				filename += "image.gif";
				ct = MediaTypeRegistry.IMAGE_GIF;
				break;
			case MediaTypeRegistry.IMAGE_JPEG:
				filename += "image.jpg";
				ct = MediaTypeRegistry.IMAGE_JPEG;
				break;
			case MediaTypeRegistry.IMAGE_PNG:
			case MediaTypeRegistry.UNDEFINED:
				filename += "image.png";
				break;
			case MediaTypeRegistry.IMAGE_TIFF:
				filename += "image.tif";
				ct = MediaTypeRegistry.IMAGE_TIFF;
				break;
			default:
				request.respond(CodeRegistry.RESP_NOT_ACCEPTABLE, "Accept GIF, JPEG, PNG, or TIFF");
				return;
		}

			
		//create file object
		File file = new File(filename);
		
		if (!file.exists()) {
			request.respond(CodeRegistry.RESP_NOT_FOUND, file.getPath());
			return;
		}
		
		//get length of file
		int fileLength = (int)file.length();
		
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try
		{
		  //open input stream from file
		  fileIn = new FileInputStream(file);
		  //read file into byte array
		  fileIn.read(fileData);
		  fileIn.close();
		} catch (Exception e) {
			request.respond(CodeRegistry.RESP_INTERNAL_SERVER_ERROR, "IO error");
			return;
		}
		
		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		
		response.setPayload(fileData);

		// set content type
		response.setContentType(ct);

		// complete the request
		request.respond(response);
	}
}

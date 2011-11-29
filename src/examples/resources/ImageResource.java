package examples.resources;

import java.io.File;
import java.io.FileInputStream;

import coap.CodeRegistry;
import coap.GETRequest;
import coap.MediaTypeRegistry;
import coap.Response;
import endpoint.LocalResource;

/*
 * This class implements an 'image' resource for demonstration purposes.
 * 
 * Provides different representations of an image through supports content
 * negotiation.
 * The required files are provided in the "run" directory for the .jar version.
 * Make sure to fix the location when running elsewhere.
 *  
 * @author Matthias Kovatsch
 * @version 1.0
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
		setAttributeValue("sz", "18029");
		setAttributeValue("ct", "21");
		setAttributeValue("ct", "22");
		setAttributeValue("ct", "23");
		setAttributeValue("ct", "24");
		setObservable(false);
	}

	// REST Operations /////////////////////////////////////////////////////////
	
	@Override
	public void performGET(GETRequest request) {
		
		String filename = "image/";
		int ct = MediaTypeRegistry.IMAGE_PNG;
		
		// content negotiation
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

		//load representation from file
		File file = new File(filename);
		
		if (!file.exists()) {
			request.respond(CodeRegistry.RESP_INTERNAL_SERVER_ERROR, "Representation not found");
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

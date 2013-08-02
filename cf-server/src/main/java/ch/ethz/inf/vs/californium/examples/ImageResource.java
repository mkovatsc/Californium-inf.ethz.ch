package ch.ethz.inf.vs.californium.examples;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.resources.ResourceAttributes;
import ch.ethz.inf.vs.californium.resources.ResourceBase;

/**
 * This resource responds with an image to GET requests. Use the ACCEPT option
 * to choose between the JPG and PNG file.
 * 
 * @author Matthias Kovatsch, Martin Lanter
 */
public class ImageResource extends ResourceBase {

	private String filePath = "src/main/java/ch/inf/vs/californium/example/";
	private String fileName = "image";
	
	private List<Integer> supported = new ArrayList<Integer>();

	public ImageResource(String resourceIdentifier) {
		super(resourceIdentifier);
		
		ResourceAttributes attributes = getAttributes();
		attributes.setTitle("GET an image with different content-types");
		attributes.addResourceType("Image");
		
		supported.add(MediaTypeRegistry.IMAGE_PNG);
		supported.add(MediaTypeRegistry.IMAGE_JPEG);
		
		for (int ct : supported) {
			attributes.addContentType(ct);
		}
		
		attributes.setMaximumSizeEstimate(18029);
	}

	@Override
	public void processGET(Exchange exchange) {
		Integer ct = exchange.getRequest().getOptions().getAccept();
		if (ct != null) {
			if (!supported.contains(ct)) {
				exchange.respond(new Response(ResponseCode.NOT_ACCEPTABLE));
				return;
			}
		} else {
			ct = MediaTypeRegistry.IMAGE_PNG;
		}
		
		String filename = filePath + fileName + "." + MediaTypeRegistry.toFileExtension(ct);

		//load representation from file
		System.out.println("Search file "+filename+", "+new File(filename).getAbsolutePath());
		File file = new File(filename);
		
		if (!file.exists()) {
			Response response = new Response(ResponseCode.INTERNAL_SERVER_ERROR);
			response.setPayload("Image file not found");
			exchange.respond(response);
			return;
		}
		
		try {
		
			byte[] fileData = Files.readAllBytes(Paths.get(filename));//new byte[fileLength];
			
			// create response
			Response response = new Response(ResponseCode.CONTENT);
			response.setPayload(fileData);
			response.getOptions().setContentFormat(ct);

			exchange.respond(response);
			
		} catch (Exception e) {
			Response response = new Response(ResponseCode.INTERNAL_SERVER_ERROR);
			response.setPayload("I/O error");
			exchange.respond(response);
		}
	}
	
}

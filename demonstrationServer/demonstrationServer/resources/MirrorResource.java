package demonstrationServer.resources;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import coap.CodeRegistry;
import coap.DELETERequest;
import coap.GETRequest;
import coap.Option;
import coap.POSTRequest;
import coap.PUTRequest;
import coap.Request;
import coap.Response;
import endpoint.LocalResource;

/*
 * This class implements a mirror resource for demonstration purposes.
 * 
 * Defines a resource that echos a POST request
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class MirrorResource extends LocalResource {
	public MirrorResource() {
		super("mirror");
		setResourceTitle("POST request to receive it back as echo");
		setResourceType("RequestMirroring");
	}

	@Override
	public void performPOST(POSTRequest request) {

		/*// retrieve text to convert from payload
		byte[] payload = request.getPayload();
		Map<Integer, List<Option>> options = request.getOptionMap();
		
		Response response = new Response (CodeRegistry.V3_RESP_OK);
		response.setPayload(payload);
		response.setOptionMap(options);

		// complete the request
		request.respond(response);*/
		mirrorRequest(request, "POST");
	}
	
	@Override
	public void performGET(GETRequest request) {
		mirrorRequest(request, "GET");
	}

	@Override
	public void performPUT(PUTRequest request) {
		mirrorRequest(request, "PUT");
	}

	@Override
	public void performDELETE(DELETERequest request) {
		mirrorRequest(request, "DELETE");
	}
	
	private void mirrorRequest(Request request, String requestType) {
		// retrieve text to convert from payload
		byte[] payload = request.getPayload();
		int initialPayloadLength = payload.length;
		Map<Integer, List<Option>> options = request.getOptionMap();
		
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(data);
		request.log(out);
		Response response = new Response (CodeRegistry.RESP_CONTENT);
		response.setPayload(data.toByteArray());
		response.setOptionMap(options);
		if ((initialPayloadLength == 0) && (requestType.equals("DELETE")))
		{
			response.setCode(CodeRegistry.RESP_DELETED);
		}

		// complete the request
		request.respond(response);
	}
}

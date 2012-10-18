package ch.ethz.inf.vs.californium.endpoint.resource;

import java.text.SimpleDateFormat;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;

public class LastHeardOfResource extends LocalResource{
	
	private ObservableNodeResource parent;
	
	public LastHeardOfResource(ObservableNodeResource par){
		super("lastheardof");
		parent = par;
	}
	
	@Override
	public void performGET(GETRequest request){
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		response.setPayload(dateFormat.format(parent.getLastHeardOf()));
		request.respond(response);
	}

}

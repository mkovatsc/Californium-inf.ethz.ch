package ch.ethz.inf.vs.californium.endpoint.resource;

import java.text.DecimalFormat;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;

public class LossRateResource extends LocalResource{
	
	private ObservableNodeResource parent;
	
	public LossRateResource(ObservableNodeResource par){
		super("lossrate");
		parent = par;
	}
	
	@Override
	public void performGET(GETRequest request){
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		if(parent.getLossRate()<0){
			response.setPayload("Not Received Enough Packets");
		}
		else{
			DecimalFormat df = new DecimalFormat("#.##");
			response.setPayload(df.format(parent.getLossRate()));
		}
		request.respond(response);
	}

}

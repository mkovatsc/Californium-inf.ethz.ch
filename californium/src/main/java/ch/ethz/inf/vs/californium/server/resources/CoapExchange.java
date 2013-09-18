package ch.ethz.inf.vs.californium.server.resources;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.OptionSet;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;

public class CoapExchange {

	private Exchange exchange;
	
	private ResourceBase resource;
	
	protected CoapExchange(Exchange exchange, ResourceBase resource) {
		if (exchange == null) throw new NullPointerException();
		if (resource == null) throw new NullPointerException();
		this.exchange = exchange;
		this.resource = resource;
	}
	
	public Code getRequestCode() {
		return exchange.getRequest().getCode();
	}
	
	public OptionSet getRequestOptions() {
		return exchange.getRequest().getOptions();
	}
	
	public byte[] getRequestPayload() {
		return exchange.getRequest().getPayload();
	}
	
	public String getRequestPayloadString() {
		return exchange.getRequest().getPayloadString();
	}
	
	public void accept() {
		exchange.accept();
	}
	
	public void reject() {
		exchange.reject();
	}
	
	public void respond(ResponseCode code) {
		respond(new Response(code));
	}
	
	public void respond(String payload) {
		respond(ResponseCode.CONTENT, payload);
	}
	
	public void respond(ResponseCode code, String payload) {
		Response response = new Response(code);
		response.setPayload(payload);
		response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
		respond(response);
	}
	
	public void respond(ResponseCode code, byte[] payload) {
		Response response = new Response(code);
		response.setPayload(payload);
		respond(response);
	}
	
	public void respond(Response response) {
		if (response == null) throw new NullPointerException();
		resource.respond(exchange, response);
	}
}

package ch.inf.vs.californium.coap;

import java.util.Arrays;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.network.Endpoint;
import ch.inf.vs.californium.network.EndpointManager;


public class Request extends Message {

	private final CoAP.Code code;
	
	private Response response;
	
	private Object lock;
	
	public Request(Code code) {
		super(Type.NCON);
		this.code = code;
	}
	
	public Code getCode() {
		return code;
	}
	
	public void send() {
		validateBeforeSending();
		EndpointManager.getEndpointManager().getDefaultEndpoint().sendRequest(this);
	}
	
	public void send(Endpoint endpoint) {
		validateBeforeSending();
		endpoint.sendRequest(this);
	}
	
	private void validateBeforeSending() {
		if (getDestination() == null)
			throw new NullPointerException("Destination is null");
		if (getDestinationPort() == 0)
			throw new NullPointerException("Destination port is 0");
	}

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
		
		if (lock != null)
			synchronized (lock) {
				lock.notifyAll();
			}
		// else: we know that nobody is waiting on the lock
	}
	
	public Response waitForResponse() throws InterruptedException {
		return waitForResponse(0);
	}
	
	public Response waitForResponse(long timeout) throws InterruptedException {
		// Lazy initialization of a lock
		if (lock == null) {
			synchronized (this) {
				if (lock == null)
					lock = new Object();
			}
		}
		// wait for response
		synchronized (lock) {
			while (response == null /* TODO: and not canceled*/) {
				lock.wait(timeout);
				if (timeout > 0) // TODO: Only when time has elapsed
					return response;
			}
		}
		return response;
	}
	
	public void cancel() {
		if (lock != null) {
			synchronized (lock) {
				lock.notifyAll();
			}
		}
		// TODO: cancel exchange
	}
	
	@Override
	public String toString() {
		String payload;
		if (getPayloadSize() <= 24)
			payload = "\""+getPayloadString()+"\"";
		else payload = "\""+getPayloadString().substring(0,20)+".. "+getPayloadSize()+" bytes\"";
		return getType()+"-"+code+"-Request: MID="+getMid()+", Token="+Arrays.toString(getToken())+", "+getOptions()+", Payload="+payload+"";
	}
}

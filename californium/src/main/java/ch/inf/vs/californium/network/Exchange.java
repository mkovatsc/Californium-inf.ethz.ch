package ch.inf.vs.californium.network;

import java.util.concurrent.ScheduledFuture;

import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;

public class Exchange {

	private Endpoint endpoint;
	
	private Request request; // the initial request we have to exchange
	private Request currentRequest; // Matching needs to know for what we expect a response
	private RequestBlockAssembler requestAssembler;
	
	/*
	 * Single, possibly blockwise
	 * Multi
	 * Observer, possibly blockwise
	 */
	private ResponseBlockAssembler responseAssembler;
	private Response currentResponse; // Retransmission needs 
//	private Response response; // the current response
	
	// true if the local server has initiated exchange
	private final boolean fromLocal;
	
	private boolean timeouted;
	
	private ScheduledFuture<?> retransmissionHandle;
	
	private int currentTimeout;
	private int transmissionCount = 0;
	
	private boolean complete; // true when all request and responses (blocks) have been exchanged
	
	public Exchange(Request request, boolean fromLocal) {
		this.currentRequest = request; // might only be a block
		this.fromLocal = fromLocal;
	}
	
	public void accept() {
		assert(!fromLocal);
		if (request.getType() == Type.CON && !request.isAcknowledged()) {
			request.setAcknowledged(true);
			EmptyMessage ack = EmptyMessage.newACK(request);
			endpoint.sendEmptyMessage(this, ack);
		}
	}
	
	public void reject() {
		assert(!fromLocal);
		request.setRejected(true);
		EmptyMessage rst = EmptyMessage.newRST(request);
		endpoint.sendEmptyMessage(this, rst);
	}
	
	public void respond(Response response) {
		assert(endpoint != null);
		// TODO: Should this routing stuff be done within a layer?
		response.setMid(request.getMid());
		response.setDestination(request.getSource());
		response.setDestinationPort(request.getSourcePort());
		this.currentResponse = response;
		endpoint.sendResponse(this, response);
	}
	
	public Request getRequest() {
		return request;
	}
	
	public void setRequest(Request request) {
		this.request = request; // by blockwise layer
	}

//	public Response getResponse() {
//		return response;
//	}
//
//	public void setResponse(Response response) {
//		this.response = response;
//	}
	
	public boolean isFromLocal() {
		return fromLocal;
	}

//	public boolean isRequestAcknowledged() {
//		return requestAcknowledged.get();
//	}
//
//	public boolean getAndSetRequestAcknowledged(boolean requestAcknowledged) {
//		request.setAcknowledged(true);
//		return this.requestAcknowledged.getAndSet(requestAcknowledged);
//	}
//
//	public boolean isResponseAcknowledged() {
//		return responseAcknowledged.get();
//	}
//
//	public boolean  getAndSetResponseAcknowledged(boolean responseAcknowledged) {
//		response.setAcknowledged(true);
//		return this.responseAcknowledged.getAndSet(responseAcknowledged);
//	}
//	
//	public boolean isRequestRejected() {
//		return requestRejected.get();
//	}
//	
//	public boolean getAndSetRequestRejected(boolean requestRejected) {
//		request.setRejected(true);
//		return this.requestRejected.getAndSet(requestRejected);
//	}
//	
//	public boolean isResponseRejected() {
//		return responseRejected.get();
//	}
//	
//	public boolean getAndSetResponseRejected(boolean responseRejected) {
//		response.setRejected(true);
//		return this.responseRejected.getAndSet(responseRejected);
//	}

	public boolean isTimeouted() {
		return timeouted;
	}

	public void setTimeouted(boolean timeouted) {
		this.timeouted = timeouted;
	}

	public int getTransmissionCount() {
		return transmissionCount;
	}

	public void setTransmissionCount(int transmissionCount) {
		this.transmissionCount = transmissionCount;
	}

	public int getCurrentTimeout() {
		return currentTimeout;
	}

	public void setCurrentTimeout(int currentTimeout) {
		this.currentTimeout = currentTimeout;
	}


	public Endpoint getEndpoint() {
		return endpoint;
	}


	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	public Request getCurrentRequest() {
		return currentRequest;
	}

	public void setCurrentRequest(Request currentRequest) {
		this.currentRequest = currentRequest;
	}

	public RequestBlockAssembler getRequestAssembler() {
		return requestAssembler;
	}

	public void setRequestAssembler(RequestBlockAssembler requestAssembler) {
		this.requestAssembler = requestAssembler;
	}

	public Response getCurrentResponse() {
		return currentResponse;
	}

	public void setCurrentResponse(Response currentResponse) {
		this.currentResponse = currentResponse;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public ResponseBlockAssembler getResponseAssembler() {
		return responseAssembler;
	}

	public void setResponseAssembler(ResponseBlockAssembler responseAssembler) {
		this.responseAssembler = responseAssembler;
	}

	public ScheduledFuture<?> getRetransmissionHandle() {
		return retransmissionHandle;
	}

	public void setRetransmissionHandle(ScheduledFuture<?> retransmissionHandle) {
		this.retransmissionHandle = retransmissionHandle;
	}
	
}

package ch.inf.vs.californium.network;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import ch.inf.vs.californium.coap.BlockOption;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.CoAP.Type;
import ch.inf.vs.californium.coap.EmptyMessage;
import ch.inf.vs.californium.coap.Message;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.layer.BlockwiseStatus;
import ch.inf.vs.californium.observe.ObserveRelation;

public class Exchange {
	
	/**
	 * The origin of an exchange. If Cf receives a new request and creates a new
	 * exchange the origin is REMOTE since the request has been initiated from a
	 * remote endpoint. If Cf creates a new request and sends it, the origin is
	 * LOCAL.
	 */
	public enum Origin {
		LOCAL, REMOTE;
	}

	// TODO: When implementing observer we need to be able to make threads stop
	// modifying the exchange. A thread working on blockwise transfer might
	// access fields that are about to change with each new response. The same
	// mech. can be used to cancel an exchange. Use an AtomicInteger to count
	// threads that are currently working on the exchange.
	
	private Endpoint endpoint;
	
	/** An observer to be called when a request is complete */
	private ExchangeObserver observer;
	
	private boolean complete;
	private long timestamp;
	
	private Request request; // the initial request we have to exchange
	private Request currentRequest; // Matching needs to know for what we expect a response
	private BlockwiseStatus requestBlockStatus;
	
	private Response response;
	private Response currentResponse; // Matching needs to know when receiving duplicate
	private BlockwiseStatus responseBlockStatus;
	
	// indicates where the request of this exchange has been initiated.
	// (as suggested by effective Java, item 40.)
	private final Origin origin;
	
	// true if the exchange has failed due to a timeout
	private boolean timeouted;
	
	// the timeout of the current request or response set by reliability layer
	private int currentTimeout;
	
	// the amount of attempted transmissions that have not succeeded yet
	private int transmissionCount = 0;

	// handle to cancel retransmission
	private ScheduledFuture<?> retransmissionHandle;
	
	// If the request was sent with a block1 option the response has to send its
	// first block piggy-backed with the Block1 option of the last request block
	private BlockOption block1ToAck;
	
	private ObserveRelation observeRelation;
	
	public Exchange(Request request, Origin origin) {
		this.currentRequest = request; // might only be the first block of the whole request
		this.origin = origin;
		this.timestamp = System.currentTimeMillis();
	}
	
	public void accept() {
		assert(origin == Origin.REMOTE);
		if (request.getType() == Type.CON && !request.isAcknowledged()) {
			request.setAcknowledged(true);
			EmptyMessage ack = EmptyMessage.newACK(request);
			endpoint.sendEmptyMessage(this, ack);
		}
	}
	
	public void reject() {
		assert(origin == Origin.REMOTE);
		request.setRejected(true);
		EmptyMessage rst = EmptyMessage.newRST(request);
		endpoint.sendEmptyMessage(this, rst);
	}
	
	public void respond(String content) {
		Response response = new Response(ResponseCode.CONTENT);
		response.setPayload(content.getBytes());
		respond(response);
	}
	
	public void respond(Response response) {
		assert(endpoint != null);
		if (observeRelation != null) {
			response.getOptions().setObserve(observeRelation.getNextObserveNumber());
		}
		// TODO: Should this routing stuff be done within a layer?
		if (request.getType() == Type.CON && !request.isAcknowledged()) {
			response.setMid(request.getMid()); // TODO: Careful with MIDs
			request.setAcknowledged(true);
		}
		response.setDestination(request.getSource());
		response.setDestinationPort(request.getSourcePort());
		this.currentResponse = response;
		endpoint.sendResponse(this, response);
	}

	public Origin getOrigin() {
		return origin;
	}
	
	public Request getRequest() {
		return request;
	}
	
	public void setRequest(Request request) {
		this.request = request; // by blockwise layer
	}

	public Request getCurrentRequest() {
		return currentRequest;
	}

	public void setCurrentRequest(Request currentRequest) {
		this.currentRequest = currentRequest;
	}

	public BlockwiseStatus getRequestBlockStatus() {
		return requestBlockStatus;
	}

	public void setRequestBlockStatus(BlockwiseStatus requestBlockStatus) {
		this.requestBlockStatus = requestBlockStatus;
	}

	public Response getResponse() {
		return response;
	}

	// TODO: make pakcage private? Becaause developer might use it to send Resp back
	public void setResponse(Response response) {
		this.response = response;
	}

	public Response getCurrentResponse() {
		return currentResponse;
	}

	public void setCurrentResponse(Response currentResponse) {
		this.currentResponse = currentResponse;
	}

	public BlockwiseStatus getResponseBlockStatus() {
		return responseBlockStatus;
	}

	public void setResponseBlockStatus(BlockwiseStatus responseBlockStatus) {
		this.responseBlockStatus = responseBlockStatus;
	}

	public BlockOption getBlock1ToAck() {
		return block1ToAck;
	}

	public void setBlock1ToAck(BlockOption block1ToAck) {
		this.block1ToAck = block1ToAck;
	}
	
	public Endpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

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

	public ScheduledFuture<?> getRetransmissionHandle() {
		return retransmissionHandle;
	}

	public void setRetransmissionHandle(ScheduledFuture<?> retransmissionHandle) {
		this.retransmissionHandle = retransmissionHandle;
	}

	public void setObserver(ExchangeObserver observer) {
		this.observer = observer;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
		observer.completed(this);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public ObserveRelation getObserveRelation() {
		return observeRelation;
	}

	public void setObserveRelation(ObserveRelation observeRelation) {
		this.observeRelation = observeRelation;
	}
	
	private static final class KeyMID {

		protected final int MID;
		protected final byte[] address;
		protected final int port;

		public KeyMID(int mid, byte[] address, int port) {
			if (address == null)
				throw new NullPointerException();
			this.MID = mid;
			this.address = address;
			this.port = port;
		}
		
		@Override
		public int hashCode() {
			return (port*31 + MID) * 31 + Arrays.hashCode(address);
		}
		
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof KeyMID))
				return false;
			KeyMID key = (KeyMID) o;
			return MID == key.MID && port == key.port && Arrays.equals(address, key.address);
		}
	}
}

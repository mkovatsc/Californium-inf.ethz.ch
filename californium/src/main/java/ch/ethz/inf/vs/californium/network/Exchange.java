package ch.ethz.inf.vs.californium.network;

import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import ch.ethz.inf.vs.californium.Utils;
import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.layer.BlockwiseLayer;
import ch.ethz.inf.vs.californium.network.layer.BlockwiseStatus;
import ch.ethz.inf.vs.californium.observe.ObserveRelation;

/**
 * An exchange represents the complete state of an exchange of one request and
 * one or more responses. The exchange's lifecycle ends when either the last
 * response has arrived and is acknowledged, when a request or response has been
 * rejected from the remove endpoint, when the request has been canceled or when
 * a request or response hat timeouted, i.e., has reached the retransmission
 * limit without being acknowledged.
 * <p>
 * Server and client applications use the class Exchange to manage an exchange
 * of {@link Request}s and {@link Response}s. The Exchange only contains state,
 * no functionality. The CoAP Stack contains the functionality of the CoAP
 * protocol and modifies the exchange appropriately. The class Exchange and its
 * fields are <em>NOT</em> thread-safe.
 * <p>
 * The only methods a developer should ever call on this class are
 * {@link #respond(Response)} and {@link #respond(String)}.
 * <p>
 * This class might change with the development of CoAP extensions.
 */
public class Exchange {
	
	private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
	
	/**
	 * The origin of an exchange. If Cf receives a new request and creates a new
	 * exchange the origin is REMOTE since the request has been initiated from a
	 * remote endpoint. If Cf creates a new request and sends it, the origin is
	 * LOCAL.
	 */
	public enum Origin {
		LOCAL, REMOTE;
	}

	/** The endpoint that processes this exchange */
	private Endpoint endpoint;
	
	/** An observer to be called when a request is complete */
	private ExchangeObserver observer;
	
	/** Indicates if the exchange is complete */
	private boolean complete;
	
	/** The timestamp when this exchange has been created */
	private long timestamp;
	
	/**
	 * The actual request that caused this exchange. Layers below the
	 * {@link BlockwiseLayer} should only work with the {@link #currentRequest}
	 * while layers above should work with the {@link #request}.
	 */
	private Request request; // the initial request we have to exchange
	
	/**
	 * The current block of the request that is being processed. This is a single
	 * block in case of a blockwise transfer or the same as {@link #request} in
	 * case of a normal transfer.
	 */
	private Request currentRequest; // Matching needs to know for what we expect a response
	
	/** The status of the blockwise transfer. null in case of a normal transfer */
	private BlockwiseStatus requestBlockStatus;
	
	/**
	 * The actual response that is supposed to be sent to the client. Layers
	 * below the {@link BlockwiseLayer} should only work with the
	 * {@link #currentResponse} while layers above should work with the
	 * {@link #response}.
	 */
	private Response response;
	
	/** The current block of the response that is being transferred. */
	private Response currentResponse; // Matching needs to know when receiving duplicate
	
	/** The status of the blockwise transfer. null in case of a normal transfer */
	private BlockwiseStatus responseBlockStatus;
	
	// indicates where the request of this exchange has been initiated.
	// (as suggested by effective Java, item 40.)
	private final Origin origin;
	
	// true if the exchange has failed due to a timeout
	private boolean timeouted;
	
	// the timeout of the current request or response set by reliability layer
	private int currentTimeout;
	
	// the amount of attempted transmissions that have not succeeded yet
	private int failedTransmissionCount = 0;

	// handle to cancel retransmission
	private ScheduledFuture<?> retransmissionHandle;
	
	// If the request was sent with a block1 option the response has to send its
	// first block piggy-backed with the Block1 option of the last request block
	private BlockOption block1ToAck;
	
	/** The relation that the target resource has established with the source*/
	private ObserveRelation relation;

	/**
	 * Constructs a new exchange with the specified request and origin. 
	 * @param request the request that starts the exchange
	 * @param origin the origin of the request (LOCAL or REMOTE)
	 */
	public Exchange(Request request, Origin origin) {
		INSTANCE_COUNTER.incrementAndGet();
		this.currentRequest = request; // might only be the first block of the whole request
		this.origin = origin;
		this.timestamp = System.currentTimeMillis();
	}
	
	/**
	 * Accept this exchange and therefore the request. Only if the request's
	 * type was a <code>CON</code> and the request has not been acknowledged
	 * yet, it sends an ACK to the client.
	 */
	public void accept() {
		assert(origin == Origin.REMOTE);
		if (request.getType() == Type.CON && !request.isAcknowledged()) {
			request.setAcknowledged(true);
			EmptyMessage ack = EmptyMessage.newACK(request);
			endpoint.sendEmptyMessage(this, ack);
		}
	}
	
	/**
	 * Reject this exchange and therefore the request. Sends an RST back to the
	 * client.
	 */
	public void reject() {
		assert(origin == Origin.REMOTE);
		request.setRejected(true);
		EmptyMessage rst = EmptyMessage.newRST(request);
		endpoint.sendEmptyMessage(this, rst);
	}
	
	/**
	 * Sends the specified content in a {@link Response} with code.
	 * {@link ResponseCode#CONTENT} over the same endpoint as the request has
	 * arrived.
	 * 
	 * @param content the content
	 */
	public void respond(String content) {
		Response response = new Response(ResponseCode.CONTENT);
		response.setPayload(content.getBytes());
		response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
		respond(response);
	}
	
	public void respond(ResponseCode code) {
		respond(new Response(code));
	}
	
	public void respond(ResponseCode code, String content) {
		Response response = new Response(code);
		response.setPayload(content);
		response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
		respond(response);
	}
	
	/**
	 * Sends the specified response over the same endpoint as the request has
	 * arrived.
	 * 
	 * @param response the response
	 */
	public void respond(Response response) {
		response.setDestination(request.getSource());
		response.setDestinationPort(request.getSourcePort());
		this.response = response;
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

	public void setTimeouted() {
		this.timeouted = true;
	}

	public int getFailedTransmissionCount() {
		return failedTransmissionCount;
	}

	public void setFailedTransmissionCount(int failedTransmissionCount) {
		this.failedTransmissionCount = failedTransmissionCount;
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
		ExchangeObserver obs = this.observer;
		if (obs != null)
			obs.completed(this);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public ObserveRelation getRelation() {
		return relation;
	}

	public void setRelation(ObserveRelation relation) {
		this.relation = relation;
	}
	
//	// TODO: Is this still necessary and useful? Since the new blockwise 
//	// transfer allows using multiple tokens in the same exchange, an
//	// Exchange no longer can be identified by a token.
//	public byte[] getToken() {
//		if (request == null) return null;
//		else return request.getToken();
//	}
	
//	public Iterable<KeyMID> getMIDKeys() {
//		return midKeys;
//	}
	
//	public Iterable<KeyToken> getTokenKeys() {
//		return tokenKeys;
//	}
	
//	public void addMIDKey(KeyMID midKey) {
//		midKeys.add(midKey);
//	}
	
//	public void addTokenKey(KeyToken tokenKey) {
//		tokenKeys.add(tokenKey);
//	}
	
	@Override
	protected void finalize() throws Throwable {
//		System.out.println(" ============  Exchange from "+ origin+" finalized, remaining: "+INSTANCE_COUNTER.decrementAndGet());
		super.finalize();
	}
	
	public static final class KeyMID {
		
		protected final int MID;
		protected final byte[] address;
		protected final int port;
		private final int hash;
		
		public KeyMID(int mid, byte[] address, int port) {
			if (address == null)
				throw new NullPointerException();
			this.MID = mid;
			this.address = address;
			this.port = port;
			this.hash = (port*31 + MID) * 31 + Arrays.hashCode(address);
		}
		
		@Override
		public int hashCode() {
			return hash;
		}
		
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof KeyMID))
				return false;
			KeyMID key = (KeyMID) o;
			return MID == key.MID && port == key.port && Arrays.equals(address, key.address);
		}
		
		@Override
		public String toString() {
			return "KeyMID["+MID+" from "+Utils.toHexString(address)+":"+port+"]";
		}
	}
	
	public static final class KeyToken {

		protected final byte[] token;
		protected final byte[] address;
		protected final int port;
		private final int hash;

		public KeyToken(byte[] token, byte[] address, int port) {
			if (address == null)
				throw new NullPointerException();
			if (token == null)
				throw new NullPointerException();
			this.token = token;
			this.address = address;
			this.port = port;
			this.hash = (port*31 + Arrays.hashCode(token)) * 31 + Arrays.hashCode(address);
		}
		
		@Override
		public int hashCode() {
			return hash;
		}
		
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof KeyToken))
				return false;
			KeyToken key = (KeyToken) o;
			return Arrays.equals(token, key.token) && port == key.port && Arrays.equals(address, key.address);
		}
		
		@Override
		public String toString() {
			return "KeyToken["+Utils.toHexString(token)+" from "+Utils.toHexString(address)+":"+port+"]";
		}
	}
	
	public static class KeyUri {

		protected final String uri;
		protected final byte[] address;
		protected final int port;
		private final int hash;
		
		public KeyUri(String uri, byte[] address, int port) {
			if (uri == null) throw new NullPointerException();
			if (address == null) throw new NullPointerException();
			this.uri = uri;
			this.address = address;
			this.port = port;
			this.hash = (port*31 + uri.hashCode()) * 31 + Arrays.hashCode(address);
		}
		
		@Override
		public int hashCode() {
			return hash;
		}
		
		@Override
		public boolean equals(Object o) {
			if (! (o instanceof KeyUri))
				return false;
			KeyUri key = (KeyUri) o;
			return uri.equals(key.uri) && port == key.port && Arrays.equals(address, key.address);
		}
		
		@Override
		public String toString() {
			return "KeyUri["+uri+" from "+Utils.toHexString(address)+":"+port+"]";
		}
	}
}

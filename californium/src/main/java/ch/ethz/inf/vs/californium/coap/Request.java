/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.coap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The Class Request describes the functionality of a CoAP Request as a subclass
 * of a CoAP {@link Message}. It provides operations to answer a request by a {@link Response}
 * using {@link #respond(Response)}. There are different ways to handle incoming
 * responses:
 * <ol>
 * <li>by overriding the protected method {@link #handleResponse(Response)}, e.g.,
 * using anonymous inner classes
 * <li>by registering a handler using {@link #registerResponseHandler(ResponseHandler)}
 * <li>by calling the blocking method {@link #receiveResponse()}
 * </ol>
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class Request extends Message {
	
// Constants ///////////////////////////////////////////////////////////////////

	/** The Constant TIMEOUT_RESPONSE. */
	// TODO better solution?
	private static final Response TIMEOUT_RESPONSE = new Response();
	
	/** The time when a request was issued to calculate Observe counter. */
	public final long startTime = System.currentTimeMillis();

// Members /////////////////////////////////////////////////////////////////////

	/** The list of response handlers that are notified about incoming responses. */
	private List<ResponseHandler> responseHandlers;
	
	/** The response queue filled by {@link #receiveResponse()}. */
	private BlockingQueue<Response> responseQueue;
	
	private Response currentResponse = null;
	
	/** The number of responses to this request. */
	private int responseCount;

// Constructors ////////////////////////////////////////////////////////////////

	/**
	 * Instantiates a new request.
	 */
	public Request(int method) {
		super();
		this.setCode(method);
	}

	/**
	 * Instantiates a new request.
	 *
	 * @param method The method code of the message
	 * @param confirmable True if the request is to be sent as a confirmable
	 */
	public Request(int method, boolean confirmable) {
		super(confirmable ? messageType.CON : messageType.NON, method);
	}

// Methods /////////////////////////////////////////////////////////////////////

	/**
	 * Executes the request on the endpoint specified by the message's URI
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void execute() throws IOException {

		this.send();
		
		// TODO: LocalEndPoint stubs?
	}

	/**
	 * Overrides {@link Message#accept()} to keep track of the response count,
	 * which is required to manage MIDs for exchanges over multiple
	 * transactions.
	 */
	@Override
	public void accept() {
		++this.responseCount;
		super.accept();
	}

	public Response getResponse() {
		return this.currentResponse;
	}
	
	public void setResponse(Response response) {
		this.currentResponse = response;
	}
	
	/**
	 * Issues a new response to this request
	 *
	 * @param response The response buddy for this request
	 */
	public void respond(Response response) {

		// assign response to this request
		response.setRequest(this);

		response.setPeerAddress( getPeerAddress() );

		// set matching MID for replies
		if (responseCount == 0 && isConfirmable()) {
			response.setMID(getMID());
		}

		// set matching type
		if (response.getType() == null) {
			if (responseCount == 0 && isConfirmable()) {
				// use piggy-backed response
				response.setType(messageType.ACK);
			} else {
				// use separate response:
				// Confirmable response to confirmable request,
				// Non-confirmable response to non-confirmable request
				response.setType(getType());
			}
		}

		
		if (response.getCode()!=CodeRegistry.EMPTY_MESSAGE) {
			
			// Reflect token
			response.setToken(this.getToken());
			
			// echo block1 option
			BlockOption block1 = (BlockOption) this.getFirstOption( OptionNumberRegistry.BLOCK1 );
			if (block1!=null) {
				// TODO: block1.setM(false); maybe in TransferLayer
				response.addOption(block1);
			}
		} else {
			LOG.severe("FIXME: Called with EMPTY MESSAGE");	// FIXME Unsure about execution path, check
		}
		
		++this.responseCount;
		
		// Endpoint will call sendResponse();
		setResponse(response);
	}

	/**
	 * Respond this request.
	 *
	 * @param code the status code
	 * @param message a string message
	 * @param contentType the Content-Type of the response
	 */
	public void respond(int code, String message, int contentType) {
		Response response = new Response(code);
		if (message != null) {
			response.setPayload(message);
			response.setContentType(contentType);
			
			LOG.finest(String.format("Responding with Content-Type %d: %d bytes", contentType, message.length()));
		}
		respond(response);
	}

	/**
	 * Respond this request.
	 *
	 * @param code the status code
	 * @param message a string message
	 */
	public void respond(int code, String message) {
		Response response = new Response(code);
		if (message != null) {
			response.setPayload(message);
		}
		respond(response);
	}

	/**
	 * Respond this request.
	 *
	 * @param code the status code
	 */
	public void respond(int code) {
		respond(code, null);
	}
	
	public void sendResponse() {
		if (currentResponse!=null) {
			if (this.getPeerAddress() != null) {
				currentResponse.send();
			} else {
				// handle locally
				handleResponse(currentResponse);
			}
		} else {
			LOG.warning(String.format("Missing response to send: Request %s for %s", key(), getUriPath()));
		}
	}

	/**
	 * Returns a response that was placed using {@link #respond()} and blocks
	 * until such a response is available.
	 * 
	 * NOTE: In order to safely use this method, the call
	 * {@link #enableResponseQueue(true)} is required BEFORE any possible
	 * {@link #respond()} calls take place.
	 *
	 * @return the next response in the queue
	 * @throws InterruptedException the interrupted exception
	 */
	public Response receiveResponse() throws InterruptedException {

		// response queue required to perform this operation
		if (!responseQueueEnabled()) {
			LOG.warning("Missing enableResponseQueue(true) call, responses may be lost");
			enableResponseQueue(true);
		}

		// take response from queue
		Response response = responseQueue.take();

		// return null if request timed out
		return response != TIMEOUT_RESPONSE ? response : null;
	}

	/**
	 * Registers a handler for responses to this request.
	 *
	 * @param handler the handler to be added
	 */
	public void registerResponseHandler(ResponseHandler handler) {

		if (handler != null) {

			// lazy creation of response handler list
			if (responseHandlers == null) {
				responseHandlers = new ArrayList<ResponseHandler>();
			}

			responseHandlers.add(handler);
		}
	}

	/**
	 * Unregister response handler.
	 *
	 * @param handler the handler to be removed
	 */
	public void unregisterResponseHandler(ResponseHandler handler) {

		if (handler != null && responseHandlers != null) {

			responseHandlers.remove(handler);
		}
	}

	/**
	 * Enables or disables the response queue
	 * 
	 * NOTE: The response queue needs to be enabled BEFORE any possible calls to
	 * {@link #receiveResponse()}.
	 *
	 * @param enable true to enable, false to disable
	 */
	public void enableResponseQueue(boolean enable) {
		if (enable != responseQueueEnabled()) {
			responseQueue = enable ? new LinkedBlockingQueue<Response>() : null;
		}
	}

	/**
	 * Checks if the response queue is enabled.
	 *
	 * @return true, if response queue is enabled
	 */
	public boolean responseQueueEnabled() {
		return responseQueue != null;
	}

// Subclassing /////////////////////////////////////////////////////////////////

	/**
	 * This method is called whenever a response was placed to this request.
	 * Subclasses can override this method in order to handle responses.
	 *
	 * @param response the response
	 */
	protected void handleResponse(Response response) {

		// enqueue response
		if (responseQueueEnabled()) {
			if (!responseQueue.offer(response)) {
				System.out.println("ERROR: Failed to enqueue response to request");
			}
		}

		// notify response handlers
		if (responseHandlers != null) {
			for (ResponseHandler handler : responseHandlers) {
				handler.handleResponse(response);
			}
		}

	}

	/**
	 * Response payload appended.
	 *
	 * @param response the response
	 * @param block the block
	 */
	protected void responsePayloadAppended(Response response, byte[] block) {
		// do nothing
	}

	/**
	 * Response completed.
	 *
	 * @param response the response
	 */
	protected void responseCompleted(Response response) {
		// do nothing
	}

	/**
	 * Direct subclasses need to override this method in order to invoke the
	 * according method of the provided RequestHandler (visitor pattern)
	 * 
	 * @param handler A handler for this request
	 */
	public void dispatch(RequestHandler handler) {
		LOG.info(String.format("Cannot dispatch: %s", CodeRegistry.toString(getCode())));
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.coap.Message#handleBy(ch.ethz.inf.vs.californium.coap.MessageHandler)
	 */
	@Override
	public void handleBy(MessageHandler handler) {
		handler.handleRequest(this);
	}

	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.coap.Message#handleTimeout()
	 */
	@Override
	public void handleTimeout() {
		if (responseQueueEnabled()) {
			responseQueue.offer(TIMEOUT_RESPONSE);
		}
	}
}

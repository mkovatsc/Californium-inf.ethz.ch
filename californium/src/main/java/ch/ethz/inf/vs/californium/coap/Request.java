/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
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

import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;

/**
 * The Class Request describes the functionality of a CoAP Request as a subclass
 * of a CoAP {@link Message}. It provides operations to answer a request by a
 * {@link Response} using {@link #respond(Response)}. There are different ways
 * to handle incoming responses:
 * <ol>
 * <li>by overriding the protected method {@link #handleResponse(Response)},
 * e.g., using anonymous inner classes
 * <li>by registering a handler using
 * {@link #registerResponseHandler(ResponseHandler)}
 * <li>by calling the blocking method {@link #receiveResponse()}
 * </ol>
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, Francesco Corazza and Matthias
 *         Kovatsch
 */
public class Request extends Message {

	// Constants ///////////////////////////////////////////////////////////////////

	/** The Constant TIMEOUT_RESPONSE. */
	// TODO better solution?
	private static final Response TIMEOUT_RESPONSE = new Response();

	/** The time when a request was issued to calculate Observe counter. */
	public final long startTime = System.currentTimeMillis(); // FIXME public
																// field!

	// Members /////////////////////////////////////////////////////////////////////

	/**
	 * The list of response handlers that are notified about incoming responses.
	 */
	private List<ResponseHandler> responseHandlers;

	/** The response queue filled by {@link #receiveResponse()}. */
	private BlockingQueue<Response> responseQueue;

	private LocalResource resource = null;
	
	private Response currentResponse = null;

	/** The number of responses to this request. */
	private int responseCount;
	
	private boolean isObserving = false;

	// Constructors ////////////////////////////////////////////////////////////////

	public static Request getRequestForMethod(int coapMethod) {
		switch (coapMethod) {
		case CodeRegistry.METHOD_GET:
			return new GETRequest();
		case CodeRegistry.METHOD_POST:
			return new POSTRequest();
		case CodeRegistry.METHOD_PUT:
			return new PUTRequest();
		case CodeRegistry.METHOD_DELETE:
			return new DELETERequest();
		default:
			LOG.warning("The value is not a valid coap request identifier");
			throw new IllegalArgumentException("The value is not a valid coap request identifier");
		}
	}

	// Methods /////////////////////////////////////////////////////////////////////

	/**
	 * Instantiates a new request.
	 *
	 * @param method The method code of the message
	 * @param confirmable True if the request is to be sent as a confirmable
	 */
	public Request(int method) {
		super();
		this.setCode(method);
	}

	/**
	 * Instantiates a new request.
	 *
	 * @param method
	 *            The method code of the message
	 * @param confirmable
	 *            True if the request is to be sent as a confirmable
	 */
	public Request(int method, boolean confirmable) {
		super(confirmable ? messageType.CON : messageType.NON, method);
	}
	
	
	public void setResource(LocalResource resouce) {
		this.resource = resouce;
	}

	/**
	 * Overrides {@link Message#accept()} to keep track of the response count,
	 * which is required to manage MIDs for exchanges over multiple
	 * transactions.
	 */
	@Override
	public void accept() {
		++responseCount;
		super.accept();
	}

	/**
	 * Direct subclasses need to override this method in order to invoke the
	 * according method of the provided RequestHandler (visitor pattern)
	 * 
	 * @param handler
	 *            A handler for this request
	 */
	public void dispatch(RequestHandler handler) {
		LOG.info(String.format("Cannot dispatch: %s", CodeRegistry.toString(getCode())));
	}

	/**
	 * Enables or disables the response queue
	 * 
	 * NOTE: The response queue needs to be enabled BEFORE any possible calls to
	 * {@link #receiveResponse()}.
	 * 
	 * @param enable
	 *            true to enable, false to disable
	 */
	public void enableResponseQueue(boolean enable) {
		if (enable != isResponseQueueEnabled()) {
			this.responseQueue = enable ? new LinkedBlockingQueue<Response>() : null;
		}
	}

	/**
	 * Executes the request on the endpoint specified by the message's URI
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void execute() throws IOException {

		// TODO reset response, requires new MID
		
		send();

		// TODO: LocalEndPoint stubs?
	}

	public Response getResponse() {
		return currentResponse;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * ch.ethz.inf.vs.californium.coap.Message#handleBy(ch.ethz.inf.vs.californium
	 * .coap.MessageHandler)
	 */
	@Override
	public void handleBy(MessageHandler handler) {
		handler.handleRequest(this);
	}

	/*
	 * (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.coap.Message#handleTimeout()
	 */
	@Override
	public void handleTimeout() {
		if (isResponseQueueEnabled()) {
			this.responseQueue.offer(TIMEOUT_RESPONSE);
		}
	}

	/**
	 * Checks if the response queue is enabled.
	 * 
	 * @return true, if response queue is enabled
	 */
	public boolean isResponseQueueEnabled() {
		return this.responseQueue != null;
	}
	
	public void sendResponse() {
		if (currentResponse!=null) {
			if (!this.isObserving) {
		
				// check if resource is to be observed
				if (this.resource!=null && resource.isObservable() && this instanceof GETRequest &&
						CodeRegistry.responseClass(this.getResponse().getCode())==CodeRegistry.CLASS_SUCCESS) {
					
					if (this.hasOption(OptionNumberRegistry.OBSERVE)) {
						
						// establish new observation relationship
						ObservingManager.getInstance().addObserver((GETRequest) this, this.resource);
	
					} else if (ObservingManager.getInstance().isObserved(this.getPeerAddress().toString(), this.resource)) {
	
						// terminate observation relationship on that resource
						ObservingManager.getInstance().removeObserver(this.getPeerAddress().toString(), this.resource);
					}
					
				}
				
				if (this.getPeerAddress() != null) {
					currentResponse.send();
				} else {
					// handle locally
					handleResponse(currentResponse);
				}
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
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	public Response receiveResponse() throws InterruptedException {

		// response queue required to perform this operation
		if (!isResponseQueueEnabled()) {
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
	 * @param handler
	 *            the handler to be added
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
	 * Respond this request.
	 * 
	 * @param code
	 *            the status code
	 */
	public void respond(int code) {
		respond(code, null);
	}

	/**
	 * Respond this request.
	 * 
	 * @param code
	 *            the status code
	 * @param message
	 *            a string message
	 */
	public void respond(int code, String message) {
		respond(code, message, MediaTypeRegistry.UNDEFINED);
	}

	/**
	 * Respond this request.
	 * 
	 * @param code
	 *            the status code
	 * @param message
	 *            a string message
	 * @param contentType
	 *            the Content-Type of the response
	 */
	public void respond(int code, String message, int contentType) {
		Response response = new Response(code);
		if (message != null) {
			
			response.setPayload(message);
			
			if (contentType!=MediaTypeRegistry.UNDEFINED) {
				response.setContentType(contentType);
				LOG.finest(String.format("Responding with Content-Type %d: %d bytes", contentType, message.length()));
			} else if (CodeRegistry.isSuccess(code)) {
				response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
				LOG.finest(String.format("Responding with implicit text/plain: %d bytes", contentType, message.length()));
			}

		}
		respond(response);
	}

	/**
	 * Issues a new response to this request
	 * 
	 * @param response
	 *            The response buddy for this request
	 */
	public void respond(Response response) {

		// assign response to this request
		response.setRequest(this);

		response.setPeerAddress(getPeerAddress());

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

		if (response.getCode() != CodeRegistry.EMPTY_MESSAGE) {

			// Reflect token
			response.setToken(getToken());

			// echo block1 option
			BlockOption block1 = (BlockOption) getFirstOption(OptionNumberRegistry.BLOCK1);
			if (block1 != null) {
				// TODO: block1.setM(false); maybe in TransferLayer
				response.addOption(block1);
			}
		} else {
			LOG.severe("FIXME: Called with EMPTY MESSAGE"); // FIXME valid execution path? check
		}

		++responseCount;

		// Endpoint will call sendResponse();
		setResponse(response);
	}

	public void respondAndSend(int code) {
		respond(code);
		sendResponse();
	}

	public void respondAndSend(Response response) {
		respond(response);
		sendResponse();
	}

	public void setResponse(Response response) {
		
		// check for valid CoAP message
		if (response.payloadSize()>0) {
			if (CodeRegistry.isSuccess(response.getCode())) {
				if (response.getCode()==CodeRegistry.RESP_VALID || response.getCode()==CodeRegistry.RESP_DELETED) {
					LOG.warning(String.format("Removing payload of %s response: %s", CodeRegistry.toString(response.getCode()), response.key()));
					response.setPayload("");
					response.setContentType(MediaTypeRegistry.UNDEFINED);
				}
			} else if (response.getContentType()!=MediaTypeRegistry.UNDEFINED) {
				LOG.warning(String.format("Removing Content-Format for %s response: %s", CodeRegistry.toString(response.getCode()), response.key()));
				response.setContentType(MediaTypeRegistry.UNDEFINED);
			}
		}
		
		currentResponse = response;
	}

	/**
	 * Unregister response handler.
	 * 
	 * @param handler
	 *            the handler to be removed
	 */
	public void unregisterResponseHandler(ResponseHandler handler) {

		if (handler != null && this.responseHandlers != null) {

			this.responseHandlers.remove(handler);
		}
	}

	/**
	 * This method is called whenever a response was placed to this request.
	 * Subclasses can override this method in order to handle responses.
	 * 
	 * @param response
	 *            the response
	 */
	protected void handleResponse(Response response) {

		// enqueue response
		if (isResponseQueueEnabled()) {
			if (!this.responseQueue.offer(response)) {
				LOG.severe("ERROR: Failed to enqueue response to request");
			}
		}

		// notify response handlers
		if (this.responseHandlers != null) {
			for (ResponseHandler handler : this.responseHandlers) {
				handler.handleResponse(response);
			}
		}

	}

	/**
	 * Response completed.
	 * 
	 * @param response
	 *            the response
	 */
	protected void responseCompleted(Response response) {
		// do nothing
	}

	/**
	 * Response payload appended.
	 * 
	 * @param response
	 *            the response
	 * @param block
	 *            the block
	 */
	protected void responsePayloadAppended(Response response, byte[] block) {
		// do nothing
	}
	
	public void setObserving(boolean isObserving) {
		this.isObserving = isObserving;
	}
}

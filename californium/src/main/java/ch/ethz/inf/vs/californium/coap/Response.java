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

/**
 * The Class Response describes the functionality of a CoAP Response as
 * a subclass of a CoAP {@link Message}. It is usually linked to a {@link Request} and
 * supports the handling of Request/Response pairs.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class Response extends Message {

// Constructors ////////////////////////////////////////////////////////////////

	// TODO get rid off
	public Response() {
		this(CodeRegistry.RESP_VALID);
	}

	/**
	 * Instantiates a new response.
	 *
	 * @param method the status code of the message
	 */
	public Response(int status) {
		setCode(status);
	}

// Methods /////////////////////////////////////////////////////////////////////
	
	public void setRequest(Request request) {
		this.request = request;
	}

	public Request getRequest() {
		return request;
	}

	/**
	 * Returns the round trip time in milliseconds (nano precision).
	 * @return RTT in ms
	 */
	public double getRTT() {
		if (request != null) {
			return (double)(getTimestamp() - request.getTimestamp())/1000000d;
		} else {
			return -1d;
		}
	}

	@Override
	protected void payloadAppended(byte[] block) {
		if (request != null) {
			request.responsePayloadAppended(this, block);
		}
	}

	@Override
	public void handleBy(MessageHandler handler) {
		handler.handleResponse(this);
	}

	public boolean isPiggyBacked() {
		return isAcknowledgement() && getCode() != CodeRegistry.EMPTY_MESSAGE;
	}

	private Request request;
}

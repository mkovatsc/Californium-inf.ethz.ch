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

package ch.ethz.inf.vs.californium.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.Message.messageType;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

/**
 * Static class that provides the translations between the messages from the
 * internal CoAP nodes and external ones.
 * 
 * @author Francesco Corazza
 */
public final class CoapTranslator {

	private static final String PROPERTIES_FILENAME = "Proxy.properties";

	public static final Properties TRANSLATION_PROPERTIES = new Properties(PROPERTIES_FILENAME);

	/** The Constant LOG. */
	protected static final Logger LOG = Logger.getLogger(CoapTranslator.class.getName());

	/**
	 * Starting from an external CoAP request, the method fills a new request
	 * for the internal CaAP nodes. Translates the proxy-uri option in the uri
	 * of the new request and simply copies the options and the payload from the
	 * original request to the new one.
	 * 
	 * @param incomingRequest
	 *            the original request
	 * @param outgoingRequest
	 *            the new request
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 */
	public static void fillRequest(final Request incomingRequest, final Request outgoingRequest) throws URISyntaxException {
		// check parameters
		if (incomingRequest == null) {
			throw new IllegalArgumentException("incomingRequest == null");
		}
		if (outgoingRequest == null) {
			throw new IllegalArgumentException("outgoingRequest == null");
		}

		// copy the code
		int code = incomingRequest.getCode();
		outgoingRequest.setCode(code);

		// copy message type
		messageType messageType = incomingRequest.getType();
		outgoingRequest.setType(messageType);

		// copy payload
		byte[] payload = incomingRequest.getPayload();
		outgoingRequest.setPayload(payload);

		// get the uri address from the proxy-uri option
		URI serverUri = incomingRequest.getProxyUri();
		// set the proxy-uri as the outgoing uri
		if (serverUri != null) {
			outgoingRequest.setURI(serverUri);
		}

		// copy every option from the original message
		// not to copy the proxy-uri option because it is not necessary in the
		// new message
		// not to copy the uri-* options because they are already filled in the
		// new message
		for (Option option : incomingRequest.getOptions()) {
			int optionNumber = option.getOptionNumber();
			if (optionNumber != OptionNumberRegistry.PROXY_URI && !OptionNumberRegistry.isUriOption(optionNumber)) {
				outgoingRequest.setOption(option);
			}
		}
	}

	/**
	 * Fills the new response with the response received from the internal CoAP
	 * node. Simply copies the options and the payload from the forwarded
	 * response to the new one.
	 * 
	 * @param incomingResponse
	 *            the forwarded request
	 * @param outgoingResponse
	 *            the original response
	 * @return the response
	 */
	public static void fillResponse(final Response incomingResponse, final Response outgoingResponse) {
		if (incomingResponse == null) {
			throw new IllegalArgumentException("incomingResponse == null");
		}
		if (outgoingResponse == null) {
			throw new IllegalArgumentException("outgoingResponse == null");
		}

		// copy the code from the message
		int code = incomingResponse.getCode();
		outgoingResponse.setCode(code);

		// copy the type
		messageType messageType = incomingResponse.getType();
		outgoingResponse.setType(messageType);

		// copy payload
		byte[] payload = incomingResponse.getPayload();
		outgoingResponse.setPayload(payload);

		// copy every option
		for (Option option : incomingResponse.getOptions()) {
			outgoingResponse.setOption(option);
		}
	}

	private CoapTranslator() {
	}
}

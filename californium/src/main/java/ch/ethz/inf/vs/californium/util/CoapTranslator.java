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
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

/**
 * Static class that provides the translations between the messages from the
 * internal CoAP nodes and external ones.
 * 
 * @author Francesco Corazza
 * @version $Revision: 1.0 $
 */
public final class CoapTranslator {

	/** The Constant LOG. */
	protected static final Logger LOG = Logger.getLogger(CoapTranslator.class.getName());

	/**
	 * Property file containing the mappings between coap messages and http
	 * messages.
	 */
	public static final Properties COAP_TRANSLATION_PROPERTIES = new Properties("Proxy.properties");

	// Error constants
	public static final int STATUS_FIELD_MALFORMED = CodeRegistry.RESP_BAD_OPTION;
	public static final int STATUS_TIMEOUT = CodeRegistry.RESP_GATEWAY_TIMEOUT;
	public static final int STATUS_TRANSLATION_ERROR = CodeRegistry.RESP_BAD_GATEWAY;

	/**
	 * Starting from an external CoAP request, the method fills a new request
	 * for the internal CaAP nodes. Translates the proxy-uri option in the uri
	 * of the new request and simply copies the options and the payload from the
	 * original request to the new one.
	 * 
	 * @param incomingRequest
	 *            the original request
	 * 
	 * 
	 * 
	 * @return Request
	 * @throws TranslationException
	 *             the translation exception
	 */
	public static Request getRequest(final Request incomingRequest) throws TranslationException {
		// check parameters
		if (incomingRequest == null) {
			throw new IllegalArgumentException("incomingRequest == null");
		}

		// get the code
		int code = incomingRequest.getCode();

		// get message type
		messageType type = incomingRequest.getType();

		// create the request
		Request outgoingRequest = new Request(code, type == messageType.CON);

		// copy payload
		byte[] payload = incomingRequest.getPayload();
		outgoingRequest.setPayload(payload);

		// get the uri address from the proxy-uri option
		URI serverUri;
		try {
			serverUri = incomingRequest.getProxyUri();
		} catch (URISyntaxException e) {
			LOG.warning("Cannot translate the server uri" + e);
			throw new TranslationException("Cannot translate the server uri", e);
		}

		// set the proxy-uri as the outgoing uri
		if (serverUri != null) {
			outgoingRequest.setURI(serverUri);
		}

		// copy every option from the original message
		for (Option option : incomingRequest.getOptions()) {
			int optionNumber = option.getOptionNumber();

			// do not copy the proxy-uri option because it is not necessary in
			// the new message
			// do not copy the token option because it is a local option and
			// have to be assigned by the proper layer
			// do not copy the block* option because it is a local option and
			// have to be assigned by the proper layer
			// do not copy the uri-* options because they are already filled in
			// the new message
			if (optionNumber != OptionNumberRegistry.PROXY_URI && !OptionNumberRegistry.isUriOption(optionNumber) && option.getOptionNumber() != OptionNumberRegistry.BLOCK1 && option.getOptionNumber() != OptionNumberRegistry.BLOCK2) {
				outgoingRequest.setOption(option);
			}
		}

		LOG.finer("Incoming request translated correctly");
		return outgoingRequest;
	}

	/**
	 * Fills the new response with the response received from the internal CoAP
	 * node. Simply copies the options and the payload from the forwarded
	 * response to the new one.
	 * 
	 * @param incomingResponse
	 *            the forwarded request
	 * 
	 * 
	 * @return the response
	 */
	public static Response getResponse(final Response incomingResponse) {
		if (incomingResponse == null) {
			throw new IllegalArgumentException("incomingResponse == null");
		}

		// get the status
		int status = incomingResponse.getCode();

		// create the response
		Response outgoingResponse = new Response(status);

		// copy payload
		byte[] payload = incomingResponse.getPayload();
		outgoingResponse.setPayload(payload);

		// copy the timestamp
		long timestamp = incomingResponse.getTimestamp();
		outgoingResponse.setTimestamp(timestamp);

		// copy every option
		for (Option option : incomingResponse.getOptions()) {
			int optionNumber = option.getOptionNumber();

			// do not copy the token option because it is a local option and
			// have to be assigned by the proper layer
			// do not copy the block* option because it is a local option and
			// have to be assigned by the proper layer
			if (optionNumber != OptionNumberRegistry.BLOCK1 && optionNumber != OptionNumberRegistry.BLOCK2) {
				outgoingResponse.addOption(option);
			}
		}

		LOG.finer("Incoming response translated correctly");
		return outgoingResponse;
	}

	/**
	 * The Constructor is private because the class is an helper class and
	 * cannot be instantiated.
	 */
	private CoapTranslator() {
	}
}

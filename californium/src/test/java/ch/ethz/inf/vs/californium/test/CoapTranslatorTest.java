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

package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Message.messageType;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.CoapTranslator;
import ch.ethz.inf.vs.californium.util.TranslationException;

/**
 * Class to test the correctness of
 * {@link ch.ethz.inf.vs.californium.util.CoapTranslator}.
 * 
 * Both the translation of the request and of the response are tested. For each
 * request test case, it has been tested against CON and NON message types and
 * all the REST methods. For each response test case, it has been tested against
 * every message type and all status codes.
 * 
 * @author Francesco Corazza
 */
public class CoapTranslatorTest {

	private static final int COAP_DEFAULT_PORT = 5683;
	private static final int[] METHODS = { CodeRegistry.METHOD_GET, CodeRegistry.METHOD_DELETE, CodeRegistry.METHOD_POST, CodeRegistry.METHOD_PUT };
	private static final int[] STATUS_CODES = { CodeRegistry.RESP_CREATED, CodeRegistry.RESP_DELETED, CodeRegistry.RESP_VALID, CodeRegistry.RESP_CHANGED, CodeRegistry.RESP_CONTENT, CodeRegistry.RESP_BAD_REQUEST, CodeRegistry.RESP_UNAUTHORIZED, CodeRegistry.RESP_BAD_OPTION, CodeRegistry.RESP_FORBIDDEN, CodeRegistry.RESP_NOT_FOUND, CodeRegistry.RESP_METHOD_NOT_ALLOWED, CodeRegistry.RESP_NOT_ACCEPTABLE, CodeRegistry.RESP_PRECONDITION_FAILED, CodeRegistry.RESP_REQUEST_ENTITY_TOO_LARGE, CodeRegistry.RESP_UNSUPPORTED_MEDIA_TYPE, CodeRegistry.RESP_INTERNAL_SERVER_ERROR, CodeRegistry.RESP_NOT_IMPLEMENTED, CodeRegistry.RESP_BAD_GATEWAY, CodeRegistry.RESP_SERVICE_UNAVAILABLE, CodeRegistry.RESP_GATEWAY_TIMEOUT, CodeRegistry.RESP_PROXYING_NOT_SUPPORTED, CodeRegistry.RESP_REQUEST_ENTITY_INCOMPLETE };

	@Test
	public void requestEmptyTest() throws TranslationException, URISyntaxException {
		requestRestMethodsTest(null, "coap://localhost:5684/resource", null);
	}

	@Test
	public void requestIPv4Test() throws TranslationException, URISyntaxException {
		requestRestMethodsTest(null, "coap://192.168.1.1:5684/resource", null);
	}

	@Test
	public void requestIPv6Test() throws TranslationException, URISyntaxException {
		requestRestMethodsTest(null, "coap://[2001:620:8:101f:250:c2ff:ff18:8d32]:5684/resource", null);
	}

	@Test
	public void requestNoPortTest() throws TranslationException, URISyntaxException {
		requestRestMethodsTest(null, "coap://localhost/resource", null);
	}

	@Test
	public void requestNormalTest() throws TranslationException, URISyntaxException, UnsupportedEncodingException {
		List<Option> options = new LinkedList<Option>();
		options.add(new Option("text/plain", OptionNumberRegistry.ACCEPT));

		requestRestMethodsTest("AAA".getBytes("UTF-8"), "coap://localhost:5684/resource", options);
	}

	@Test(expected = IllegalArgumentException.class)
	public void requestNullTest() throws TranslationException {
		CoapTranslator.getRequest(null);
	}

	@Test
	public void requestOptionTest() throws TranslationException, URISyntaxException {
		List<Option> options = new LinkedList<Option>();
		options.add(new Option("text/plain", OptionNumberRegistry.ACCEPT));

		// the following options should not be considered during the translation
		// because they are local in respect of the incoming request (the
		// outgoing request should not keep track of these options)
		// options.add(new Option("true", OptionNumberRegistry.OBSERVE));
		options.add(new Option("123", OptionNumberRegistry.BLOCK1));
		options.add(new Option("456", OptionNumberRegistry.BLOCK2));

		requestRestMethodsTest(null, "coap://localhost:5684/resource", options);
	}

	@Test
	public void requestPayloadTest() throws UnsupportedEncodingException, TranslationException, URISyntaxException {
		requestRestMethodsTest("AAA".getBytes("UTF-8"), "coap://localhost:5684/resource", null);
	}

	@Test
	public void requestQueryTest() throws TranslationException, URISyntaxException {
		requestRestMethodsTest(null, "coap://localhost:5684/resource?a=1&b=2&c=3", null);
	}

	@Test
	public void requestSubResourceTest() throws TranslationException, URISyntaxException {
		requestRestMethodsTest(null, "coap://localhost:5684/resource/sub1/sub2", null);
	}

	@Test
	public void responseEmptyTest() throws TranslationException {
		responseStatusesTest(null, null);
	}

	@Test
	public void responseInvalidOptionsTest() throws TranslationException {
		// TODO
	}

	@Test
	public void responseNormalTest() throws TranslationException, UnsupportedEncodingException {
		List<Option> options = new LinkedList<Option>();
		options.add(new Option("text/plain", OptionNumberRegistry.ACCEPT));

		responseStatusesTest("AAA".getBytes("UTF-8"), options);
	}

	@Test(expected = IllegalArgumentException.class)
	public void responseNullTest() throws TranslationException {
		CoapTranslator.getResponse(null);
	}

	@Test
	public void responseOptionTest() throws TranslationException {
		List<Option> options = new LinkedList<Option>();
		options.add(new Option("text/plain", OptionNumberRegistry.CONTENT_TYPE));

		// the following options should not be considered during the translation
		// because they are local in respect of the incoming response (the
		// outgoing request should not keep track of these options)
		// options.add(new Option("true", OptionNumberRegistry.OBSERVE));
		options.add(new Option("123", OptionNumberRegistry.BLOCK1));
		options.add(new Option("456", OptionNumberRegistry.BLOCK2));

		responseStatusesTest(null, options);
	}

	@Test
	public void responsePayloadTest() throws UnsupportedEncodingException, TranslationException {
		responseStatusesTest("AAA".getBytes("UTF-8"), null);
	}

	/**
	 * @param incomingMessage
	 * @param testedMessage
	 */
	private void messageTest(Message incomingMessage, Message testedMessage) {
		// check if the code/status is equal
		assertTrue(testedMessage.getCode() == incomingMessage.getCode());

		// check the type
		// it is not important to check the type because it will be set when the
		// response will be sent
		// assertTrue(testedMessage.getType() == incomingMessage.getType());

		// check the payload
		byte[] testedPayload = testedMessage.getPayload();
		if (testedPayload != null && testedPayload.length != 0) {
			assertTrue(testedPayload.equals(incomingMessage.getPayload()));
		}

		// check the options set in the translated request
		for (Option option : testedMessage.getOptions()) {
			int optionNumber = option.getOptionNumber();
			// the uri-* options should not be compared because they are
			// different before and after the translation
			if (!OptionNumberRegistry.isUriOption(optionNumber)) {
				// TODO check the entire set of options
				Option testedOption = incomingMessage.getFirstOption(optionNumber);
				assertTrue(option.getRawValue() == testedOption.getRawValue());
			}
		}
	}

	private void requestRestMethodsTest(byte[] payload, String proxyUri, List<Option> options) throws TranslationException, URISyntaxException {
		// test each REST method
		for (int method : METHODS) {
			// test both types of requests: confirmable and non-confirmable
			// TODO not RST and ACK?
			requestTest(method, messageType.CON, payload, proxyUri, options);
			requestTest(method, messageType.NON, payload, proxyUri, options);
		}
	}

	private void requestTest(int code, messageType type, byte[] payload, String proxyUri, List<Option> options) throws TranslationException, URISyntaxException {
		// create the test request according to the parameters
		Request incomingRequest = new Request(code, type == messageType.CON);
		incomingRequest.setPayload(payload);
		incomingRequest.setOption(new Option(proxyUri, OptionNumberRegistry.PROXY_URI));
		incomingRequest.setOptions(options);
		// set a dummy uri to emulate the proxy address
		incomingRequest.setURI(new URI("coap://localhost:666/proxy"));

		// translate the incoming request
		Request testedRequest = CoapTranslator.getRequest(incomingRequest);
		assertNotNull(testedRequest);

		// check the parameters of the message
		messageTest(incomingRequest, testedRequest);

		// check the URIs
		URI incomingRequestProxyUri = incomingRequest.getProxyUri();
		URI testedRequestCompleteUri = testedRequest.getCompleteUri();
		// check the port (default or custom)
		if (incomingRequestProxyUri.getPort() == -1) {
			// check if the absence of the port in the incoming request has been
			// translated with the default port in the translated request
			assertTrue(testedRequestCompleteUri.getPort() == COAP_DEFAULT_PORT);
		} else {
			assertTrue(incomingRequestProxyUri.equals(testedRequestCompleteUri));

			String testedRequestCompleteUriString = testedRequest.getCompleteUri().toString();
			assertTrue(proxyUri.equals(testedRequestCompleteUriString));
		}
	}

	private void responseStatusesTest(byte[] payload, List<Option> options) throws TranslationException {
		// iterate foreach status code
		for (int status : STATUS_CODES) {
			responseTest(status, messageType.CON, payload, options);
			responseTest(status, messageType.NON, payload, options);
			responseTest(status, messageType.ACK, payload, options);
			responseTest(status, messageType.RST, payload, options);
		}
	}

	private void responseTest(int status, messageType type, byte[] payload, List<Option> options) throws TranslationException {
		// create the response
		Response incomingResponse = new Response(status);

		// set type
		incomingResponse.setType(type);

		// set the payload
		incomingResponse.setPayload(payload);

		// set options
		incomingResponse.setOptions(options);

		// get the translated response
		Response testedResponse = CoapTranslator.getResponse(incomingResponse);
		assertNotNull(testedResponse);

		// check the parameters of the message
		messageTest(incomingResponse, testedResponse);
	}
}

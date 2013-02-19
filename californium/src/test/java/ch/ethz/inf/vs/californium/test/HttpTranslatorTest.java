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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.HttpTranslator;
import ch.ethz.inf.vs.californium.util.TranslationException;

/**
 * Test the of class {@link ch.ethz.inf.vs.californium.util.HttpTranslator}.
 * 
 * @author Francesco Corazza
 */
public class HttpTranslatorTest {

	private static final String[] COAP_METHODS = { "get", "post", "put", "delete" };

	private static final String PROXY_RESOURCE = "proxy";

	@Test
	public final void getCoapContentTypeHeaderSemiColonTest() throws UnsupportedEncodingException {
		// create the http message and associate the entity
		HttpRequest httpRequest = new BasicHttpEntityEnclosingRequest("get", "http://localhost");
		HttpEntity httpEntity = new ByteArrayEntity("aaa".getBytes(Charset.forName("ISO-8859-1")));
		((BasicHttpEntityEnclosingRequest) httpRequest).setEntity(httpEntity);

		// create the header
		httpRequest.setHeader("content-type", "text/plain; charset=iso-8859-1");

		Request coapRequest = new GETRequest();

		// set the content-type
		int coapContentType = HttpTranslator.getCoapMediaType(httpRequest);
		coapRequest.setContentType(coapContentType);

		assertEquals(coapRequest.getContentType(), MediaTypeRegistry.TEXT_PLAIN);
	}

	@Test
	public final void getCoapContentTypeHeaderTest() throws UnsupportedEncodingException {
		// create the http message and associate the entity
		HttpRequest httpRequest = new BasicHttpEntityEnclosingRequest("get", "http://localhost");
		HttpEntity httpEntity = new ByteArrayEntity("aaa".getBytes());
		((BasicHttpEntityEnclosingRequest) httpRequest).setEntity(httpEntity);

		// create the header
		httpRequest.setHeader("content-type", "text/plain");

		Request coapRequest = new GETRequest();

		// set the content-type
		int coapContentType = HttpTranslator.getCoapMediaType(httpRequest);
		coapRequest.setContentType(coapContentType);

		assertEquals(MediaTypeRegistry.TEXT_PLAIN, coapRequest.getContentType());
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapContentTypeNoEntityTest() {
		// create an http message not carrying an entity
		HttpRequest httpRequest = new BasicHttpRequest("get", "http://localhost");
		// Request coapRequest = new GETRequest();

		// set the content-type
		// int coapContentType =
		HttpTranslator.getCoapMediaType(httpRequest);
		// coapRequest.setContentType(coapContentType);
		// assertEquals(coapRequest.getContentType(),
		// MediaTypeRegistry.UNDEFINED);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapContentTypeNoEntityTest2() {
		// create an http message carrying a null entity
		HttpMessage httpMessage = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");

		// set the content-type
		HttpTranslator.getCoapMediaType(httpMessage);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapContentTypeNoEntityTest3() {
		// create an http message carrying a null entity
		HttpRequest httpRequest = new BasicHttpEntityEnclosingRequest("get", "http://localhost");

		// get the content-type
		HttpTranslator.getCoapMediaType(httpRequest);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapContentTypeNullTest() {
		HttpTranslator.getCoapMediaType(null);
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.HttpTranslator#getCoapContentType(org.apache.http.HttpMessage, ch.ethz.inf.vs.californium.coap.Message)}
	 * .
	 * 
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public final void getCoapContentTypeTest() throws UnsupportedEncodingException {
		// create the http message and associate the entity
		HttpRequest httpRequest = new BasicHttpEntityEnclosingRequest("get", "http://localhost");
		HttpEntity httpEntity = new StringEntity("aaa");
		((BasicHttpEntityEnclosingRequest) httpRequest).setEntity(httpEntity);
		Request coapRequest = new GETRequest();

		// set the content-type
		int coapContentType = HttpTranslator.getCoapMediaType(httpRequest);
		coapRequest.setContentType(coapContentType);

		assertEquals(coapRequest.getContentType(), MediaTypeRegistry.TEXT_PLAIN);
	}

	@Test
	public final void getCoapContentTypeUnknownTest() throws UnsupportedEncodingException {
		// create the http message and associate the entity
		HttpRequest httpRequest = new BasicHttpEntityEnclosingRequest("get", "http://localhost");
		HttpEntity httpEntity = new ByteArrayEntity("aaa".getBytes());
		((BasicHttpEntityEnclosingRequest) httpRequest).setEntity(httpEntity);

		// create the header
		httpRequest.setHeader("content-type", "multipart/form-data");

		Request coapRequest = new GETRequest();

		// set the content-type
		int coapContentType = HttpTranslator.getCoapMediaType(httpRequest);
		coapRequest.setContentType(coapContentType);

		assertEquals(coapRequest.getContentType(), MediaTypeRegistry.APPLICATION_OCTET_STREAM);
	}

	@Test
	public final void getCoapOptionsAcceptTest() {
		// create the header
		String headerName = "accept";
		String headerValue = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
		Header header = new BasicHeader(headerName, headerValue);

		// create the message
		HttpMessage httpMessage = new BasicHttpRequest("get", "http://localhost");
		httpMessage.addHeader(header);

		// translate the header
		List<Option> options = HttpTranslator.getCoapOptions(httpMessage.getAllHeaders());
		assertFalse(options.isEmpty());
		assertTrue(options.size() == 2);

		// get the option list
		Message coapMessage = new GETRequest();
		coapMessage.setOptions(options);
		List<Option> testedOptions = coapMessage.getOptions(OptionNumberRegistry.ACCEPT);
		assertFalse(testedOptions.isEmpty());

		// only 2 to 3 content-types are translated
		assertTrue(testedOptions.size() == 2);
		assertTrue(testedOptions.contains(new Option(MediaTypeRegistry.TEXT_HTML, OptionNumberRegistry.ACCEPT)));
		assertTrue(testedOptions.contains(new Option(MediaTypeRegistry.APPLICATION_XML, OptionNumberRegistry.ACCEPT)));
	}

	@Test
	public final void getCoapOptionsAcceptWildcardTest() {
		// create the header
		String headerName = "accept";
		String headerValue = "text/*";
		Header header = new BasicHeader(headerName, headerValue);

		// create the message
		HttpMessage httpMessage = new BasicHttpRequest("get", "http://localhost");
		httpMessage.addHeader(header);

		// translate the header
		List<Option> options = HttpTranslator.getCoapOptions(httpMessage.getAllHeaders());
		assertFalse(options.isEmpty());

		// get the option list
		Message coapMessage = new GETRequest();
		coapMessage.setOptions(options);
		int optionNumber = Integer.parseInt(HttpTranslator.HTTP_TRANSLATION_PROPERTIES.getProperty("http.message.header." + headerName));
		List<Option> testedOptions = coapMessage.getOptions(optionNumber);
		assertFalse(testedOptions.isEmpty());

		// check content-types translated
		for (Integer mediaType : MediaTypeRegistry.parseWildcard(headerValue)) {
			assertTrue(testedOptions.contains(new Option(mediaType, OptionNumberRegistry.ACCEPT)));
		}
	}

	@Test
	public final void getCoapOptionsContentTypeTest() {
		// create the message
		HttpMessage httpMessage = new BasicHttpRequest("get", "http://localhost");

		// create the header
		String headerName = "content-type";
		String headerValue = "text/plain";
		Header header = new BasicHeader(headerName, headerValue);
		httpMessage.addHeader(header);

		// translate the header
		List<Option> options = HttpTranslator.getCoapOptions(httpMessage.getAllHeaders());
		// the context-type should not be handled by this method
		assertTrue(options.isEmpty());
	}

	@Test
	public final void getCoapOptionsEmptyTest() {
		HttpMessage httpMessage = new BasicHttpRequest("get", "http://localhost");
		List<Option> options = HttpTranslator.getCoapOptions(httpMessage.getAllHeaders());
		assertTrue(options.isEmpty());

		httpMessage = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "");
		options = HttpTranslator.getCoapOptions(httpMessage.getAllHeaders());
		assertTrue(options.isEmpty());
	}

	@Test
	public final void getCoapOptionsMaxAgeTest() {
		// create the message
		HttpMessage httpMessage = new BasicHttpRequest("get", "http://localhost");

		// create the header
		String headerName = "cache-control";
		int maxAge = 25;
		String headerValue = "max-age=" + maxAge;
		Header header = new BasicHeader(headerName, headerValue);
		httpMessage.addHeader(header);

		// translate the header
		List<Option> options = HttpTranslator.getCoapOptions(httpMessage.getAllHeaders());
		assertFalse(options.isEmpty());
		assertTrue(options.size() == 1);

		// get the option list
		Message coapMessage = new GETRequest();
		coapMessage.setOptions(options);
		Option testedOption = coapMessage.getFirstOption(OptionNumberRegistry.MAX_AGE);
		assertNotNull(testedOption);
		assertEquals(maxAge, testedOption.getIntValue());
	}

	@Test
	public final void getCoapOptionsMaxAgeTest2() {
		// create the message
		HttpMessage httpMessage = new BasicHttpRequest("get", "http://localhost");

		// create the header
		String headerName = "cache-control";
		String headerValue = "no-cache";
		Header header = new BasicHeader(headerName, headerValue);
		httpMessage.addHeader(header);

		// translate the header
		List<Option> options = HttpTranslator.getCoapOptions(httpMessage.getAllHeaders());
		assertFalse(options.isEmpty());
		assertTrue(options.size() == 1);

		// get the option list
		Message coapMessage = new GETRequest();
		coapMessage.setOptions(options);
		Option testedOption = coapMessage.getFirstOption(OptionNumberRegistry.MAX_AGE);
		assertNotNull(testedOption);
		assertEquals(0, testedOption.getIntValue());
	}

	@Test
	public final void getCoapOptionsNotFoundTest() {
		// create the header
		String headerName = "unknown-header";
		String headerValue = "aaaa";
		Header header = new BasicHeader(headerName, headerValue);

		// create the message
		HttpMessage httpMessage = new BasicHttpRequest("get", "http://localhost");
		httpMessage.addHeader(header);

		// translate the header
		List<Option> options = HttpTranslator.getCoapOptions(httpMessage.getAllHeaders());
		assertTrue(options.isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapOptionsNullTest() {
		HttpTranslator.getCoapOptions(null);
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.HttpTranslator#getCoapOptions(org.apache.http.HttpMessage)}
	 * .
	 */
	@Test
	public final void getCoapOptionsTest() {
		// create the message
		HttpMessage httpMessage = new BasicHttpRequest("get", "http://localhost");

		// create the header
		String headerName = "if-match";
		String headerValue = "\"737060cd8c284d8af7ad3082f209582d\"";
		Header header = new BasicHeader(headerName, headerValue);
		httpMessage.addHeader(header);

		// translate the header
		List<Option> options = HttpTranslator.getCoapOptions(httpMessage.getAllHeaders());
		assertFalse(options.isEmpty());

		// get the option list
		Message coapMessage = new GETRequest();
		coapMessage.setOptions(options);
		int optionNumber = Integer.parseInt(HttpTranslator.HTTP_TRANSLATION_PROPERTIES.getProperty("http.message.header." + headerName));
		assertEquals(coapMessage.getFirstOption(optionNumber).getStringValue(), headerValue);
	}

	@Test
	public final void getCoapPayloadCharsetTest() throws UnsupportedEncodingException, TranslationException {
		// create the entity with a different charset encoding
		String contentString = "aaa";
		HttpEntity httpEntity = new ByteArrayEntity(contentString.getBytes(Charset.forName("ISO_8859_1")), ContentType.TEXT_PLAIN);

		// get the translation
		byte[] payload = HttpTranslator.getCoapPayload(httpEntity);

		// check the payload
		assertNotNull(payload);
		assertArrayEquals(contentString.getBytes(Charset.forName("UTF-8")), payload);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapPayloadNullTest() throws TranslationException {
		HttpTranslator.getCoapPayload(null);
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.HttpTranslator#getCoapPayload(org.apache.http.HttpEntity)}
	 * .
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws TranslationException
	 */
	@Test
	public final void getCoapPayloadTest() throws UnsupportedEncodingException, TranslationException {
		// create the entity
		String contentString = "aaa";
		HttpEntity httpEntity = new StringEntity(contentString);

		// get the translation
		byte[] payload = HttpTranslator.getCoapPayload(httpEntity);

		// check the payload
		assertNotNull(payload);
		assertArrayEquals(contentString.getBytes(Charset.forName("UTF-8")), payload);
	}

	@Test(expected = TranslationException.class)
	public final void getCoapRequestConnectMethodTest() throws TranslationException {
		RequestLine requestLine = new BasicRequestLine("connect", "http://localhost", HttpVersion.HTTP_1_1);
		HttpRequest httpRequest = new BasicHttpRequest(requestLine);

		HttpTranslator.getCoapRequest(httpRequest, "", true);
	}

	@Test
	public final void getCoapRequestDecodeUriTest() throws TranslationException {
		String coapServerUri = "coap://%5B2001:620:8:101f:250:c2ff:ff18:8d32%5D:5683/sensors/temp";
		String uri = "/" + PROXY_RESOURCE + "/" + coapServerUri;

		getCoapRequestTemplateTest(coapServerUri, uri);
	}

	@Test
	public final void getCoapRequestLocalResourceTest() throws TranslationException {
		String resourceName = "localResource";
		String resourceString = "/" + resourceName;

		for (String httpMethod : COAP_METHODS) {
			// create the http request
			RequestLine requestLine = new BasicRequestLine(httpMethod, resourceString, HttpVersion.HTTP_1_1);
			HttpRequest httpRequest = new BasicHttpRequest(requestLine);

			// translate the request
			Request coapRequest = HttpTranslator.getCoapRequest(httpRequest, PROXY_RESOURCE, true);
			assertNotNull(coapRequest);

			// check the method translation
			int coapMethod = Integer.parseInt(HttpTranslator.HTTP_TRANSLATION_PROPERTIES.getProperty("http.request.method." + httpMethod));
			assertTrue(coapRequest.getCode() == coapMethod);

			// check the uri-path
			String uriPath = coapRequest.getFirstOption(OptionNumberRegistry.URI_PATH).getStringValue();
			assertEquals(uriPath, resourceName);

			// check the absence of the proxy-uri option
			assertNull(coapRequest.getFirstOption(OptionNumberRegistry.PROXY_URI));
		}
	}

	@Test(expected = TranslationException.class)
	public final void getCoapRequestMalformedUriTest() throws TranslationException {
		String coapServerUri = "/%coapServer:?^%&$//subresource";
		String uri = "http://localhost:80/" + PROXY_RESOURCE + "/" + coapServerUri;

		getCoapRequestTemplateTest(coapServerUri, uri);
	}

	@Test(expected = TranslationException.class)
	public final void getCoapRequestMalformedUriTest2() throws TranslationException {
		String coapServerUri = "coap://coapServer:5684/helloWorld";
		String uri = "localhost=(//" + PROXY_RESOURCE + "///$%&" + coapServerUri;

		getCoapRequestTemplateTest(coapServerUri, uri);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapRequestNullTest() throws TranslationException {
		HttpTranslator.getCoapRequest(null, "", true);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapRequestNullTest2() throws TranslationException {
		HttpTranslator.getCoapRequest(new BasicHttpRequest("get", "http://localhost"), null, true);
	}

	@Test(expected = TranslationException.class)
	public final void getCoapRequestOptionsMethodTest() throws TranslationException {
		RequestLine requestLine = new BasicRequestLine("options", "http://localhost", HttpVersion.HTTP_1_1);
		HttpRequest httpRequest = new BasicHttpRequest(requestLine);

		HttpTranslator.getCoapRequest(httpRequest, "", true);
	}

	@Test
	public final void getCoapRequestPayloadTest() throws TranslationException {
		String coapServerUri = "coap://coapServer:5684/helloWorld";

		for (String httpMethod : COAP_METHODS) {
			// create the http request
			RequestLine requestLine = new BasicRequestLine(httpMethod, "/" + PROXY_RESOURCE + "/" + coapServerUri, HttpVersion.HTTP_1_1);
			BasicHttpEntityEnclosingRequest httpRequest = new BasicHttpEntityEnclosingRequest(requestLine);

			// create the entity
			String contentString = "aaa";
			HttpEntity httpEntity = new ByteArrayEntity(contentString.getBytes(Charset.forName("ISO_8859_1")), ContentType.TEXT_PLAIN);
			httpRequest.setEntity(httpEntity);

			// set the content-type
			httpRequest.setHeader("content-type", "text/plain;  charset=iso-8859-1");

			// create the header
			String headerName = "if-match";
			String headerValue = "\"737060cd8c284d8af7ad3082f209582d\"";
			Header header = new BasicHeader(headerName, headerValue);
			httpRequest.addHeader(header);

			// translate the request
			Request coapRequest = HttpTranslator.getCoapRequest(httpRequest, PROXY_RESOURCE, true);
			assertNotNull(httpRequest);

			// check the method translation
			int coapMethod = Integer.parseInt(HttpTranslator.HTTP_TRANSLATION_PROPERTIES.getProperty("http.request.method." + httpMethod));
			assertTrue(coapRequest.getCode() == coapMethod);

			// check the proxy-uri
			String proxyUri = coapRequest.getFirstOption(OptionNumberRegistry.PROXY_URI).getStringValue();
			assertEquals(proxyUri, coapServerUri);

			// check the absence of the uri-* options
			assertNull(coapRequest.getFirstOption(OptionNumberRegistry.URI_PATH));
			assertNull(coapRequest.getFirstOption(OptionNumberRegistry.URI_HOST));
			assertNull(coapRequest.getFirstOption(OptionNumberRegistry.URI_QUERY));
			assertNull(coapRequest.getFirstOption(OptionNumberRegistry.URI_PORT));

			// check the payload
			assertNotNull(coapRequest.getPayload());
			assertArrayEquals(contentString.getBytes(Charset.forName("UTF-8")), coapRequest.getPayload());

			// check the option
			assertFalse(coapRequest.getOptions().isEmpty());
			int optionNumber = Integer.parseInt(HttpTranslator.HTTP_TRANSLATION_PROPERTIES.getProperty("http.message.header." + headerName));
			assertEquals(coapRequest.getFirstOption(optionNumber).getStringValue(), headerValue);

			// check the content-type
			assertEquals(coapRequest.getContentType(), MediaTypeRegistry.TEXT_PLAIN);
		}
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.HttpTranslator#getCoapRequest(org.apache.http.HttpRequest, java.lang.String, boolean)}
	 * .
	 * 
	 * @throws TranslationException
	 */
	@Test
	public final void getCoapRequestTest() throws TranslationException {
		String coapServerUri = "coap://coapServer:5684/helloWorld";
		String uri = "/" + PROXY_RESOURCE + "/" + coapServerUri;

		getCoapRequestTemplateTest(coapServerUri, uri);
	}

	@Test(expected = TranslationException.class)
	public final void getCoapRequestTraceMethodTest() throws TranslationException {
		RequestLine requestLine = new BasicRequestLine("trace", "/resource", HttpVersion.HTTP_1_1);
		HttpRequest httpRequest = new BasicHttpRequest(requestLine);

		HttpTranslator.getCoapRequest(httpRequest, "", true);
	}

	@Test(expected = TranslationException.class)
	public final void getCoapRequestUnknownMethodTest() throws TranslationException {
		RequestLine requestLine = new BasicRequestLine("UNKNOWN", "/resource", HttpVersion.HTTP_1_1);
		HttpRequest httpRequest = new BasicHttpRequest(requestLine);

		HttpTranslator.getCoapRequest(httpRequest, "", true);
	}

	@Test
	public final void getCoapRequestUriTest() throws TranslationException {
		// without coap://
		String coapServerUri = "coapServer:5684/resource1/subresource";
		// without the port
		String uri = "/" + PROXY_RESOURCE + "/" + coapServerUri;

		getCoapRequestTemplateTest(coapServerUri, uri);
	}

	@Test
	public final void getCoapResponseNoContentTest() throws TranslationException {
		// create the response
		StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NO_CONTENT, EnglishReasonPhraseCatalog.INSTANCE.getReason(HttpStatus.SC_NO_CONTENT, Locale.ENGLISH));
		HttpResponse httpResponse = new BasicHttpResponse(statusLine);

		// translate the http response
		Response coapResponse = HttpTranslator.getCoapResponse(httpResponse, new GETRequest());

		assertNotNull(coapResponse);
		assertTrue(coapResponse.getCode() == CodeRegistry.RESP_CHANGED);

		coapResponse = HttpTranslator.getCoapResponse(httpResponse, new POSTRequest());

		assertNotNull(coapResponse);
		assertTrue(coapResponse.getCode() == CodeRegistry.RESP_CHANGED);

		coapResponse = HttpTranslator.getCoapResponse(httpResponse, new PUTRequest());

		assertNotNull(coapResponse);
		assertTrue(coapResponse.getCode() == CodeRegistry.RESP_CHANGED);

		coapResponse = HttpTranslator.getCoapResponse(httpResponse, new DELETERequest());

		assertNotNull(coapResponse);
		assertTrue(coapResponse.getCode() == CodeRegistry.RESP_DELETED);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapResponseNullTest() throws TranslationException {
		HttpTranslator.getCoapResponse(null, new GETRequest());
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getCoapResponseNullTest2() throws TranslationException {
		HttpTranslator.getCoapResponse(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, ""), null);
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.HttpTranslator#getCoapResponse(org.apache.http.HttpResponse)}
	 * .
	 * 
	 * @throws IllegalAccessException
	 * @throws TranslationException
	 */
	@Test
	public final void getCoapResponseTest() throws IllegalAccessException, TranslationException {
		for (Field field : HttpStatus.class.getDeclaredFields()) {
			// get the code
			int httpCode = field.getInt(null);
			// if(http)

			// create the response
			StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, httpCode, EnglishReasonPhraseCatalog.INSTANCE.getReason(httpCode, Locale.ENGLISH));
			HttpResponse httpResponse = new BasicHttpResponse(statusLine);

			// create the entity
			String contentString = "aaa";
			HttpEntity httpEntity = new ByteArrayEntity(contentString.getBytes(Charset.forName("ISO_8859_1")), ContentType.TEXT_PLAIN);
			httpResponse.setEntity(httpEntity);

			// set the content-type
			httpResponse.setHeader("content-type", "text/plain;  charset=iso-8859-1");

			// create the header
			String headerName = "if-match";
			String headerValue = "\"737060cd8c284d8af7ad3082f209582d\"";
			Header header = new BasicHeader(headerName, headerValue);
			httpResponse.addHeader(header);

			// translate the http response
			Response coapResponse = HttpTranslator.getCoapResponse(httpResponse, new GETRequest());
			assertNotNull(coapResponse);

			// check the payload
			assertNotNull(coapResponse.getPayload());
			assertArrayEquals(contentString.getBytes(Charset.forName("UTF-8")), coapResponse.getPayload());

			// check the option
			assertFalse(coapResponse.getOptions().isEmpty());
			int optionNumber = Integer.parseInt(HttpTranslator.HTTP_TRANSLATION_PROPERTIES.getProperty("http.message.header." + headerName));
			assertEquals(coapResponse.getFirstOption(optionNumber).getStringValue(), headerValue);

			// check the content-type
			assertEquals(coapResponse.getContentType(), MediaTypeRegistry.TEXT_PLAIN);
		}
	}

	@Test(expected = TranslationException.class)
	public final void getCoapResponseUnknownCodeTest() throws TranslationException {
		// create the response
		StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, 199, "");
		HttpResponse httpResponse = new BasicHttpResponse(statusLine);

		// translate the http response
		HttpTranslator.getCoapResponse(httpResponse, new GETRequest());
	}

	@Test
	public final void getHttpEntityNullPayloadTest() throws TranslationException {
		Request request = new GETRequest();
		HttpEntity httpEntity = HttpTranslator.getHttpEntity(request);

		assertNull(httpEntity);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getHttpEntityNullTest() throws TranslationException {
		HttpTranslator.getHttpEntity(null);
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.HttpTranslator#getHttpEntity(ch.ethz.inf.vs.californium.coap.Message)}
	 * .
	 * 
	 * @throws IOException
	 * @throws TranslationException
	 */
	@Test
	public final void getHttpEntityTest() throws TranslationException, IOException {
		getEntity(MediaTypeRegistry.TEXT_PLAIN, ContentType.TEXT_PLAIN.toString());
	}

	@Test
	public final void getHttpEntityTest2() throws TranslationException, IOException {
		getEntity(MediaTypeRegistry.APPLICATION_XML, ContentType.APPLICATION_XML.toString());
	}

	@Test
	public final void getHttpEntityUndefinedContentTypeTest() throws TranslationException, IllegalStateException, IOException {
		getEntity(MediaTypeRegistry.UNDEFINED, ContentType.APPLICATION_OCTET_STREAM.toString());
	}

	@Test
	public final void getHttpHeadersAcceptTest() {
		// create the request
		Request coapRequest = new GETRequest();

		// set the options
		List<Integer> accept = new ArrayList<Integer>();
		accept.add(MediaTypeRegistry.TEXT_PLAIN);
		accept.add(MediaTypeRegistry.TEXT_HTML);
		accept.add(MediaTypeRegistry.APPLICATION_JSON);
		for (int type : accept) {
			coapRequest.addOption(new Option(type, OptionNumberRegistry.ACCEPT));
		}

		// translate the message
		Header[] headers = HttpTranslator.getHttpHeaders(coapRequest.getOptions());

		assertNotNull(headers);
		assertTrue(headers.length == 3);
		for (Header header : headers) {
			assertEquals(header.getName().toLowerCase(), OptionNumberRegistry.toString(OptionNumberRegistry.ACCEPT).toLowerCase());
			assertTrue(accept.contains(Integer.parseInt(header.getValue())));
		}
	}

	@Test
	public final void getHttpHeadersContentTypeTest() {
		// create the request
		Request coapRequest = new GETRequest();
		coapRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);

		// translate the message
		Header[] headers = HttpTranslator.getHttpHeaders(coapRequest.getOptions());

		assertNotNull(headers);
		assertTrue(headers.length == 0);
	}

	@Test
	public final void getHttpHeadersEmptyTest() {
		// create the request
		Request coapRequest = new GETRequest();

		// translate the message
		Header[] headers = HttpTranslator.getHttpHeaders(coapRequest.getOptions());

		assertNotNull(headers);
		assertTrue(headers.length == 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getHttpHeadersNullTest() {
		HttpTranslator.getHttpHeaders(null);
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.HttpTranslator#getHttpHeaders(ch.ethz.inf.vs.californium.coap.Message)}
	 * .
	 */
	@Test
	public final void getHttpHeadersTest() {
		// create the request
		Request coapRequest = new GETRequest();

		// set the options
		String etag = "1235234636547";
		coapRequest.setOption(new Option(etag, OptionNumberRegistry.ETAG));

		// translate the message
		Header[] headers = HttpTranslator.getHttpHeaders(coapRequest.getOptions());

		assertNotNull(headers);
		assertTrue(headers.length == 1);
		assertEquals(headers[0].getName().toLowerCase(), OptionNumberRegistry.toString(OptionNumberRegistry.ETAG).toLowerCase());
		assertEquals(headers[0].getValue().toLowerCase(), etag.toLowerCase());
	}

	@Test
	public final void getHttpHeadersUnknownOptionTest() {
		// create the request
		Request coapRequest = new GETRequest();
		coapRequest.setOption(new Option("coap://localhost:5683/resource", OptionNumberRegistry.PROXY_URI));
		coapRequest.setOption(new Option("EDCS", OptionNumberRegistry.BLOCK1));

		// translate the message
		Header[] headers = HttpTranslator.getHttpHeaders(coapRequest.getOptions());

		assertNotNull(headers);
		assertTrue(headers.length == 0);
	}

	@Test(expected = TranslationException.class)
	public final void getHttpRequestEmptyProxyUriTest() throws TranslationException {
		// create the request
		Request coapRequest = new GETRequest();

		// translate the message
		HttpTranslator.getHttpRequest(coapRequest);
	}

	@Test
	public final void getHttpRequestEntityTest() throws TranslationException, IOException {
		// create the coap request
		Request coapRequest = new POSTRequest();
		coapRequest.setOption(new Option("coap://localhost:5683/resource", OptionNumberRegistry.PROXY_URI));
		coapRequest.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		String payload = "aaa";
		coapRequest.setPayload(payload);

		// translate the request
		HttpRequest httpRequest = HttpTranslator.getHttpRequest(coapRequest);

		// check
		assertNotNull(httpRequest);
		assertNotNull(httpRequest.getAllHeaders());
		assertEquals(httpRequest.getClass(), BasicHttpEntityEnclosingRequest.class);

		// check the content-type
		assertEquals(httpRequest.getFirstHeader("content-type").getValue().toLowerCase(), "text/plain; charset=ISO-8859-1".toLowerCase());

		// check the content
		assertArrayEquals(payload.getBytes(), getByteArray(((HttpEntityEnclosingRequest) httpRequest).getEntity().getContent()));
	}

	@Test(expected = TranslationException.class)
	public final void getHttpRequestMalformedProxyUriTest() throws TranslationException {
		// create the request
		Request coapRequest = new GETRequest();
		coapRequest.setOption(new Option("coap:??=)(&)&%//localhost:=)??=)/?)%£$$!&5683/resource", OptionNumberRegistry.PROXY_URI));

		// translate the message
		HttpTranslator.getHttpRequest(coapRequest);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getHttpRequestNullTest() throws TranslationException {
		HttpTranslator.getHttpRequest(null);
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.HttpTranslator#getHttpRequest(ch.ethz.inf.vs.californium.coap.Request)}
	 * .
	 * 
	 * @throws TranslationException
	 */
	@Test
	public final void getHttpRequestTest() throws TranslationException {
		// create the coap request
		Request coapRequest = new GETRequest();
		coapRequest.setOption(new Option("coap://localhost:5683/resource", OptionNumberRegistry.PROXY_URI));

		// translate the request
		HttpRequest httpRequest = HttpTranslator.getHttpRequest(coapRequest);

		// check
		assertNotNull(httpRequest);
		assertTrue(httpRequest.getAllHeaders().length == 0);
		assertEquals(httpRequest.getClass(), BasicHttpRequest.class);
	}

	@Test
	public final void getHttpResponseErrorCodeTest() throws TranslationException, IOException {
		// create the coap response
		Response coapResponse = new Response(CodeRegistry.RESP_NOT_FOUND);
		String reason = "custom reason";
		coapResponse.setPayload(reason.getBytes("UTF-8"));

		// create the http response
		HttpRequest httpRequest = new BasicHttpRequest("GET", "coap://localhost");

		// translate the http response
		HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH));
		HttpTranslator.getHttpResponse(httpRequest, coapResponse, httpResponse);

		// check
		assertNotNull(httpResponse);
		assertTrue(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND);

		// check the payload
		assertEquals(ContentType.TEXT_PLAIN.toString().toLowerCase(), httpResponse.getFirstHeader("content-type").getValue().toLowerCase());
		byte[] byteArrayActual = getByteArray(httpResponse.getEntity().getContent());
		byte[] bytesExpected = reason.getBytes("ISO-8859-1");
		assertArrayEquals(bytesExpected, byteArrayActual);

		// check the headers
		assertNotNull(httpResponse.getAllHeaders());
		assertEquals(2, httpResponse.getAllHeaders().length);
	}

	@Test
	public final void getHttpResponseHeadMethodTest() throws TranslationException, UnsupportedEncodingException {
		// create the coap response
		Response coapResponse = new Response(CodeRegistry.RESP_CREATED);
		String payload = "aaa";
		coapResponse.setPayload(payload.getBytes("UTF-8"));

		// create the http response
		HttpRequest httpRequest = new BasicHttpRequest("HEAD", "coap://localhost");

		// translate the http response
		HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH));
		HttpTranslator.getHttpResponse(httpRequest, coapResponse, httpResponse);

		// check
		assertNotNull(httpResponse);
		assertTrue(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED);

		// check the payload
		assertFalse(httpResponse.containsHeader("content-type"));
		assertNull(httpResponse.getEntity());

		// check the headers
		assertNotNull(httpResponse.getAllHeaders());
		assertEquals(1, httpResponse.getAllHeaders().length);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getHttpResponseNullTest() throws TranslationException {
		HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH));
		HttpTranslator.getHttpResponse(null, new Response(0), httpResponse);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getHttpResponseNullTest2() throws TranslationException {
		HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH));
		HttpTranslator.getHttpResponse(new BasicHttpRequest("get", "http://localhost"), null, httpResponse);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void getHttpResponseNullTest3() throws TranslationException {
		HttpTranslator.getHttpResponse(new BasicHttpRequest("get", "http://localhost"), new Response(0), null);
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.HttpTranslator#getHttpResponse(ch.ethz.inf.vs.californium.coap.Response, org.apache.http.HttpResponse)}
	 * .
	 * 
	 * @throws IOException
	 */
	@Test
	public final void getHttpResponseTest() throws TranslationException, IOException {
		// create the coap response
		Response coapResponse = new Response(CodeRegistry.RESP_CREATED);
		String payload = "aaa";
		coapResponse.setPayload(payload.getBytes("UTF-8"));
		coapResponse.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		String etag = "254636899";
		coapResponse.setOption(new Option(etag, OptionNumberRegistry.ETAG));

		// create the http response
		HttpRequest httpRequest = new BasicHttpRequest("POST", "coap://localhost");

		// translate the http response
		HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH));
		HttpTranslator.getHttpResponse(httpRequest, coapResponse, httpResponse);

		// check
		assertNotNull(httpResponse);
		assertTrue(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED);

		// check the payload
		assertEquals(ContentType.TEXT_PLAIN.toString().toLowerCase(), httpResponse.getFirstHeader("content-type").getValue().toLowerCase());
		byte[] byteArrayActual = getByteArray(httpResponse.getEntity().getContent());
		byte[] bytesExpected = payload.getBytes("ISO-8859-1");
		assertArrayEquals(bytesExpected, byteArrayActual);

		// check the headers
		assertNotNull(httpResponse.getAllHeaders());
		assertEquals(3, httpResponse.getAllHeaders().length);
		assertEquals(etag, httpResponse.getFirstHeader("etag").getValue().toLowerCase());
	}

	@Test(expected = TranslationException.class)
	public final void getHttpResponseWrongCodeTest() throws TranslationException {
		// create the coap response
		Response coapResponse = new Response(CodeRegistry.EMPTY_MESSAGE);

		// create the http response
		HttpRequest httpRequest = new BasicHttpRequest("POST", "coap://localhost");

		// translate the http response
		HttpResponse httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH));
		HttpTranslator.getHttpResponse(httpRequest, coapResponse, httpResponse);
	}

	private byte[] getByteArray(InputStream inputStream) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		try {
			while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
		} catch (IOException e) {
		}

		return buffer.toByteArray();
	}

	/**
	 * @param coapServerUri
	 * @param uri
	 * @throws TranslationException
	 */
	private void getCoapRequestTemplateTest(String coapServerUri, String uri) throws TranslationException {
		for (String httpMethod : COAP_METHODS) {
			// create the http request
			RequestLine requestLine = new BasicRequestLine(httpMethod, uri, HttpVersion.HTTP_1_1);
			HttpRequest httpRequest = new BasicHttpRequest(requestLine);

			// translate the request
			Request coapRequest = HttpTranslator.getCoapRequest(httpRequest, PROXY_RESOURCE, true);
			assertNotNull(coapRequest);

			// check the method translation
			int coapMethod = Integer.parseInt(HttpTranslator.HTTP_TRANSLATION_PROPERTIES.getProperty("http.request.method." + httpMethod));
			assertTrue(coapRequest.getCode() == coapMethod);

			// check the proxy-uri
			String proxyUri = coapRequest.getFirstOption(OptionNumberRegistry.PROXY_URI).getStringValue();

			if (!proxyUri.equals(coapServerUri)) {
				try {
					coapServerUri = URLDecoder.decode(coapServerUri, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			if (!coapServerUri.contains("coap://")) {
				coapServerUri = "coap://" + coapServerUri;
			}
			assertEquals(proxyUri, coapServerUri);

			// check the absence of the uri-* options
			assertNull(coapRequest.getFirstOption(OptionNumberRegistry.URI_PATH));
			assertNull(coapRequest.getFirstOption(OptionNumberRegistry.URI_HOST));
			assertNull(coapRequest.getFirstOption(OptionNumberRegistry.URI_QUERY));
			assertNull(coapRequest.getFirstOption(OptionNumberRegistry.URI_PORT));
		}
	}

	/**
	 * @throws TranslationException
	 * @throws IOException
	 */
	private void getEntity(int coapContentType, String htmlContentType) throws TranslationException, IOException {
		// create the coap message
		Request coapRequest = new POSTRequest();
		String payloadContent = "aaa";
		coapRequest.setPayload(payloadContent.getBytes());
		coapRequest.setContentType(coapContentType);

		// translate the message
		HttpEntity httpEntity = HttpTranslator.getHttpEntity(coapRequest);

		// check the existence of the http entity
		assertNotNull(httpEntity);

		// check the content-type
		assertEquals(httpEntity.getContentType().getValue(), htmlContentType);

		// check the content
		assertArrayEquals(payloadContent.getBytes(), getByteArray(httpEntity.getContent()));
	}
}

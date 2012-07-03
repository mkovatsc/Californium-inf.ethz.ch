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

package ch.ethz.inf.vs.californium.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
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
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

/**
 * Class that provides the translations from the HTTP representations to the
 * CoAP representation and vice versa.
 * 
 * @author Francesco Corazza
 */
public final class HttpTranslator {

	private static final String PROPERTIES_FILENAME = "Proxy.properties";

	public static final Properties TRANSLATION_PROPERTIES = new Properties(PROPERTIES_FILENAME);

	protected static final Logger LOG = Logger.getLogger(HttpTranslator.class.getName());

	public static Request getCoapRequest(HttpRequest httpRequest, String proxyResource) throws TranslationException {
		if (httpRequest == null) {
			throw new IllegalArgumentException("httpRequest == null");
		}

		// get the http method
		String httpMethod = httpRequest.getRequestLine().getMethod().toLowerCase();

		// get the coap method
		String coapMethodString = HttpTranslator.TRANSLATION_PROPERTIES.getProperty("http.request.method." + httpMethod);
		if (coapMethodString.contains("error")) {
			throw new TranslationException(httpMethod + " method not supported");
		}
		int coapMethod = Integer.parseInt(coapMethodString);

		// create the request
		Request coapRequest = Request.getRequestForMethod(coapMethod);

		// set the uri
		String uriString = httpRequest.getRequestLine().getUri();
		// TODO check the uri with regexp

		// get the real requested coap server's uri if the proxy resource
		// requested is in the path of the http request
		if (uriString.matches(".?" + proxyResource + ".*")) {
			// find the occurrence of the resource
			int index = uriString.indexOf(proxyResource);
			// delete the slash
			index = uriString.indexOf('/', index);
			uriString = uriString.substring(index + 1);

			// if the uri hasn't the indication of the scheme, add it
			if (!uriString.matches("^coap://.*")) {
				uriString = "coap://" + uriString;
			}

			// the uri will be set as a proxy-uri option
			// set the proxy-uri option to allow the lower layers to underes
			Option proxyUriOption = new Option(uriString, OptionNumberRegistry.PROXY_URI);
			coapRequest.addOption(proxyUriOption);
		} else {
			// set the uri string as uri-path option
			Option uriPathOption = new Option(uriString, OptionNumberRegistry.URI_PATH);
			coapRequest.setOption(uriPathOption);
		}

		// set the proxy as the sender to receive the response correctly
		coapRequest.setURI(URI.create("localhost")); // TODO check

		// set the options
		setCoapOptions(httpRequest, coapRequest);

		// get the http entity if present in the http request
		if (httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntity httpEntity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
			handleEntity(coapRequest, httpEntity);
		}
		return coapRequest;
	}

	/**
	 * Gets the CoAP response from an incoming HTTP response. No null value
	 * returned. the error code.
	 * 
	 * @param httpResponse
	 *            the http response
	 * @return the coap response
	 * @throws TranslationException
	 */
	public static Response getCoapResponse(HttpResponse httpResponse) throws TranslationException {
		if (httpResponse == null) {
			throw new IllegalArgumentException("httpResponse == null");
		}

		// get/set the response code
		int httpCode = httpResponse.getStatusLine().getStatusCode();
		String coapCodeString = TRANSLATION_PROPERTIES.getProperty("http.response.code." + httpCode);
		int coapCode = Integer.parseInt(coapCodeString);

		// create the coap reaponse
		Response coapResponse = new Response(coapCode);

		// set the options
		setCoapOptions(httpResponse, coapResponse);

		// get the entity
		HttpEntity httpEntity = httpResponse.getEntity();
		if (httpEntity != null) {
			handleEntity(coapResponse, httpEntity);
		} // if (httpEntity != null)

		return coapResponse;
	}

	/**
	 * Get the http request starting from a CoAP request.
	 * 
	 * @param coapRequest
	 * @return
	 * @throws URISyntaxException
	 * @throws TranslationException
	 */
	public static HttpRequest getHttpRequest(Request coapRequest) throws URISyntaxException, TranslationException {
		if (coapRequest == null) {
			throw new IllegalArgumentException("coapRequest == null");
		}

		HttpRequest httpRequest = null;

		// get the coap method
		String coapMethod = CodeRegistry.toString(coapRequest.getCode());

		// obtain the requestLine
		RequestLine requestLine = new BasicRequestLine(coapMethod, coapRequest.getProxyUri().getPath(), HttpVersion.HTTP_1_1);

		// get the http entity
		HttpEntity httpEntity = getHttpEntity(coapRequest);

		// create the http request
		if (httpEntity == null) {
			httpRequest = new BasicHttpRequest(requestLine);
		} else {
			httpRequest = new BasicHttpEntityEnclosingRequest(requestLine);
			((HttpEntityEnclosingRequest) httpRequest).setEntity(httpEntity);

			// set the content-type header
			String contentTypeString = getHttpContentType(coapRequest);
			Header contentTypeHeader = new BasicHeader("content-type", contentTypeString);
			httpRequest.setHeader(contentTypeHeader);
		}
		return httpRequest;
	}

	/**
	 * Gets the http response from a CoAP response.
	 * 
	 * @param coapResponse
	 *            the coap response
	 * @param httpResponse2
	 * @return the http response
	 * @throws TranslationException
	 */
	public static HttpResponse getHttpResponse(Response coapResponse, HttpResponse httpResponse) throws TranslationException {
		if (coapResponse == null) {
			throw new IllegalArgumentException("coapResponse == null");
		}

		// get/set the response code
		int coapCode = coapResponse.getCode();
		String httpCodeString = TRANSLATION_PROPERTIES.getProperty("coap.response.code." + coapCode);
		int httpCode = Integer.parseInt(httpCodeString);

		// create and set the status line
		StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, httpCode, EnglishReasonPhraseCatalog.INSTANCE.getReason(httpCode, Locale.ENGLISH));
		httpResponse.setStatusLine(statusLine);

		// get the http content-type
		String contentTypeString;

		if (coapResponse.getContentType() == MediaTypeRegistry.UNDEFINED && (CodeRegistry.isClientError(coapCode) || CodeRegistry.isServerError(coapCode))) {
			// if the content-type is not set in the coap response and if the
			// response contains an error, then the content-type should set to
			// text-plain
			contentTypeString = ContentType.TEXT_PLAIN.getMimeType();
		} else {
			contentTypeString = getHttpContentType(coapResponse);
		}

		// set the content-type header
		Header contentTypeHeader = new BasicHeader("content-type", contentTypeString);
		httpResponse.setHeader(contentTypeHeader);

		// get the http entity
		HttpEntity httpEntity = getHttpEntity(coapResponse);
		httpResponse.setEntity(httpEntity);

		return httpResponse;
	}

	private static byte[] changeCharset(byte[] payload, Charset fromCharset, Charset toCharset) throws TranslationException {
		try {
			// decode with the source charset
			CharsetDecoder decoder = fromCharset.newDecoder();
			CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(payload));

			// encode in destination charset
			CharsetEncoder encoder = toCharset.newEncoder();
			ByteBuffer byteBuffer = encoder.encode(charBuffer);
			payload = byteBuffer.array();
		} catch (CharacterCodingException e) {
			throw new TranslationException("Problem in the decoding/encoding charset", e);
		}

		return payload;
	}

	private static String getHttpContentType(Message coapMessage) {
		// get the content-type
		int coapContentType = coapMessage.getContentType();

		// get the content type
		String coapContentTypeString = MediaTypeRegistry.toString(coapContentType);
		ContentType contentType = ContentType.parse(coapContentTypeString);
		return contentType.getMimeType();
	}

	/**
	 * Generate an HTTP entity starting from a CoAP request.
	 * 
	 * @param coapMessage
	 *            the coap message
	 * @return null if the request has no payload
	 * @throws TranslationException
	 */
	private static HttpEntity getHttpEntity(Message coapMessage) throws TranslationException {
		// the result
		HttpEntity httpEntity = null;

		// check if coap request has a payload
		byte[] payload = coapMessage.getPayload();
		if (payload != null && payload.length != 0) {
			// get the content-type
			int coapContentType = coapMessage.getContentType();

			// if the payload is printable, choose the right charset
			if (coapContentType != MediaTypeRegistry.UNDEFINED && MediaTypeRegistry.isPrintable(coapContentType)) {
				// get the content type
				String coapContentTypeString = MediaTypeRegistry.toString(coapContentType);
				ContentType contentType = ContentType.parse(coapContentTypeString);
				Charset charset = contentType.getCharset();

				// use the default if the charset is null
				if (charset == null) {
					charset = Charset.defaultCharset();
				}

				// the only supported charset in CoAP is UTF-8
				Charset utf8Charset = Charset.forName("UTF-8");
				if (!charset.equals(utf8Charset)) {
					payload = changeCharset(payload, utf8Charset, charset);
				}

				String payloadString = new String(payload, charset);
				// create the entity
				httpEntity = new StringEntity(payloadString, charset);
			} else {
				// create the entity
				httpEntity = new ByteArrayEntity(payload);
			}
		}

		return httpEntity;
	}

	/**
	 * Gets the payload.
	 * 
	 * @param httpEntity
	 *            the http entity
	 * @return the payload
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws TranslationException
	 */
	private static byte[] getPayload(HttpEntity httpEntity) throws IOException, TranslationException {
		// get the bytes from the entity
		byte[] payload = EntityUtils.toByteArray(httpEntity);
		// ensure all content has been consumed, so that the underlying
		// connection could be re-used
		EntityUtils.consume(httpEntity);

		// check the charset
		ContentType contentType = ContentType.getOrDefault(httpEntity);
		Charset charset = contentType.getCharset();

		// the only supported charset in CoAP is UTF-8
		Charset utf8Charset = Charset.forName("UTF-8");
		if (charset != null && !charset.equals(utf8Charset)) {
			payload = changeCharset(payload, charset, utf8Charset);
		}
		return payload;
	}

	/**
	 * @param coapMessage
	 * @param httpEntity
	 * @throws TranslationException
	 */
	private static void handleEntity(Message coapMessage, HttpEntity httpEntity) throws TranslationException {
		// get/set the content-type
		ContentType contentType = ContentType.getOrDefault(httpEntity);
		int coapContentType = MediaTypeRegistry.parse(contentType.getMimeType());
		// TODO check other conversions
		if (coapContentType != MediaTypeRegistry.UNDEFINED) {
			coapMessage.setContentType(coapContentType);
		}

		// get/set the payload
		try {
			// get the content of the entity regarding also the charset
			// encoding used
			byte[] payload = getPayload(httpEntity);
			coapMessage.setPayload(payload);
		} catch (IOException e) {
			LOG.warning("Cannot get the content of the http entity: " + e.getMessage());
			throw new TranslationException("Cannot get the content of the http entity", e);
		} finally {
			try {
				// ensure all content has been consumed, so that the
				// underlying connection could be re-used
				EntityUtils.consume(httpEntity);
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Sets the coap options.
	 * 
	 * @param httpMessage
	 *            the http message
	 * @param coapMessage
	 *            the coap message
	 */
	private static void setCoapOptions(HttpMessage httpMessage, Message coapMessage) {
		// get the headers
		HeaderIterator headerIterator = httpMessage.headerIterator();
		while (headerIterator.hasNext()) {
			Header header = headerIterator.nextHeader();
			String optionCodeString = TRANSLATION_PROPERTIES.getProperty("http.request.header." + header.getName().toLowerCase());

			// ignore the header if not found in the properties file
			if (optionCodeString != null) {
				// create the option for the current header
				int optionNumber = Integer.parseInt(optionCodeString);
				Option option = new Option(optionNumber);

				// if the option is accept

				// get the value of the current header
				String headerValue = header.getValue();
				// delete the last part if any
				headerValue = headerValue.split(";")[0];

				for (String headerFragment : headerValue.split(",")) {
					if (option.getOptionNumber() == OptionNumberRegistry.ACCEPT) {
						int coapContentType = MediaTypeRegistry.parse(headerFragment);
						// TODO check other conversions
						if (coapContentType != MediaTypeRegistry.UNDEFINED) {
							option.setIntValue(coapContentType);
							coapMessage.addOption(option);
						}
					} else {
						option.setValue(headerValue.getBytes(Charset.forName("ISO-8859-1")));
						coapMessage.addOption(option);
					}
				}
			}
		}
	}

	/**
	 * The Constructor is private because the class is only an helper class and
	 * cannot be instantiated.
	 */
	private HttpTranslator() {

	}

}

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
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.UnmappableCharacterException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.entity.AbstractHttpEntity;
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

import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;

/**
 * Class providing the translations (mappings) from the HTTP message
 * representations to the CoAP message representations and vice versa.
 * 
 * @author Francesco Corazza
 */
public final class HttpTranslator {

	/**
	 * Property file containing the mappings between coap messages and http
	 * messages.
	 */
	public static final Properties HTTP_TRANSLATION_PROPERTIES = new Properties("Proxy.properties");

	// Error constants
	public static final int STATUS_TIMEOUT = HttpStatus.SC_GATEWAY_TIMEOUT;
	public static final int STATUS_NOT_FOUND = HttpStatus.SC_BAD_GATEWAY;
	public static final int STATUS_TRANSLATION_ERROR = HttpStatus.SC_BAD_GATEWAY;
	public static final int STATUS_URI_MALFORMED = HttpStatus.SC_BAD_REQUEST;
	public static final int STATUS_WRONG_METHOD = HttpStatus.SC_NOT_IMPLEMENTED;

	/** Default value for the option max-age of the coap messages. */
	public static final int DEFAULT_MAX_AGE = 60;
	public static final int ZERO_MAX_AGE = 0;

	protected static final Logger LOG = Logger.getLogger(HttpTranslator.class.getName());

	public static int getCoapContentType(HttpMessage httpMessage) {
		if (httpMessage == null) {
			throw new IllegalArgumentException("httpMessage == null");
		}

		// set the content-type with a default value
		int coapContentType = MediaTypeRegistry.UNDEFINED;

		// get the entity
		HttpEntity httpEntity = null;
		if (httpMessage instanceof HttpResponse) {
			httpEntity = ((HttpResponse) httpMessage).getEntity();
		} else if (httpMessage instanceof HttpEntityEnclosingRequest) {
			httpEntity = ((HttpEntityEnclosingRequest) httpMessage).getEntity();
		}

		// check that the entity is actually present in the http message
		if (httpEntity != null) {

			// get the content-type from the entity
			ContentType contentType = ContentType.get(httpEntity);

			if (contentType != null) {
				String contentTypeString = contentType.toString();
				// delete the last part if any
				// (coap handles only utf-8 encoding)
				contentTypeString = contentTypeString.split(";")[0];
				coapContentType = MediaTypeRegistry.parse(contentTypeString);
			}

			// if undefined, get the content-type from the header to set the
			// proper content-type
			if (coapContentType == MediaTypeRegistry.UNDEFINED) {
				Header contentTypeHeader = httpMessage.getFirstHeader("content-type");
				if (contentTypeHeader != null) {
					String contentTypeString = contentTypeHeader.getValue();
					if (contentTypeString != null && !contentTypeString.isEmpty()) {
						// remove the last part of the header value because in
						// coap only UTF-8 is allowed as charset
						contentTypeString = contentTypeString.split(";")[0];
						coapContentType = MediaTypeRegistry.parse(contentTypeString);
					}
				}
			}

			// if not recognized, the content-type should be
			// application/octet-stream (draft-castellani-core-http-mapping 6.2)
			if (coapContentType == MediaTypeRegistry.UNDEFINED) {
				coapContentType = MediaTypeRegistry.APPLICATION_OCTET_STREAM;
			}
		}

		return coapContentType;
	}

	/**
	 * Gets the coap options starting from an http message. The content-type is
	 * not handled by this method.
	 * 
	 * @param headers
	 *            the http message
	 * @param coapMessage
	 *            the coap message
	 */
	public static List<Option> getCoapOptions(Header[] headers) {
		if (headers == null) {
			throw new IllegalArgumentException("httpMessage == null");
		}

		List<Option> optionList = new LinkedList<Option>();

		// iterate over the headers
		for (Header header : headers) {
			String headerName = header.getName().toLowerCase();

			// get the mapping from the property file
			String optionCodeString = HTTP_TRANSLATION_PROPERTIES.getProperty("http.message.header." + headerName);

			// ignore the header if not found in the properties file
			if (optionCodeString == null || optionCodeString.isEmpty()) {
				continue;
			}

			// get the option number
			int optionNumber = OptionNumberRegistry.RESERVED_0;
			try {
				optionNumber = Integer.parseInt(optionCodeString.trim());
			} catch (Exception e) {
				// ignore the option if not recognized
				continue;
			}

			// ignore the content-type because it will be handled with the
			// payload
			if (optionNumber == OptionNumberRegistry.CONTENT_TYPE) {
				continue;
			}

			// get the value of the current header
			String headerValue = header.getValue();

			// if the option is accept, it needs to translate the
			// values
			if (optionNumber == OptionNumberRegistry.ACCEPT) {
				// remove the part where the client express the weight of each
				// choice
				headerValue = headerValue.split(";")[0];

				// iterate for each content-type indicated
				for (String headerFragment : headerValue.split(",")) {
					// translate the content-type
					int coapContentType = MediaTypeRegistry.parse(headerFragment);

					// if is present a conversion for the content-type, then add
					// a new option
					if (coapContentType != MediaTypeRegistry.UNDEFINED) {
						// create the option
						Option option = new Option(optionNumber);
						option.setIntValue(coapContentType);
						optionList.add(option);
					}
				}
			} else {
				// create the option
				Option option = new Option(optionNumber);
				// option.setValue(headerValue.getBytes(Charset.forName("ISO-8859-1")));
				option.setStringValue(headerValue);
				optionList.add(option);
			}
		} // while (headerIterator.hasNext())

		return optionList;
	}

	/**
	 * Method to map the http entity of a http message in a coherent payload for
	 * the coap message.
	 * 
	 * @param coapMessage
	 *            the coap message
	 * @param httpEntity
	 *            the http entity
	 * @throws TranslationException
	 *             the translation exception
	 */
	public static byte[] getCoapPayload(HttpEntity httpEntity) throws TranslationException {
		if (httpEntity == null) {
			throw new IllegalArgumentException("httpEntity == null");
		}

		byte[] payload = null;
		try {
			// get the bytes from the entity
			payload = EntityUtils.toByteArray(httpEntity);

			// the only supported charset in CoAP is UTF-8
			Charset utf8Charset = Charset.forName("UTF-8");

			// check the charset
			ContentType contentType = ContentType.getOrDefault(httpEntity);
			Charset charset = contentType.getCharset();
			if (charset != null && !charset.equals(utf8Charset)) {
				// translate the payload to the utf-8 charset
				payload = changeCharset(payload, charset, utf8Charset);
			}
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

		return payload;
	}

	/**
	 * Gets the coap request.
	 * 
	 * @param httpRequest
	 *            the http request
	 * @param proxyResource
	 *            the proxy resource, if present in the uri, indicates the need
	 *            of forwarding for the current request
	 * @return the coap request
	 * @throws TranslationException
	 *             the translation exception
	 */
	public static Request getCoapRequest(HttpRequest httpRequest, String proxyResource) throws TranslationException {
		if (httpRequest == null) {
			throw new IllegalArgumentException("httpRequest == null");
		}
		if (proxyResource == null) {
			throw new IllegalArgumentException("proxyResource == null");
		}

		// get the http method
		String httpMethod = httpRequest.getRequestLine().getMethod().toLowerCase();

		// get the coap method
		String coapMethodString = HTTP_TRANSLATION_PROPERTIES.getProperty("http.request.method." + httpMethod);
		if (coapMethodString == null || coapMethodString.contains("error")) {
			LOG.warning(httpMethod + " method not supported");
			throw new InvalidMethodException(httpMethod + " method not supported");
		}

		int coapMethod = 0;
		try {
			coapMethod = Integer.parseInt(coapMethodString.trim());
		} catch (NumberFormatException e) {
			LOG.warning("Cannot convert the http method in coap method: " + e);
			throw new TranslationException("Cannot convert the http method in coap method", e);
		}

		// create the request
		Request coapRequest = Request.getRequestForMethod(coapMethod);

		// get the uri
		String uriString = httpRequest.getRequestLine().getUri();
		// remove the initial "/"
		uriString = uriString.substring(1);

		// decode the uri to translate the application/x-www-form-urlencoded
		// format
		try {
			uriString = URLDecoder.decode(uriString, "UTF-8");
		} catch (IllegalArgumentException e) {
			LOG.warning("Malformed uri: " + e.getMessage());
			throw new InvalidFieldException("Malformed uri: " + e.getMessage());
		} catch (UnsupportedEncodingException e) {
			LOG.warning("Failed to decode the uri: " + e.getMessage());
			throw new TranslationException("Failed decoding the uri: " + e.getMessage());
		}

		// if the uri contains the proxy resource name, the request should be
		// forwarded and it is needed to get the real requested coap server's
		// uri
		// e.g.:
		// /proxy/vslab-dhcp-17.inf.ethz.ch:5684/helloWorld
		// proxy resource: /proxy
		// coap server: vslab-dhcp-17.inf.ethz.ch:5684
		// coap resource: helloWorld
		if (uriString.matches(".?" + proxyResource + ".*")) {

			// find the first occurrence of the proxy resource
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

			// set the proxy as the sender to receive the response correctly
			try {
				// TODO check with multihomed hosts
				InetAddress localHostAddress = InetAddress.getLocalHost();
				EndpointAddress localHostEndpoint = new EndpointAddress(localHostAddress);
				coapRequest.setPeerAddress(localHostEndpoint);
			} catch (UnknownHostException e) {
				LOG.warning("Cannot get the localhost address: " + e.getMessage());
				throw new TranslationException("Cannot get the localhost address: " + e.getMessage());
			}
		} else {
			// if the uri does not contains the proxy resource, it means the
			// request is local to the proxy and it shouldn't be forwarded

			// set the uri string as uri-path option
			Option uriPathOption = new Option(uriString, OptionNumberRegistry.URI_PATH);
			coapRequest.setOption(uriPathOption);
		}

		// translate the http headers in coap options
		List<Option> coapOptions = getCoapOptions(httpRequest.getAllHeaders());
		coapRequest.setOptions(coapOptions);

		// set the payload if the http entity is present
		if (httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntity httpEntity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();

			// translate the http entity in coap payload
			byte[] payload = getCoapPayload(httpEntity);
			coapRequest.setPayload(payload);

			// set the content-type
			int coapContentType = getCoapContentType(httpRequest);
			coapRequest.setContentType(coapContentType);
		}

		return coapRequest;
	}

	/**
	 * Gets the CoAP response from an incoming HTTP response. No null value
	 * returned. the error code.
	 * 
	 * @param httpResponse
	 *            the http response
	 * @param coapRequest
	 * @return the coap response
	 * @throws TranslationException
	 *             the translation exception
	 */
	public static Response getCoapResponse(HttpResponse httpResponse, Request coapRequest) throws TranslationException {
		if (httpResponse == null) {
			throw new IllegalArgumentException("httpResponse == null");
		}
		if (coapRequest == null) {
			throw new IllegalArgumentException("coapRequest == null");
		}

		// get/set the response code
		int httpCode = httpResponse.getStatusLine().getStatusCode();
		int coapCode = 0;
		int coapMethod = coapRequest.getCode();

		// the code 204-"no content" should be managed
		// separately because it can be mapped to different coap codes
		// depending on the request that has originated the response
		if (httpCode == HttpStatus.SC_NO_CONTENT) {
			if (coapMethod == CodeRegistry.METHOD_DELETE) {
				coapCode = CodeRegistry.RESP_DELETED;
			} else {
				coapCode = CodeRegistry.RESP_CHANGED;
			}
		} else {
			// get the translation from the property file
			String coapCodeString = HTTP_TRANSLATION_PROPERTIES.getProperty("http.response.code." + httpCode);

			if (coapCodeString == null || coapCodeString.isEmpty()) {
				LOG.warning("coapCodeString == null");
				throw new TranslationException("coapCodeString == null");
			}

			try {
				coapCode = Integer.parseInt(coapCodeString.trim());
			} catch (NumberFormatException e) {
				LOG.warning("Cannot convert the status code in number: " + e.getMessage());
				throw new TranslationException("Cannot convert the status code in number", e);
			}
		}

		// create the coap reaponse
		Response coapResponse = new Response(coapCode);

		// translate the http headers in coap options
		List<Option> coapOptions = getCoapOptions(httpResponse.getAllHeaders());
		coapResponse.setOptions(coapOptions);

		// the response should indicate a max-age value (CoAP 10.1.1)
		if (coapResponse.getOptions(OptionNumberRegistry.MAX_AGE).isEmpty()) {
			// The Max-Age Option for responses to POST, PUT or DELETE requests
			// should always be set to 0 (draft-castellani-core-http-mapping).
			if (coapMethod == CodeRegistry.METHOD_GET) {
				coapResponse.setMaxAge(DEFAULT_MAX_AGE);
			} else {
				coapResponse.setMaxAge(ZERO_MAX_AGE);
			}
		}

		// get the entity
		HttpEntity httpEntity = httpResponse.getEntity();
		if (httpEntity != null) {
			// translate the http entity in coap payload
			byte[] payload = getCoapPayload(httpEntity);
			coapResponse.setPayload(payload);

			// set the content-type
			int coapContentType = getCoapContentType(httpResponse);
			coapResponse.setContentType(coapContentType);
		}

		return coapResponse;
	}

	/**
	 * Generate an HTTP entity starting from a CoAP request. If the coap message
	 * has no payload, it returns a null http entity.
	 * 
	 * @param coapMessage
	 *            the coap message
	 * @return null if the request has no payload
	 * @throws TranslationException
	 *             the translation exception
	 */
	public static HttpEntity getHttpEntity(Message coapMessage) throws TranslationException {
		if (coapMessage == null) {
			throw new IllegalArgumentException("coapMessage == null");
		}

		// the result
		HttpEntity httpEntity = null;

		// check if coap request has a payload
		byte[] payload = coapMessage.getPayload();
		if (payload != null && payload.length != 0) {

			// get the coap content-type
			int coapContentType = coapMessage.getContentType();
			ContentType contentType = null;

			// if the content type is not set, translate with octect-stream
			if (coapContentType == MediaTypeRegistry.UNDEFINED) {
				contentType = ContentType.APPLICATION_OCTET_STREAM;
			} else {
				// search for the media type inside the property file
				String coapContentTypeString = HTTP_TRANSLATION_PROPERTIES.getProperty("coap.message.media." + coapContentType);

				// if the content-type has not been found in the property file,
				// try to get its string value (expressed in mime type)
				if (coapContentTypeString == null || coapContentTypeString.isEmpty()) {
					coapContentTypeString = MediaTypeRegistry.toString(coapContentType);

					// if the coap content-type is printable, it is needed to
					// set the default charset (i.e., UTF-8)
					if (MediaTypeRegistry.isPrintable(coapContentType)) {
						coapContentTypeString += "; charset=UTF-8";
					}
				}

				// parse the content type
				try {
					contentType = ContentType.parse(coapContentTypeString);
				} catch (ParseException e) {
					contentType = ContentType.APPLICATION_OCTET_STREAM;
				} catch (UnsupportedCharsetException e) {
					contentType = ContentType.APPLICATION_OCTET_STREAM;
				}
			}

			// get the charset
			Charset charset = contentType.getCharset();
			// if there is a charset, means that the content is not binary
			if (charset != null) {

				// according to the class ContentType the default content-type
				// with
				// UTF-8 charset is application/json. If the content-type
				// parsed is different, or is not iso encoded, it is needed a
				// translation
				Charset isoCharset = Charset.forName("ISO-8859-1");
				if (!charset.equals(isoCharset) && contentType != ContentType.APPLICATION_JSON) {
					byte[] newPayload = changeCharset(payload, charset, isoCharset);

					// since ISO-8859-1 is a subset of UTF-8, it is needed to
					// check if the mapping could be accomplished, only if the
					// operation is succesful the payload and the charset should
					// be changed
					if (newPayload != null) {
						payload = newPayload;
						// if the charset is changed, also the entire
						// content-type must change
						contentType = ContentType.create(contentType.getMimeType(), isoCharset);
					}
				}

				// create the content
				String payloadString = new String(payload, contentType.getCharset());

				// create the entity
				httpEntity = new StringEntity(payloadString, contentType);
			} else {
				// create the entity
				httpEntity = new ByteArrayEntity(payload);
			}

			// set the content-type
			((AbstractHttpEntity) httpEntity).setContentType(contentType.toString());
		} // if (payload != null && payload.length != 0)

		return httpEntity;
	}

	/**
	 * Gets the http headers.
	 * 
	 * @param optionList
	 *            the coap message
	 * @param httpMessage
	 *            the http message
	 */
	public static Header[] getHttpHeaders(List<Option> optionList) {
		if (optionList == null) {
			throw new IllegalArgumentException("coapMessage == null");
		}

		List<Header> headers = new LinkedList<Header>();

		// iterate over each option
		for (Option option : optionList) {
			// skip content-type because it should be translated while handling
			// the payload
			// skip proxy-uri because it has to be translated in a different way
			int optionNumber = option.getOptionNumber();
			if (optionNumber != OptionNumberRegistry.CONTENT_TYPE && optionNumber != OptionNumberRegistry.CONTENT_TYPE) {
				// get the mapping from the property file
				String headerName = HTTP_TRANSLATION_PROPERTIES.getProperty("coap.message.option." + optionNumber);

				// set the header
				if (headerName != null && !headerName.isEmpty()) {
					Header header = new BasicHeader(headerName, option.getStringValue());
					headers.add(header);
				}
			}
		}

		return headers.toArray(new Header[0]);
	}

	/**
	 * Get the http request starting from a CoAP request.
	 * 
	 * @param coapRequest
	 *            the coap request
	 * @return the http request
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 * @throws TranslationException
	 *             the translation exception
	 */
	public static HttpRequest getHttpRequest(Request coapRequest) throws TranslationException {
		if (coapRequest == null) {
			throw new IllegalArgumentException("coapRequest == null");
		}

		HttpRequest httpRequest = null;

		// get the coap method
		String coapMethod = CodeRegistry.toString(coapRequest.getCode());

		// get the proxy-uri
		URI proxyUri = null;
		try {
			proxyUri = coapRequest.getProxyUri();
		} catch (URISyntaxException e) {
			LOG.warning("Cannot get the proxy-uri from the coap message: " + e.getMessage());
			throw new InvalidFieldException("Cannot get the proxy-uri from the coap message", e);
		}

		if (proxyUri == null) {
			LOG.warning("proxyUri == null");
			throw new InvalidFieldException("proxyUri == null");
		}

		// create the requestLine
		RequestLine requestLine = new BasicRequestLine(coapMethod, proxyUri.toString(), HttpVersion.HTTP_1_1);

		// get the http entity
		HttpEntity httpEntity = getHttpEntity(coapRequest);

		// create the http request
		if (httpEntity == null) {
			httpRequest = new BasicHttpRequest(requestLine);
		} else {
			httpRequest = new BasicHttpEntityEnclosingRequest(requestLine);
			((HttpEntityEnclosingRequest) httpRequest).setEntity(httpEntity);

			// get the content-type from the entity and set the header
			ContentType contentType = ContentType.get(httpEntity);
			httpRequest.setHeader("content-type", contentType.toString());
		}

		// set the headers
		Header[] headers = getHttpHeaders(coapRequest.getOptions());
		for (Header header : headers) {
			httpRequest.addHeader(header);
		}

		return httpRequest;
	}

	/**
	 * Gets the http response from a CoAP response.
	 * 
	 * @param coapResponse
	 *            the coap response
	 * @param httpResponse
	 * @param httpResponse
	 *            the http response
	 * @return the http response
	 * @throws TranslationException
	 *             the translation exception
	 */
	public static void getHttpResponse(HttpRequest httpRequest, Response coapResponse, HttpResponse httpResponse) throws TranslationException {
		if (httpRequest == null) {
			throw new IllegalArgumentException("httpRequest == null");
		}
		if (coapResponse == null) {
			throw new IllegalArgumentException("coapResponse == null");
		}
		if (httpResponse == null) {
			throw new IllegalArgumentException("httpResponse == null");
		}

		// get/set the response code
		int coapCode = coapResponse.getCode();
		String httpCodeString = HTTP_TRANSLATION_PROPERTIES.getProperty("coap.response.code." + coapCode);

		if (httpCodeString == null || httpCodeString.isEmpty()) {
			LOG.warning("httpCodeString == null");
			throw new TranslationException("httpCodeString == null");
		}

		int httpCode = 0;
		try {
			httpCode = Integer.parseInt(httpCodeString.trim());
		} catch (NumberFormatException e) {
			LOG.warning("Cannot convert the coap code in http status code" + e);
			throw new TranslationException("Cannot convert the coap code in http status code", e);
		}

		// create the http response and set the status line
		String reason = EnglishReasonPhraseCatalog.INSTANCE.getReason(httpCode, Locale.ENGLISH);
		StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, httpCode, reason);
		httpResponse.setStatusLine(statusLine);

		// set the headers
		Header[] headers = getHttpHeaders(coapResponse.getOptions());
		httpResponse.setHeaders(headers);

		// set max-age if not already set
		if (!httpResponse.containsHeader("max-age")) {
			httpResponse.setHeader("max-age", Integer.toString(DEFAULT_MAX_AGE));
		}

		// get the http entity if the request was not HEAD
		if (!httpRequest.getRequestLine().getMethod().equalsIgnoreCase("head")) {

			// if the content-type is not set in the coap response and if the
			// response contains an error, then the content-type should set to
			// text-plain
			if (coapResponse.getContentType() == MediaTypeRegistry.UNDEFINED && (CodeRegistry.isClientError(coapCode) || CodeRegistry.isServerError(coapCode))) {
				coapResponse.setContentType(MediaTypeRegistry.TEXT_PLAIN);
			}

			HttpEntity httpEntity = getHttpEntity(coapResponse);
			httpResponse.setEntity(httpEntity);

			// get the content-type from the entity and set the header
			ContentType contentType = ContentType.get(httpEntity);
			httpResponse.setHeader("content-type", contentType.toString());
		}
	}

	/**
	 * Change charset.
	 * 
	 * @param payload
	 *            the payload
	 * @param fromCharset
	 *            the from charset
	 * @param toCharset
	 *            the to charset
	 * @return the byte[]
	 * @throws TranslationException
	 *             the translation exception
	 */
	private static byte[] changeCharset(byte[] payload, Charset fromCharset, Charset toCharset) throws TranslationException {
		try {
			// decode with the source charset
			CharsetDecoder decoder = fromCharset.newDecoder();
			CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(payload));
			decoder.flush(charBuffer);

			// encode to the destination charset
			CharsetEncoder encoder = toCharset.newEncoder();
			ByteBuffer byteBuffer = encoder.encode(charBuffer);
			encoder.flush(byteBuffer);
			payload = byteBuffer.array();
		} catch (UnmappableCharacterException e) {
			// thrown when an input character (or byte) sequence is valid but
			// cannot be mapped to an output byte (or character) sequence.
			// If the character sequence starting at the input buffer's current
			// position cannot be mapped to an equivalent byte sequence and the
			// current unmappable-character
			return null;
		} catch (CharacterCodingException e) {
			LOG.warning("Problem in the decoding/encoding charset: " + e.getMessage());
			throw new TranslationException("Problem in the decoding/encoding charset", e);
		}

		return payload;
	}

	/**
	 * The Constructor is private because the class is an helper class and
	 * cannot be instantiated.
	 */
	private HttpTranslator() {

	}

}

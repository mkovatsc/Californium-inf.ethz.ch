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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
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
 * Class that provides the translations from the http representations to the
 * coap representation and viceversa.
 * 
 * @author Francesco Corazza
 */
public final class HttpTranslator {

	private static final String PROPERTIES_FILENAME = "Proxy.properties";

	public static final Properties TRANSLATION_PROPERTIES = new Properties(PROPERTIES_FILENAME);

	protected static final Logger LOG = Logger.getLogger(HttpTranslator.class.getName());

	public static Response getCoapResponse(HttpResponse httpResponse) {
		// the result
		Response coapResponse;

		// get/set the response code
		int httpCode = httpResponse.getStatusLine().getStatusCode();
		String coapCodeString = TRANSLATION_PROPERTIES.getProperty("http.response.code." + httpCode);
		int coapCode = Integer.parseInt(coapCodeString);

		// create the coap reaponse
		coapResponse = new Response(coapCode);

		// get the entity
		HttpEntity httpEntity = httpResponse.getEntity();

		// set the payload
		try {
			// copy the http entity in the payload of the coap
			// request

			// get the inputstream
			// byte[] payload = getContentToByteArray(httpEntity);
			byte[] payload = EntityUtils.toByteArray(httpEntity);
			coapResponse.setPayload(payload);

			// ensure all content has been consumed, so that the
			// underlying connection could be re-used
			EntityUtils.consume(httpEntity);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			LOG.warning("Cannot get the content of the payload");
		}

		// get/set the content-type
		String httpContentType = httpEntity.getContentType().getValue();
		int coapContentType = MediaTypeRegistry.parse(httpContentType);
		// TODO MediaTypeRegistry.contentNegotiation
		coapResponse.setContentType(coapContentType);

		return coapResponse;
	}

	/**
	 * Method to generate an http entity starting from a coap request.
	 * 
	 * @param coapRequest
	 * @return null if the request has no payload
	 */
	public static HttpEntity getHttpEntity(Request coapRequest) {
		// the result
		HttpEntity httpEntity = null;

		// check if coap request has a payload
		byte[] payload = coapRequest.getPayload();
		if (payload != null && payload.length != 0) {
			httpEntity = new ByteArrayEntity(payload);
		}

		return httpEntity;
	}

	public static RequestLine getHttpRequestLine(Request coapRequest, URI httpUri) {
		// get the parameters
		String coapMethod = CodeRegistry.toString(coapRequest.getCode());
		return new BasicRequestLine(coapMethod, httpUri.getPath(), HttpVersion.HTTP_1_1);
	}

	public static HttpResponse getHttpResponse(final Response coapResponse) {
		// the result
		HttpResponse httpResponse;

		// get/set the response code
		int coapCode = coapResponse.getCode();
		String httpCodeString = TRANSLATION_PROPERTIES.getProperty("coap.response.code." + coapCode);
		int httpCode = Integer.parseInt(httpCodeString);

		// create the status line
		StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, httpCode, EnglishReasonPhraseCatalog.INSTANCE.getReason(httpCode, Locale.ENGLISH));

		// create the new http response
		httpResponse = new BasicHttpResponse(statusLine);

		// get/set the content-type
		// TODO MediaTypeRegistry.contentNegotiation
		int coapContentType = coapResponse.getContentType();

		// if the content-type is not set in the coap response and if the
		// response contains an error, the content-type should set to text-plain
		if (coapContentType == MediaTypeRegistry.UNDEFINED && (CodeRegistry.isClientError(coapCode) || CodeRegistry.isServerError(coapCode))) {
			coapContentType = MediaTypeRegistry.TEXT_PLAIN;
		}
		String contentTypeString = MediaTypeRegistry.toString(coapContentType);
		// ContentType contentType = ContentType.create(contentTypeString);
		Header contentTypeHeader = new BasicHeader("content-type", contentTypeString);
		httpResponse.setHeader(contentTypeHeader);

		// create the entity to be filled
		HttpEntity httpEntity = new BasicHttpEntity();
		httpResponse.setEntity(httpEntity);

		return httpResponse;
	}

	/**
	 * @param coapRequest
	 * @return
	 * @throws URISyntaxException
	 */
	public static URI getHttpUri(Request coapRequest) throws URISyntaxException {
		// check params
		if (coapRequest == null) {
			throw new IllegalArgumentException("coapRequest == null");
		}
		if (!coapRequest.isProxyUriSet()) {
			throw new IllegalArgumentException("!coapRequest.isProxyUriSet()");
		}

		// get the proxy-uri
		int proxyUriOptNumber = OptionNumberRegistry.PROXY_URI;
		Option proxyUriOption = coapRequest.getFirstOption(proxyUriOptNumber);
		String proxyUriString = proxyUriOption.getStringValue();

		// create the URI
		URI httpUri = new URI(proxyUriString);
		return httpUri;
	}

	public static void setCoapContentType(HttpEntity httpEntity, ContentType httpContentType, Request coapRequest) {

		// set the content type in the request if it is recognized
		String mimeType = httpContentType.getMimeType();
		int coapContentType = MediaTypeRegistry.parse(mimeType);

		// TODO check additional conversions
		// if (coapContentType == MediaTypeRegistry.UNDEFINED) {
		// contentType = MediaTypeRegistry
		// .checkPossibleConversion(contentTypeString);
		// }

		if (coapContentType != MediaTypeRegistry.UNDEFINED) {
			// set coap content-type
			coapRequest.setContentType(coapContentType);
		} else {
			// TODO exception
			LOG.warning("Conversion not found for the content-type: " + mimeType);
		}
	}

	public static void setCoapOptions(HttpMessage httpMessage, Message coapMessage) {
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

				// get the value of the current header
				String headerValue = header.getValue();
				// delete the last part if any
				headerValue = headerValue.split(";")[0];

				// split the value if it is multi-value
				if (OptionNumberRegistry.isSingleValue(optionNumber)) {
					option.setValue(headerValue.getBytes(Charset.forName("UTF-8")));
					coapMessage.addOption(option);
				} else {
					// iterate over the values of the header
					StringTokenizer stringTokenizer = new StringTokenizer(headerValue, ",");
					while (stringTokenizer.hasMoreTokens()) {
						String nextToken = stringTokenizer.nextToken();
						option.setValue(nextToken.getBytes(Charset.forName("UTF-8")));
						coapMessage.addOption(option);
					}
				}
			}
		}
	}

	public static void setCoapUri(String proxyResource, HttpRequest httpRequest, Request coapRequest, boolean isProxyRequest) {
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
		}

		// if the uri hasn't the indication of the scheme, add it
		if (!uriString.matches("^coap://.*")) {
			uriString = "coap://" + uriString;
		}

		// set the proxy as the sender to receive the response correctly
		coapRequest.setURI(URI.create("localhost"));

		if (isProxyRequest) {
			// set the proxy-uri option to allow the lower layers to underes
			Option proxyUriOption = new Option(uriString, OptionNumberRegistry.PROXY_URI);
			coapRequest.addOption(proxyUriOption);
		} else {
			// set the uri string only as uri-path option
			Option uriPathOption = new Option(uriString, OptionNumberRegistry.URI_PATH);
			coapRequest.setOption(uriPathOption);
		}
	}

	/**
	 * @param httpEntity
	 * @return
	 * @throws IOException
	 */
	private static byte[] getContentToByteArray(HttpEntity httpEntity) throws IOException {
		InputStream inputStream = httpEntity.getContent();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int byteRead;

		while ((byteRead = inputStream.read()) != -1) {
			buffer.write(byteRead);
		}

		buffer.flush();

		inputStream.close();

		return buffer.toByteArray();
	}

	/**
	 * The private Constructor because the class is only an helper class.
	 */
	private HttpTranslator() {

	}
}

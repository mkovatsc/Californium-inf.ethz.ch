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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
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

	public static void entityEnclosed(HttpEntity httpEntity, ContentType httpContentType, Request coapRequest) {

		// set the content type in the request if it is recognized
		int coapContentType = MediaTypeRegistry.parse(httpContentType.getMimeType());

		// TODO
		// check additional conversions
		// if (coapContentType == MediaTypeRegistry.UNDEFINED) {
		// contentType = MediaTypeRegistry
		// .checkPossibleConversion(contentTypeString);
		// }

		if (coapContentType != MediaTypeRegistry.UNDEFINED) {
			// set coap content-type
			coapRequest.setContentType(coapContentType);

			// try {
			// copy the http entity in the payload of the coap
			// request

			// byte[] payload = EntityUtils.toByteArray(httpEntity);
			// coapRequest.setPayload(payload);

			// ensure all content has been consumed, so that the
			// underlying connection could be re-used
			// EntityUtils.consume(httpEntity);
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// LOG.warning("Cannot get the content of the payload");
			// }
		} else {
			// TODO exception
			LOG.warning("Conversion not found for the content-type: " + httpContentType.getMimeType());
		}

	}

	/**
	 * Creates the coap request starting from a httpRequest.
	 * 
	 * @param httpRequest
	 *            the http request
	 * @return the request
	 * @throws HttpException
	 *             the http exception
	 */
	public static Request fillCoapRequest(final HttpRequest httpRequest, Request coapRequest) throws HttpException {
		if (httpRequest == null) {
			throw new IllegalArgumentException("httpRequest == null");
		}
		if (coapRequest == null) {
			throw new IllegalArgumentException("coapRequest == null");
		}

		// requestReceived(httpRequest, coapRequest);

		// get the http entity if present in the http request
		if (httpRequest instanceof HttpEntityEnclosingRequest) {
			HttpEntity httpEntity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();

			if (httpEntity != null) {
				ContentType contentType = ContentType.getOrDefault(httpEntity);
				entityEnclosed(httpEntity, contentType, coapRequest);
			}
		}

		// DEBUG
		coapRequest.prettyPrint();

		return coapRequest;
	}

	/**
	 * Fill http response.
	 * 
	 * @param httpRequest
	 *            the http request
	 * @param httpResponse
	 *            the http response
	 * @param coapResponse
	 *            the coap response
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 */
	public static void fillHttpResponse(HttpResponse httpResponse, final Response coapResponse, boolean head) throws UnsupportedEncodingException {
		if (httpResponse == null) {
			throw new IllegalArgumentException("httpResponse == null");
		}
		if (coapResponse == null) {
			throw new IllegalArgumentException("coapResponse == null");
		}

		// DEBUG
		coapResponse.prettyPrint();

		httpResponse = generateResponse(coapResponse);

		// add the entity to the response if present a payload and if the http
		// method was not HEAD
		HttpEntity httpEntity = null;
		if (coapResponse.getPayload().length != 0 && !head) {
			// get/set the payload as entity
			byte[] payload = coapResponse.getPayload();
			httpEntity = new ByteArrayEntity(payload);
			httpResponse.setEntity(httpEntity);

			// get/set the content-type
			// TODO MediaTypeRegistry.contentNegotiation
			int coapContentType = coapResponse.getContentType();
			String contentType = MediaTypeRegistry.toString(coapContentType);
			Header contentTypeHeader = new BasicHeader("content-type", contentType);
			httpResponse.setHeader(contentTypeHeader);
		} else {
			httpEntity = new StringEntity(""); // FIXME HACK
			httpResponse.setEntity(httpEntity);
		}

		((AbstractHttpEntity) httpEntity).setChunked(true);
		httpResponse.setEntity(httpEntity);
	}

	public static HttpResponse generateResponse(final Response coapResponse) {
		HttpResponse httpResponse;

		// get/set the response code
		int coapCode = coapResponse.getCode();
		String httpCodeString = TRANSLATION_PROPERTIES.getProperty("coap.response.code." + coapCode);
		int httpCode = Integer.parseInt(httpCodeString);

		// create the status line
		StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, httpCode, null);

		// create the new http response
		httpResponse = new BasicHttpResponse(statusLine);

		// get/set the content-type
		// TODO MediaTypeRegistry.contentNegotiation
		int coapContentType = coapResponse.getContentType();
		String contentTypeString = MediaTypeRegistry.toString(coapContentType);
		// ContentType contentType = ContentType.create(contentTypeString);
		Header contentTypeHeader = new BasicHeader("content-type", contentTypeString);
		httpResponse.setHeader(contentTypeHeader);

		// create the entity to be filled
		HttpEntity httpEntity = new BasicHttpEntity();
		httpResponse.setEntity(httpEntity);

		return httpResponse;
	}

	public static void requestReceived(String resource, HttpRequest httpRequest, Request coapRequest) {
		// set the uri
		String uriString = httpRequest.getRequestLine().getUri();
		// TODO check the uri with regexp

		// check if the query string is present
		// if there is no queries, the request is intended for the local http
		// server and consequently forwarded to the proxy endpoint
		// if (uriString.contains("?")) {
		// // get the url in the request
		// uriString = uriString.substring(uriString.indexOf("?") + 1);
		//
		// // add the scheme if not present
		// if (!uriString.contains("coap")) { // TODO better check
		// uriString = "coap://" + uriString;
		// }
		// }

		// get the real requested coap server's uri if the local resource
		// requested is in the path
		// if (uriString.contains(resource)) {
		if (uriString.matches(".?" + resource + ".*")) {
			// find the occurrence of the resource
			int index = uriString.indexOf(resource);
			// delete the slash
			index = uriString.indexOf('/', index);
			uriString = uriString.substring(index + 1);
		}

		// if the uri hasn't the indication of the scheme, add it
		if (!uriString.matches("^coap://.*")) {
			uriString = "coap://" + uriString;
		}

		// URI uri = null;
		// try {
		// uri = new URI(uriString);
		// coapRequest.setURI(uri);
		//
		// // check for the correctness of the uri
		// if (!coapRequest.getPeerAddress().isInitialized()) {
		// throw new URISyntaxException(uriString, "URI malformed"); // TODO
		// }
		// } catch (URISyntaxException e) {
		// LOG.severe("URI malformed: " + e.getMessage());
		// throw new ParseException("URI malformed"); // TODO
		// }

		coapRequest.setURI(URI.create("localhost"));

		int proxyUriOptNumber = OptionNumberRegistry.PROXY_URI;
		Option proxyUriOption = new Option(uriString, proxyUriOptNumber);
		coapRequest.addOption(proxyUriOption);

		// set the headers
		HeaderIterator headerIterator = httpRequest.headerIterator();
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
					coapRequest.addOption(option);
				} else {
					// iterate over the values of the header
					StringTokenizer stringTokenizer = new StringTokenizer(headerValue, ",");
					while (stringTokenizer.hasMoreTokens()) {
						String nextToken = stringTokenizer.nextToken();
						option.setValue(nextToken.getBytes(Charset.forName("UTF-8")));
						coapRequest.addOption(option);
					}
				}
			}
		}
	}

	/**
	 * The private Constructor because the class is only an helper class.
	 */
	private HttpTranslator() {

	}
}

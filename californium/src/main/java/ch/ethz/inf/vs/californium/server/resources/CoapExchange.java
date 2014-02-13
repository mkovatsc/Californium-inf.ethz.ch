/*******************************************************************************
 * Copyright (c) 2014, Institute for Pervasive Computing, ETH Zurich.
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
package ch.ethz.inf.vs.californium.server.resources;

import java.net.InetAddress;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.OptionSet;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;

/**
 * The Class CoapExchange represents an exchange of a CoAP request and response
 * and provides a user-friendly API to subclasses of {@link ResourceBase} for
 * responding to requests.
 */
public class CoapExchange {
	
	/* The internal (advanced) exchange. */
	private Exchange exchange;
	
	/* The destination resource. */
	private ResourceBase resource;
	
	/* Response option values. */
	private String locationPath = null;
	private String locationQuery = null;
	private long maxAge = 60;
	private byte[] eTag = null;
	
	/**
	 * Constructs a new CoAP Exchange object representing the specified exchange
	 * and Resource.
	 * 
	 * @param exchange the exchange
	 * @param resource the resource
	 */
	protected CoapExchange(Exchange exchange, ResourceBase resource) {
		if (exchange == null) throw new NullPointerException();
		if (resource == null) throw new NullPointerException();
		this.exchange = exchange;
		this.resource = resource;
	}
	
	/**
	 * Gets the source address of the request.
	 *
	 * @return the source address
	 */
	public InetAddress getSourceAddress() {
		return exchange.getRequest().getSource();
	}
	
	/**
	 * Gets the source port of the request.
	 *
	 * @return the source port
	 */
	public int getSourcePort() {
		return exchange.getRequest().getSourcePort();
	}
	
	/**
	 * Gets the request code: <tt>GET</tt>, <tt>POST</tt>, <tt>PUT</tt> or
	 * <tt>DELETE</tt>.
	 * 
	 * @return the request code
	 */
	public Code getRequestCode() {
		return exchange.getRequest().getCode();
	}
	
	/**
	 * Gets the request's options.
	 *
	 * @return the request options
	 */
	public OptionSet getRequestOptions() {
		return exchange.getRequest().getOptions();
	}
	
	/**
	 * Gets the request payload as byte array.
	 *
	 * @return the request payload
	 */
	public byte[] getRequestPayload() {
		return exchange.getRequest().getPayload();
	}
	
	/**
	 * Gets the request payload as string.
	 *
	 * @return the request payload string
	 */
	public String getRequestText() {
		return exchange.getRequest().getPayloadString();
	}
	
	/**
	 * Accept the exchange, i.e. send an acknowledgment to the client that the
	 * exchange has arrived and a separate message is being computed and sent
	 * soon. Call this method on an exchange if the computation of a response
	 * might take some time and might trigger a timeout at the client.
	 */
	public void accept() {
		exchange.sendAccept();
	}
	
	/**
	 * Reject the exchange if it is impossible to be processed, e.g. if it
	 * carries an unknown critical option. In most cases, it is better to
	 * respond with an error response code to bad requests though.
	 */
	public void reject() {
		exchange.sendReject();
	}
	
	/**
	 * Set the Location-Path for the response.
	 */
	public void setLocationPath(String path) {
		locationPath = path;
	}
	
	/**
	 * Set the Location-Query for the response.
	 */
	public void setLocationQuery(String query) {
		locationQuery = query;
	}
	
	/**
	 * Set the Max-Age for the response body.
	 */
	public void setMaxAge(long age) {
		maxAge = age;
	}

	/**
	 * Set the ETag for the response.
	 */
	public void setETag(byte[] tag) {
		eTag = tag;
	}
	
	/**
	 * Respond the specified response code and no payload.
	 *
	 * @param code the code
	 */
	public void respond(ResponseCode code) {
		respond(new Response(code));
	}
	
	/**
	 * Respond with response code 2.05 (Content) and the specified payload.
	 * 
	 * @param payload the payload as string
	 */
	public void respond(String payload) {
		respond(ResponseCode.CONTENT, payload);
	}
	
	/**
	 * Respond with the specified response code and the specified payload.
	 *
	 * @param code the response code
	 * @param payload the payload
	 */
	public void respond(ResponseCode code, String payload) {
		Response response = new Response(code);
		response.setPayload(payload);
		response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
		respond(response);
	}
	
	/**
	 * Respond with the specified response code and the specified payload.
	 *
	 * @param code the response code
	 * @param payload the payload
	 */
	public void respond(ResponseCode code, byte[] payload) {
		Response response = new Response(code);
		response.setPayload(payload);
		respond(response);
	}

	/**
	 * Respond with the specified response code and the specified payload.
	 *
	 * @param code the response code
	 * @param payload the payload
	 * @param contentFormat the Content-Format of the payload
	 */
	public void respond(ResponseCode code, byte[] payload, int contentFormat) {
		Response response = new Response(code);
		response.setPayload(payload);
		response.getOptions().setContentFormat(contentFormat);
		respond(response);
	}
	
	/**
	 * Respond with the specified response code and the specified payload.
	 *
	 * @param code the response code
	 * @param payload the payload
	 * @param contentFormat the Content-Format of the payload
	 */
	public void respond(ResponseCode code, String payload, int contentFormat) {
		Response response = new Response(code);
		response.setPayload(payload);
		response.getOptions().setContentFormat(contentFormat);
		respond(response);
	}
	
	/**
	 * Respond with the specified response.
	 *
	 * @param response the response
	 */
	public void respond(Response response) {
		if (response == null) throw new NullPointerException();
		
		// set the response options configured through the CoapExchange API
		if (locationPath != null) response.getOptions().setLocationPath(locationPath);
		if (locationQuery != null) response.getOptions().setLocationQuery(locationQuery);
		if (maxAge != 60) response.getOptions().setMaxAge(maxAge);
		if (eTag != null) {
			response.getOptions().clearETags();
			response.getOptions().addETag(eTag);
		}
		
		resource.checkObserveRelation(exchange, response);
		
		exchange.sendResponse(response);
	}
	
	/**
	 * Provides access to the internal Exchange object.
	 * 
	 * @return the Exchange object
	 */
	public Exchange advanced() {
		return exchange;
	}
}

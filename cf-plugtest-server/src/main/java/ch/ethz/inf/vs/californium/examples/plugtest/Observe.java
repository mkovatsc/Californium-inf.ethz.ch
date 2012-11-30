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
package ch.ethz.inf.vs.californium.examples.plugtest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Message.messageType;
import ch.ethz.inf.vs.californium.coap.ObservingManager;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;

/**
 * This resource implements a test of specification for the
 * ETSI IoT CoAP Plugtests, Paris, France, 24 - 25 March 2012.
 * 
 * @author Matthias Kovatsch
 */
public class Observe extends LocalResource {

	// Members ////////////////////////////////////////////////////////////////

	private byte[] data = null;
	private int dataCt = MediaTypeRegistry.TEXT_PLAIN;
	private boolean wasUpdated = false;
	private boolean wasDeleted = false;

	// The current time represented as string
	private String time;

	/*
	 * Constructor for a new TimeResource
	 */
	public Observe() {
		super("obs");
		setTitle("Observable resource which changes every 5 seconds");
		setResourceType("observe");
		isObservable(true);

		// Set timer task scheduling
		Timer timer = new Timer();
		timer.schedule(new TimeTask(), 0, 5000);
	}

	/*
	 * Defines a new timer task to return the current time
	 */
	private class TimeTask extends TimerTask {

		@Override
		public void run() {
			time = getTime();

			// Call changed to notify subscribers
			changed();
		}
	}

	/*
	 * Returns the current time
	 * 
	 * @return The current time
	 */
	private String getTime() {
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Date time = new Date();
		return dateFormat.format(time);
	}

	@Override
	public void performGET(GETRequest request) {

		if (wasDeleted) {
			Response response = new Response(CodeRegistry.RESP_NOT_FOUND);
			if (wasUpdated) {
				// TD_COAP_OBS_08
				response.setCode(CodeRegistry.RESP_INTERNAL_SERVER_ERROR);
			}
			response.setType(messageType.CON);
			request.respond(response);

		} else {
			Response response = new Response(CodeRegistry.RESP_CONTENT);
			if (wasUpdated) {
				response.setPayload(data);
				wasUpdated = false;
			} else {
				response.setPayload(time);
			}
			// TODO first response must be ACK, all others CON
			// response.setType(messageType.CON);
			response.setContentType(dataCt);
			response.setMaxAge(5);

			// complete the request
			request.respond(response);
		}

	}
	
	@Override
	public void performPUT(PUTRequest request) {

		if (request.getContentType() == MediaTypeRegistry.UNDEFINED) {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Content-Type not set");
			return;
		}
		
		// store payload
		storeData(request);

		// complete the request
		request.respond(CodeRegistry.RESP_CHANGED);
	}

	@Override
	public void performDELETE(DELETERequest request) {
		wasUpdated = false;
		wasDeleted = true;
		
		ObservingManager.getInstance().removeObservers(this);
		
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	@Override
	public void performPOST(POSTRequest request) {
		wasUpdated = true;
		wasDeleted = true;
		
		ObservingManager.getInstance().removeObservers(this);
		
		
		request.respond(CodeRegistry.RESP_CHANGED);
	}

	// Internal ////////////////////////////////////////////////////////////////
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private synchronized void storeData(Request request) {

		if (request.getContentType() != dataCt) {

			wasDeleted = true;
			wasUpdated = true;
			
			// signal that resource state changed
			changed();
			
			ObservingManager.getInstance().removeObservers(this);
		}
		
		// set payload and content type
		data = request.getPayload();
		dataCt = request.getContentType();
		clearAttribute(LinkFormat.CONTENT_TYPE);
		setContentTypeCode(dataCt);

		
		// signal that resource state changed
		changed();
	}
}

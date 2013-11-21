/*******************************************************************************
 * Copyright (c) 2013, Institute for Pervasive Computing, ETH Zurich.
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

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the
 * ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class ObserveNon extends ResourceBase {

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
	public ObserveNon() {
		super("obs-non");
		setObservable(true);
		getAttributes().setTitle("Observable resource which changes every 5 seconds");
		getAttributes().addResourceType("observe");
		getAttributes().setObservable();

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
	public void handleGET(Exchange exchange) {
		if (wasDeleted) {
			Response response = new Response(ResponseCode.NOT_FOUND);
			if (wasUpdated) {
				// TD_COAP_OBS_08
				response = new Response(ResponseCode.INTERNAL_SERVER_ERROR);
			}
			response.setType(Type.CON);
			exchange.respond(response);

		} else {
			Response response = new Response(ResponseCode.CONTENT);
			if (wasUpdated) {
				response.setPayload(data);
				wasUpdated = false;
			} else {
				response.setPayload(time);
			}

			response.getOptions().setContentFormat(dataCt);
			response.getOptions().setMaxAge(5);
			response.setType(Type.NON);

			// complete the request
			respond(exchange, response);
		}

	}
	
	@Override
	public void handlePUT(Exchange exchange) {
		Request request = exchange.getRequest();

		if (!request.getOptions().hasContentFormat()) {
			exchange.respond(ResponseCode.BAD_REQUEST, "Content-Format not set");
			return;
		}
		
		// store payload
		storeData(request);

		// complete the request
		exchange.respond(ResponseCode.CHANGED);
	}

	@Override
	public void handleDELETE(Exchange exchange) {
		wasUpdated = false;
		wasDeleted = true;
		
//		ObservingManager.getInstance().removeObservers(this);
		clearAndNotifyObserveRelations();
		
		wasDeleted = false;
		exchange.respond(ResponseCode.DELETED);
	}
	

	// Internal ////////////////////////////////////////////////////////////////
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private synchronized void storeData(Request request) {

		wasUpdated = true;
		
		if (request.getOptions().getContentFormat() != dataCt) {

			wasDeleted = true;
			
			// signal that resource state changed
			changed();
			
			clearAndNotifyObserveRelations();

			wasDeleted = false;
		}
		
		// set payload and content type
		data = request.getPayload();

		getAttributes().clearContentType();
		getAttributes().addContentType(dataCt);
		
		// signal that resource state changed
		changed();
	}
}

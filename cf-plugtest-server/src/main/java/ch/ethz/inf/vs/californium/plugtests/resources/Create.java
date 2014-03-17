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
package ch.ethz.inf.vs.californium.plugtests.resources;

import static ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode.*;
import static ch.ethz.inf.vs.californium.coap.MediaTypeRegistry.*;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class Create extends ResourceBase {

	// Members ////////////////////////////////////////////////////////////////

	private byte[] data = null;
	private int dataCf = UNDEFINED;

	public Create() {
		super("create1");
		getAttributes().setTitle("Resource which does not exist yet (to perform atomic PUT)");
		setVisible(false);
	}
	
	@Override
	public void handlePUT(CoapExchange exchange) {
		if (data!=null && exchange.getRequestOptions().hasIfNoneMatch()) {
			exchange.respond(PRECONDITION_FAILED);
			
			// automatically reset
			data = null;
		} else {
			if (exchange.getRequestOptions().hasContentFormat()) {
				storeData(exchange.getRequestPayload(), exchange.getRequestOptions().getContentFormat());
				exchange.respond(CREATED);
			} else {
				exchange.respond(BAD_REQUEST, "Content-Format not set");
			}
		}
	}
	
	@Override
	public void handleGET(CoapExchange exchange) {
		if (data!=null) {
			exchange.respond(CONTENT, data, dataCf);
		} else {
			exchange.respond(NOT_FOUND);
		}
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		data = null;
		setVisible(false);
		exchange.respond(DELETED);
	}
	
	// Internal ////////////////////////////////////////////////////////////////
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private synchronized void storeData(byte[] payload, int cf) {

		// set payload and content type
		data = payload;
		dataCf = cf;
		getAttributes().clearContentType();
		getAttributes().addContentType(dataCf);
		getAttributes().setMaximumSizeEstimate(data.length);
		
		setVisible(true);

		// signal that resource state changed
		changed();
	}
}

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

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.Exchange;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource implements a test of specification for the ETSI IoT CoAP Plugtests, Las Vegas, NV, USA, 19 - 22 Nov 2013.
 * 
 * @author Matthias Kovatsch
 */
public class Create extends ResourceBase {

	// Members ////////////////////////////////////////////////////////////////

	private byte[] data = null;
	private int dataCt = MediaTypeRegistry.UNDEFINED;

	public Create() {
		super("create1");
		getAttributes().setTitle("Resource which does not exist yet (to perform atomic PUT)");
		setVisible(false);
	}
	
	@Override
	public void handlePUT(Exchange exchange) {
		if (data!=null && exchange.getRequest().getOptions().hasIfNoneMatch()) {
			exchange.respond(new Response(ResponseCode.PRECONDITION_FAILED));
			
			// automatically reset
			data = null;
		} else {
			if (exchange.getRequest().getOptions().hasContentFormat()) {
				storeData(exchange.getRequest());
				exchange.respond(ResponseCode.CREATED);
			} else {
				exchange.respond(ResponseCode.BAD_REQUEST, "Content-Format not set");
			}
		}
	}
	
	@Override
	public void handleGET(Exchange exchange) {
		if (data!=null) {
			Response response = new Response(ResponseCode.CONTENT);
			response.setPayload(data);
			response.getOptions().setContentFormat(dataCt);
			exchange.respond(response);
		} else {
			exchange.respond(ResponseCode.NOT_FOUND);
		}
	}

	@Override
	public void handleDELETE(Exchange exchange) {
		data = null;
		setVisible(false);
		exchange.respond(ResponseCode.DELETED);
	}
	
	// Internal ////////////////////////////////////////////////////////////////
	
	/*
	 * Convenience function to store data contained in a 
	 * PUT/POST-Request. Notifies observing endpoints about
	 * the change of its contents.
	 */
	private synchronized void storeData(Request request) {

		// set payload and content type
		data = request.getPayload();
		dataCt = request.getOptions().getContentFormat();
		getAttributes().clearContentType();
		getAttributes().addContentType(dataCt);
		getAttributes().setMaximumSizeEstimate(data.length);
		
		setVisible(true);

		// signal that resource state changed
		changed();
	}
}

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
package ch.ethz.inf.vs.persistingservice.resources.persisting;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.HistoryResource;

// TODO: Auto-generated Javadoc
/**
 * The Class RunningResource keeps track of the running status of the task.
 * When the task is running, it collects data from the source device and stores it in the database.
 */
public class RunningResource extends LocalResource {

	/** The observing resource. */
	private ObservingResource observingResource;
	
	/** The history resource. */
	private HistoryResource historyResource;
	
	/** The device. */
	private String device;
	
	/** The running. */
	private boolean running;
	
	/** The get options. */
	private List<Option> getOptions;

	/**
	 * Instantiates a new running resource.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param device the device
	 * @param running the running
	 * @param getOptions the get options
	 */
	public RunningResource(String resourceIdentifier, String device, boolean running, List<Option> getOptions) {
		super(resourceIdentifier);
		this.device = device;
		this.running = running;
		this.getOptions = getOptions;
	}
	
	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * Setup references creates the references to access the observing resource and history resource.
	 *
	 * @param observingResource the observing resource
	 * @param historyResource the history resource
	 */
	public void setupReferences(ObservingResource observingResource, HistoryResource historyResource) {
		this.observingResource = observingResource;
		this.historyResource = historyResource;
	}
	
	/**
	 * performGET responds with the running status:
	 * 
	 * true:	running
	 * false: 	not running
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET RUNNING: get running for device " + device);
		request.prettyPrint();
		
		request.respond(CodeRegistry.RESP_CONTENT, Boolean.toString(running));
	}
	
	/**
	 * performPUT changes the running status
	 * 
	 * payload:
	 * true:				start running
	 * false:				stop running
	 * false;withstorage:	stop running but retrieve data one more time
	 */
	public void performPUT(PUTRequest request) {
		System.out.println("CHANGE RUNNING: change the running status for device " + device);
		request.prettyPrint();
		
		String payload = request.getPayloadString();
		if (payload.equals("true")) {
			if (!running) {
				running = true;
				while(!observingResource.isSet()) {
					try {
						Thread.sleep(1000); // sleep until for the observer mechanism was checked
					} catch (InterruptedException e) {
						System.err.println("Exception: " + e.getMessage());
					}
				}
				historyResource.startHistory(observingResource.isObserving(), getOptions);
			}
			request.respond(CodeRegistry.RESP_CHANGED);
		} else if (payload.startsWith("false")) {
			if (running) {
				running = false;
				while(!observingResource.isSet()) {
					try {
						Thread.sleep(1000); // sleep until for the observer mechanism was checked
					} catch (InterruptedException e) {
						System.err.println("Exception: " + e.getMessage());
					}
				}
				if (payload.contains(";") && payload.substring(payload.indexOf(';')+1).equals("withstorage")) {
					historyResource.stopHistory(observingResource.isObserving(), getOptions, true);
				} else {
					historyResource.stopHistory(observingResource.isObserving(), getOptions, false);
				}
			}
			request.respond(CodeRegistry.RESP_CHANGED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
		}
	}
	
	/**
	 * Stop running stops the task from collecting data and storing it in the database.
	 */
	public void stopRunning() {
		historyResource.stopHistory(observingResource.isObserving(), getOptions, false);
	}
}

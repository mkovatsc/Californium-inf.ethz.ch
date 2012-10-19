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

import java.io.IOException;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.persistingservice.database.documents.DefaultStorage;
import ch.ethz.inf.vs.persistingservice.resources.PersistingServiceResource;
import ch.ethz.inf.vs.persistingservice.resources.TasksResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.AbstractValueSet;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.HistoryResource;

/**
 * The Class PersistingResource holds all the subresources for a single task.
 * 
 *	/persistingresource
 *		/type
 *		/running
 *		/observing
 *		/devicepath
 *		/deviceinfo
 *		/options
 *		/history
 */
public class PersistingResource extends LocalResource {
	
	/** The tasks resource. */
	private TasksResource tasksResource;
	
	/** The top resource. */
	private String topResource;
	
	/** The running resource. */
	private RunningResource runningResource;
	
	/** The observing resource. */
	private ObservingResource observingResource;
	
	/** The type resource. */
	private TypeResource typeResource;
	
	/** The history resource. */
	private HistoryResource historyResource;
	
	/** The device path resource. */
	private DevicePathResource devicePathResource;
	
	/** The device info resource. */
//	private DeviceInfoResource deviceInfoResource;
	
	/** The options resource. */
	private OptionsResource optionsResource;
	
	/** The options. */
	private List<Option> options;
	
	/** The resource identifier. */
	private String resourceIdentifier;
	
	private String devRoot;
	
	private String devResource;

	/**
	 * Instantiates a new persisting resource.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param type the type
	 * @param deviceROOT the device root
	 * @param deviceRES the device res
	 * @param options the options
	 * @param tasksResource the tasks resource
	 * @param topResource the top resource
	 */
	public PersistingResource(String resourceIdentifier, String type, String deviceROOT, String deviceRES, List<Option> options, TasksResource tasksResource, String topResource) {
		super(resourceIdentifier);
		this.resourceIdentifier = resourceIdentifier;
		this.options = options;
		this.tasksResource = tasksResource;
		this.topResource = topResource;
		this.devRoot=deviceROOT;
		this.devResource=deviceRES;
		
		add(typeResource = new TypeResource("type", deviceROOT + deviceRES, type));
		add(runningResource = new RunningResource("running", deviceROOT + deviceRES, false, options));
		add(observingResource = new ObservingResource("observing", false, deviceROOT + deviceRES, options));
		add(devicePathResource = new DevicePathResource("devicepath", deviceROOT + deviceRES));
		//add(deviceInfoResource = new DeviceInfoResource("deviceinfo", deviceROOT + deviceRES, deviceROOT));
		add(optionsResource = new OptionsResource("options", deviceROOT + deviceRES, options));
		
		String optionsString = "";
		if (options!=null) {
			optionsString = "?"+options.get(0);
			for (int i=1;i<options.size();i++) {
				optionsString += "&"+options.get(i);
			}
		}
		
		// add the history resouce depending on the type
		if (type.equals("number")) {
			/**
			 * The class NumberValueSet implements a method to set the value for the couchdb document whenever the type is number.
			 */
			class NumberValueSet extends AbstractValueSet {
				public boolean perform(DefaultStorage defaultStorage, String payload) {
					try {
						defaultStorage.setValue(Float.parseFloat(payload));
						return true;
					} catch (NumberFormatException e) {
						System.err.println("Exception: " + e.getMessage());
						return false;
					}
				}
			}
			
			add(historyResource = new HistoryResource<Float>("history", topResource, optionsString, type, deviceROOT, deviceRES, new NumberValueSet(), true));
		} else if (type.equals("string")) {
			/**
			 * The class StringValueSet implements a method to set the value for the couchdb document whenever the type is string.
			 */
			class StringValueSet extends AbstractValueSet {
				public boolean perform(DefaultStorage defaultStorage, String payload) {
					defaultStorage.setValue(payload);
					return true;
				}
			}
			
			add(historyResource = new HistoryResource<String>("history", topResource, optionsString, type, deviceROOT, deviceRES, new StringValueSet(), false));
		}
		
		runningResource.setupReferences(observingResource, historyResource);
		observingResource.setupReferences(historyResource);
	}
	
	// Requests //////////////////////////////////////////////
	
	/**
	 * performDELETE removes the persisting resource from the resource tree. The task is stopped. If this was the last sub resource for some top resource, the top resource is removed as well.
	 */
	public void performDELETE(DELETERequest request) {
		System.out.println("DELETE PERSISTINGRESOURCE: persisting resource " + resourceIdentifier + "is being deleted");
		request.prettyPrint();
		
		if (runningResource.isRunning()) {
			runningResource.stopRunning();
		}
		
		remove();
		tasksResource.cleanUp(topResource);
		request.respond(CodeRegistry.RESP_DELETED);
	}

	public String getDevResource() {
		return devResource;
	}

	public String getDevRoot() {
		return devRoot;
	}
}

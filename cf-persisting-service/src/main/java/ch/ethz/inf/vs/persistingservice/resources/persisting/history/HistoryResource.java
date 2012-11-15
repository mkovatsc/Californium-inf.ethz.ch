/*******************************************************************************
0 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
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
package ch.ethz.inf.vs.persistingservice.resources.persisting.history;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.persistingservice.config.DateFormats;
import ch.ethz.inf.vs.persistingservice.database.DatabaseConnection;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.*;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.*;


// TODO: Auto-generated Javadoc
/**
 * The Class HistoryResource is used to request data from a specified device and store it in the database.
 * <p>
 * If possible the history resource registers as observer on the device
 * resource, otherwise the history polls data periodically.
 * <p>
 * The data is stored in the following format:<br>
 * device = DEVICE_PATH<br>
 * value = VALUE_OF_DATA<br>
 * dateTime = yyyy/MM/dd-HH/mm/ss
 * <p>
 * The subresources to retrieve data from the database are:<br>
 * -	<b>newest</b>: returns only the value of the newest document stored for the target
 * device<br>
 * -	<b>all</b>: returns the values of all documents stored for the target device<br>
 * (-	<b>all/sum</b>: returns the sum of all documents stored for the target device)<br>
 * (-	<b>all/avg</b>: returns the average of all documents stored for the target device)<br>
 * (-	<b>all/max</b>: returns the maximum of all documents stored for the target device)<br>
 * (-	<b>all/min</b>: returns the minimum of all documents stored for the target device)<br>
 * -	<b>last</b>: returns the values of the last *limit* documents. <br>
 * (-	<b>last/sum</b>: returns the sum of the last x documents stored for the target device)<br>
 * (-	<b>last/avg</b>: returns the average of the last x documents stored for the target device)<br>
 * (-	<b>last/max</b>: returns the maximum of the last x documents stored for the target device)<br>
 * (-	<b>last/min</b>: returns the minimum of the last x documents stored for the target device)<br>
 * -	<b>since</b>: returns the values of all documents stored since *date* for the target
 * device<br>
 * (-	<b>since/sum</b>: returns the sum of all documents stored since *date* for the target device)<br>
 * (-	<b>since/avg</b>: returns the average of all documents stored since *date* for the target device)<br>
 * (-	<b>since/max</b>: returns the maximum of all documents stored since *date* for the target device)<br>
 * (-	<b>since/min</b>: returns the minimum of all documents stored since *date* for the target device)<br>
 * -	<b>onday</b>: returns the values of all documents stored on *date* for the target
 * device<br>
 * (-	<b>onday/sum</b>: returns the sum of all documents stored on *date* for the target device)<br>
 * (-	<b>onday/avg</b>: returns the average of all documents stored on *date* for the target device)<br>
 * (-	<b>onday/max</b>: returns the maximum of all documents stored on *date* for the target device)<br>
 * (-	<b>onday/min</b>: returns the minimum of all documents stored on *date* for the target device)<br>
 * -	<b>timerange</b>: returns the values of all documents stored between *startdate* and
 * *enddate* for the target device<br>
 * (-	<b>timerange/sum</b>: returns the sum of all documents stored between *startdate* and *enddate* for the target device)<br>
 * (-	<b>timerange/avg</b>: returns the average of all documents stored between *startdate* and *enddate* for the target device)<br>
 * (-	<b>timerange/max</b>: returns the maximum of all documents stored between *startdate* and *enddate* for the target device)<br>
 * (-	<b>timerange/min</b>: returns the minimum of all documents stored between *startdate* and *enddate* for the target device)<br>
 *
 * @param <T> the generic type
 */
public class HistoryResource<T extends Comparable> extends LocalResource{
	
	/** The all resource. */
	private AllResource allResource;
	
	/** The newest resource. */
	private NewestResource newestResource;
	
	/** The last resource. */
	private LastResource lastResource;
	
	/** The since resource. */
	private SinceResource sinceResource;
	
	/** The onday resource. */
	private OnDayResource ondayResource;
	
	/** The timerange resource. */
	private TimeRangeResource timerangeResource;
	
	/** The resource identifier. */
	private String resourceIdentifier;
	
	/** The type repository. */
	private DatabaseRepository<T> typeRepository;

	/** The type. */
	private String type;
	
	/** The device root. */
	private String deviceROOT;
	
	/** The device res. */
	private String deviceRES;
	
	/** The device. */
	private String device;
	
	/** The device id. */
	private String deviceID;
	
	/** The abstract set value. */
	private AbstractValueSet abstractSetValue;
	
	/** The observing handler. */
	private ObservingHandler observingHandler;
	
	/** The observing request. */
	private Request observingRequest;
	
	/** The timer. */
	private Timer timer;
	
	/**
	 * Instantiates a new history resource for the device deviceROOT +
	 * deviceRES and adds the required subresources. A unique key for each device
	 * is created: topresource;;deviceROOT+deviceRES+optionsString
	 * <p>
	 * A private string type repository is created to connect to the database
	 * and store data or query data from it.
	 * <p>
	 * The history resource tries to register as observer on the device.
	 * Otherwise it starts a polling task to retrieve the data from the device
	 * periodically.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param topResource the top resource for the task
	 * @param optionsString the options string
	 * @param type the type
	 * @param deviceROOT the device root
	 * @param deviceRES the device res
	 * @param abstractSetValue the abstract set value is an object containing a method to set the value for the document to store in the database
	 * @param withSubResources the with sub resources
	 */
	@SuppressWarnings("unchecked")
	public HistoryResource(String resourceIdentifier, String topResource, String optionsString, String type, String deviceROOT, String deviceRES, AbstractValueSet abstractSetValue, boolean withSubResources) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a Number value");
		setResourceType("numbertype");
				
		this.resourceIdentifier = resourceIdentifier;
		this.type = type;
		this.deviceROOT = deviceROOT;
		this.deviceRES = deviceRES;
		this.device = deviceROOT + deviceRES;
		
		// build the unique key for the device
		if (topResource.isEmpty() && optionsString.isEmpty())
			this.deviceID = this.device;
		else if (optionsString.isEmpty())
			this.deviceID = topResource + ";;" + this.device;
		else if (topResource.isEmpty())
			this.deviceID = this.device + optionsString;
		else
			this.deviceID = topResource + ";;" + this.device + optionsString;
		
		this.abstractSetValue = abstractSetValue;

		// create a new database respository to retrieve and store data in the database
		typeRepository = new DatabaseRepository<T>((Class<DefaultStorage<T>>) ((Class<? extends DefaultStorage<T>>) DefaultStorage.class), DatabaseConnection.getCouchDbConnector(), this.deviceID);

		add((allResource = new AllResource<T>("all", type, typeRepository, device, withSubResources)));
		add((newestResource = new NewestResource("newest", device)));
		add((lastResource = new LastResource<T>("last", type, typeRepository, device, withSubResources)));
		add((sinceResource = new SinceResource<T>("since", type, typeRepository, device, withSubResources)));
		add((ondayResource = new OnDayResource<T>("onday", type, typeRepository, device, withSubResources)));
		add((timerangeResource = new TimeRangeResource<T>("timerange", type, typeRepository, device, withSubResources)));
		
		List<Default> res = typeRepository.queryDeviceLimit(1, type);
		if (!res.isEmpty())
			newestResource.notifyChanged(res.get(0).getValue(), res.get(0).getDateTime());
	}
	
	/**
	 * Gets the device specified for this specific number resource.
	 *
	 * @return the device
	 */
	public String getDevice() {
		return this.device;
	}
	
	/**
	 * Start history starts collecting data from the source device.
	 * <p>
	 * If the source device is observable, it registers as observer.
	 * Otherwise it starts a polling task, which periodically requests data fromt the source device.
	 *
	 * @param observing the observing
	 * @param options the options
	 */
	public void startHistory(boolean observing, List<Option> options) {
		if (observing) {
			System.out.println("START OBSERVING: device " + device + " is being observed.");
			observingRequest = new GETRequest();
			observingRequest.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
			if (options != null)
				observingRequest.setOptions(options);
			observingHandler = new ObservingHandler();
			observingRequest.registerResponseHandler(observingHandler);
			observingRequest.setURI(device);
			
			try {
				observingRequest.execute();
			} catch (IOException e) {
				System.err.println("Exception: " + e.getMessage());
			}
		} else {
			System.out.println("START POLLING: device " + device + " is being polled.");
			timer = new Timer();
			timer.schedule(new PollingTask(options), 0, 120000);
		}
	}
	
	/**
	 * Stop history stopps collecting data from the source device.
	 * <p>
	 * It unregisters as observer, or stopps the polling task.
	 *
	 * @param observing the observing
	 * @param options the options
	 * @param withStorage the with storage tells the task to one more time request data from the source device and store it in the database.
	 */
	public void stopHistory(boolean observing, List<Option> options, boolean withStorage) {
		if (observing) {
			System.out.println("STOP OBSERVING: device " + device + " was being observed.");
			observingRequest.unregisterResponseHandler(observingHandler);
			
			observingRequest = new GETRequest();
			if (options != null)
				observingRequest.setOptions(options);
			if (withStorage)
				observingRequest.registerResponseHandler(new LastResponseHandler());
			observingRequest.setURI(device);
			
			try {
				observingRequest.execute();
			} catch (IOException e) {
				System.err.println("Exception: " + e.getMessage());
			}
		} else {
			System.out.println("STOP POLLING: device " + device + " is being polled.");
			if (timer != null)
				timer.cancel();
		}
	}
	
	/**
	 * The Class LastResponseHandler stores the retrieved data one last time in the database before stopping the collection mechanism.
	 */
	public class LastResponseHandler implements ResponseHandler {

		/**
		 * Handles the response and stores the data in the database. Then
		 * notifies the subresources, that new data was received.
		 *
		 * @param response the response
		 */
		@Override
		public void handleResponse(Response response) {
			String payload = response.getPayloadString();
			System.out.println("LAST RESPONSE: last data (value: " + payload + ") was fetched for " + device + " before turning it off");
			
			// store in database
			DefaultStorage<T> storageType = new DefaultStorage<T>();
			storageType.setDevice(deviceID);
			abstractSetValue.perform(storageType, payload);
			DateFormat dateFormat = new SimpleDateFormat(DateFormats.DATE_FORMAT);
	        Date date = new Date();
			storageType.setDateTime(dateFormat.format(date));
	
			typeRepository.add(storageType);
			System.out.println("DATABASE: data (value: " + payload + ") was stored for device " + device);
			
			notifyChanged(payload, dateFormat.format(date));
			System.out.println("PUSH NOTIFICATION: notify resources after new data was stored for device " + device);
			
			// response.getRequest().unregisterResponseHandler(this);
		}
		
	}
	
	/**
	 * Notify subresources that new data was received from the device and the
	 * database has changed.
	 * 
	 * @param value
	 *            the new value retrieved from the device
	 * @param date
	 *            the current date
	 */
	private void notifyChanged(String value, String date) {
		allResource.notifyChanged(value);
		newestResource.notifyChanged(value, date);
		lastResource.notifyChanged(value);
		sinceResource.notifyChanged(value, date);
		ondayResource.notifyChanged(value, date);
		timerangeResource.notifyChanged(value, date);
	}
		
	// Handler/ ///////////////////////////////////////////////////////////////
	
	/**
	 * The Class ObservingHandler handles the response when the history
	 * resource registered as observer and stores the data in the database.
	 */
	public class ObservingHandler implements ResponseHandler {

		/**
		 * Handles the response and stores the data in the database. Then
		 * notifies the subresources, that new data was received.
		 *
		 * @param response the response
		 */
		@Override
		public void handleResponse(Response response) {
			String payload = response.getPayloadString().trim();
			System.out.println("OBSERVING: new data (value: " + payload + ") was being pushed from device " + device);
			
			// store in database
			DefaultStorage<T> storageType = new DefaultStorage<T>();
			DateFormat dateFormat = new SimpleDateFormat(DateFormats.DATE_FORMAT);
		    Date date = new Date();
			
		    if (abstractSetValue.perform(storageType, payload)) {
				storageType.setDevice(deviceID);
				storageType.setDateTime(dateFormat.format(date));
		
				typeRepository.add(storageType);
				System.out.println("DATABASE: data (value: " + payload + ") was stored for device " + device);
			} else {
				System.out.println("DATA ERROR: data (value: " + payload + ") could not be stored for device " + device);
			}
			
			notifyChanged(payload, dateFormat.format(date));
			System.out.println("PUSH NOTIFICATION: notify resources after new data was stored for device " + device);
		}
		
	}
	
	// Polling Task ///////////////////////////////////////////////////////////
	
	/**
	 * The Class PollingTask periodically retrieves data from the source device
	 * and stores it in the database.
	 */
	public class PollingTask extends TimerTask {

		/** The options. */
		private List<Option> options;
		/** The old value. */
		private String oldValue;
		
		/**
		 * Instantiates a new polling task with the options to request data.
		 *
		 * @param options the options
		 */
		public PollingTask(List<Option> options) {
			this.oldValue = "";
			this.options = options;
		}
		
		/**
		 * run performs a get request on the source device and reads the
		 * response value. If the value has changed, it stores it in the
		 * database and notifies the subresources.
		 */
		@Override
		public void run() {
			Request getRequest = new GETRequest();
			getRequest.setURI(device);
			if (options != null)
				getRequest.setOptions(options);
			getRequest.enableResponseQueue(true);
			
			try {
				getRequest.execute();
			} catch (IOException e) {
				System.err.println("Exception: " + e.getMessage());
			}

			String payload = null;
			
			try {
				Response response = getRequest.receiveResponse();
				payload = response.getPayloadString().trim();
			} catch (InterruptedException e) {
				System.err.println("Exception: " + e.getMessage());
			}
			System.out.println("POLLING: data (value: " + payload + ") is being polled from device " + device);
			
			if (!oldValue.equals(payload)) {
				oldValue = payload;
				
				DefaultStorage<T> storageType = new DefaultStorage<T>();
				DateFormat dateFormat = new SimpleDateFormat(DateFormats.DATE_FORMAT);
			    Date date = new Date();
			    
				if (abstractSetValue.perform(storageType, payload)) {
					storageType.setDevice(deviceID);
					storageType.setDateTime(dateFormat.format(date));
					
					typeRepository.add(storageType);
					System.out.println("DATABASE: data (value: " + payload + ") was stored for device " + device);
				} else {
					System.out.println("DATA ERROR: data (value: " + payload + ") could not be stored for device " + device);
				}
				
				notifyChanged(payload, dateFormat.format(date));
				System.out.println("PUSH NOTIFICATION: notify resources after new data was stored for device " + device);
			}
		}
	}

}
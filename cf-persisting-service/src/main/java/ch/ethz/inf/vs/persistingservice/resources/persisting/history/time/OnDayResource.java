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
package ch.ethz.inf.vs.persistingservice.resources.persisting.history.time;

import java.util.Collections;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.persistingservice.config.DateFormats;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.Default;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.AvgResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MaxResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MinResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.SumResource;

// TODO: Auto-generated Javadoc
/**
 * The Class OnDayResource is observable and can be used to retrieve all documents for a source device.
 * <p>
 * Depending on the data type, additional subresources are added to retrieve aggregated values from the database.
 * 
 * Resource Tree:
 *	/all
 *	|...(/avg)
 *	|...(/max)
 *	|...(/min)
 *	|...(/sum)
 *
 * @param <T> the generic type
 */
public class OnDayResource<T extends Comparable<T>> extends AbstractTimeResource {

	/** The sum resource. */
	private SumResource<T> sumResource;		
	
	/** The avg resource. */
	private AvgResource<T> avgResource;		
	
	/** The max resource. */
	private MaxResource<T> maxResource;
	
	/** The min resource. */
	private MinResource<T> minResource;
	
	/** The date. */
	private String date;
	
	/** The type. */
	private String type;
	
	/** The type repository. */
	private DatabaseRepository<T> typeRepository;
	
	/** The device. */
	private String device;
	
	/** The with sub resources. */
	private boolean withSubResources;
	
	/**
	 * Instantiates a new on day resource and makes it observable. All
	 * subresources are added.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param type the type
	 * @param typeRepository the type repository
	 * @param device the device
	 * @param withSubResources the with sub resources
	 */
	public OnDayResource(String resourceIdentifier, String type, DatabaseRepository<T> typeRepository, String device, boolean withSubResources) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.typeRepository = typeRepository;
		this.device = device;
		this.withSubResources = withSubResources;
		
		this.date = "";
		
		if (withSubResources) {
			add((sumResource = new SumResource<T>("sum", type, typeRepository, device, this)));
			add((avgResource = new AvgResource<T>("avg", type, typeRepository, device, this)));
			add((maxResource = new MaxResource<T>("max", type, typeRepository, device, this)));
			add((minResource = new MinResource<T>("min", type, typeRepository, device, this)));
		}
	}
	
	/**
	 * perform GET queries the database for all documents of this device
	 * stored on some day and responds with their values.
	 * <p>
	 * Payload:<br>
	 * date = yyyy/MM/dd-HH:mm:ss
	 *
	 * @param request the request
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET ONDAY: get request for device " + device);
		request.prettyPrint();

		acceptGetRequest(request, new OnDayQuery());
	}
	
	/**
	 * Accept get request reacts to the get request.
	 * 
	 * @param request
	 * 			the request is the get request received.
	 * @param query
	 * 			the query is a container for a method, which defines the mechanism to retrieve data from the database. 
	 */
	public void acceptGetRequest(GETRequest request, AbstractQuery query) {
		List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
		OptionParser parsedOptions = new OptionParser(options);
		
		String ret = "";

		if (parsedOptions.containsLabel("date")) {
			this.date = parsedOptions.getStringValue("date");
			String startOnDay = this.date + "-00:00:00";
			String endOnDay = this.date + "-23:59:59";
			
			ret += query.perform(parsedOptions, AbstractQuery.ONDAY, startOnDay, endOnDay);
			
			System.out.println("GETRequst ONDAY: (value: " + ret.substring(0, Math.min(50, ret.length())) + ") for device " + device);
			request.respond(CodeRegistry.RESP_CONTENT, ret);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "date = " + DateFormats.DATE_FORMAT_DAY);
		}
	}
	
	/**
	 * The Class OnDayQuery accesses the database and returns the retrieved data.
	 */
	private class OnDayQuery extends AbstractQuery {
		
		/**
		 * perform retrieves the data from the database depending on the parent resource and returns it.
		 * <p>
		 * The values can also be retrieved with the date they where stored.
		 *
		 * @param parsedOptions the parsed options
		 * @param timeResID the time res id
		 * @param params the params
		 * @return the string
		 */
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			List<Default> resOnDay = typeRepository.queryDeviceRange(params[0], params[1], type);
			Collections.reverse(resOnDay);
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				for (Default nt : resOnDay) {
					ret += nt.getDateTime() + ";" +nt.getValue() + "\n";
				}
			} else {
				for (Default nt : resOnDay) {
					ret += nt.getValue() + "\n";
				}
			}
			return ret;
		}
	}
	
	/**
	 * Notify changed and pass the notification to the subresources.
	 *
	 * @param value the newest value
	 * @param date the current date
	 */
	public void notifyChanged(String value, String date) {
		if (date.equals(this.date)) {
			changed();
			
			if (withSubResources) {
				sumResource.notifyChanged(value);
				avgResource.notifyChanged(value);
				maxResource.notifyChanged(value);
				minResource.notifyChanged(value);
			}
		}
	}
	
}

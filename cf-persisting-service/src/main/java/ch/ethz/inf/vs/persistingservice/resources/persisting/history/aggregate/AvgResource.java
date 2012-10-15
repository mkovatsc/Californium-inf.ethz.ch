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
package ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef.DEFAULT;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.persistingservice.config.DateFormats;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.Avg;
import ch.ethz.inf.vs.persistingservice.database.documents.DateAvg;
import ch.ethz.inf.vs.persistingservice.database.documents.Default;
import ch.ethz.inf.vs.persistingservice.database.documents.Sum;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractQuery;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractTimeResource;

/**
 * The Class AvgResource is a resource to retrieve the avg of a collection of documents belonging to a source device.
 *
 * @param <T> the generic type of the data
 */
public class AvgResource<T extends Comparable> extends LocalResource {
	
	private String type;
	private DatabaseRepository<T> databaseRepository;
	private String device;
	private AbstractTimeResource abstractTimeResource;
	
	/** The avg. */
	private String avg;

	/**
	 * Instantiates a new avg resource and makes it observable.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param type the type
	 * @param databaseRepository the type repository
	 * @param device the device
	 * @param abstractTimeResource the abstract time resource is the parent resource of this resource
	 */
	public AvgResource(String resourceIdentifier, String type, DatabaseRepository<T> databaseRepository, String device, AbstractTimeResource abstractTimeResource) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.databaseRepository = databaseRepository;
		this.device = device;
		this.abstractTimeResource = abstractTimeResource;
		
		avg = "0";
	}
	
	/**
	 * performGET responds with the avg.
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET ALL AVG: get request for device " + device);
		request.prettyPrint();

		abstractTimeResource.acceptGetRequest(request, new AvgQuery());
	}
	
	/**
	 * The Class AvgQuery accesses the database and returns the retrieved data.
	 * <p>
	 * The data retrieval depends on the type of parent resource.
	 */
	private class AvgQuery extends AbstractQuery {
		
		/**
		 * perform retrieves the data from the database depending on the parent resource and returns it.
		 * <p>
		 * The average can also be retrieved in a weighted form, where the values are weighted by the time they were valid.
		 * The last value is considered to be valid at only one millisecond. This makes sense, when the user observes the average.
		 *
		 * @param parsedOptions the parsed options
		 * @param timeResID the time res id is used to decide, which avg has to be retrieved from the database.
		 * @param params the params
		 * @return the string is the data retrieved from the database
		 */
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			
			if(parsedOptions.containsLabel("weighted") && parsedOptions.getBooleanValue("weighted")) {
				switch(timeResID) {
					case NEWEST:
					case ALL:
						List<Default> resAvg = databaseRepository.queryDevice(type);
						ret = "" + computeWeigthedAvg(resAvg);
						break;
					case SINCE:
						List<Default> resSinceAvg = databaseRepository.queryDeviceSince(params[0], type);
						ret = "" + computeWeigthedAvg(resSinceAvg);
						break;
					case ONDAY:
					case TIMERANGE:
						List<Default> resDateAvg = databaseRepository.queryDeviceRange(params[0], params[1], type);
						ret = "" + computeWeigthedAvg(resDateAvg);
						break;
					case LAST:
						List<Default> resLimit = databaseRepository.queryDeviceLimit(Integer.parseInt(params[0]), type);
						ret = "" + computeWeigthedAvg(resLimit);
						break;
					default:
						ret = "UNKNOWN TIMERESID";
				}
			} else {
				switch(timeResID) {
					case NEWEST:
					case ALL:
						List<Avg> resAvg = databaseRepository.queryDeviceAvg(type);
						if (!resAvg.isEmpty())
							ret = "" + resAvg.get(0).getAvg();
						break;
					case SINCE:
						List<DateAvg> resSinceAvg = databaseRepository.queryDeviceSinceAvg(params[0], type);
						if (!resSinceAvg.isEmpty())
							ret = "" + resSinceAvg.get(0).getAvg();
						break;
					case ONDAY:
					case TIMERANGE:
						List<DateAvg> resDateAvg = databaseRepository.queryDeviceRangeAvg(params[0], params[1], type);
						if (!resDateAvg.isEmpty())
							ret = "" + resDateAvg.get(0).getAvg();
						break;
					case LAST:
						List<Default> resLimit = databaseRepository.queryDeviceLimit(Integer.parseInt(params[0]), type);
						float sum = 0;
						if (!resLimit.isEmpty()) {
							for (Default nt : resLimit) {
								sum += nt.getNumberValue();
							}
						}
						ret += ""+(sum/resLimit.size());
						break;
					default:
						ret = "UNKNOWN TIMERESID";
				}
			}
			
			System.out.println("GETRequst SUM: (value: " + ret + ") for device " + device);
			
			return ret;
		}
		
		/**
		 * Compute weighted average computes the average of a list of values, while weighting each value with the time it was valid.
		 * 
		 * @param res the res is the list of all values to compute the weighted average from.
		 * @return the weighted average.
		 */
		private String computeWeigthedAvg(List<Default> res) {
			String ret = "";
			
			if (!res.isEmpty()) {
				long first = stringToMillis(res.get(0).getDateTime());;
				long time_1 = first;
				long time_2 = 0;
				float sum = res.get(0).getNumberValue();
				System.out.println("SUM: " + sum);
				for (int i=1; i<res.size(); i++) {
					time_2 = stringToMillis(res.get(i).getDateTime());
					sum += res.get(i).getNumberValue() * (time_2 - time_1);
					System.out.println("SUM: " + sum);
					time_1 = time_2;
				}
				ret += (sum / (time_1 - first + 1));
				System.out.println("RET: " + ret);
			}
			return ret;
		}
		
		/**
		 * String to millis translates a date string into the corresponding milliseconds.
		 * 
		 * @param date 
		 * 			the date is the date in string form.
		 * @return the date in milliseconds.
		 */
		private long stringToMillis(String date) {
			Calendar cal = Calendar.getInstance();
			DateFormat dateFormat = new SimpleDateFormat(DateFormats.DATE_FORMAT);
			try {
				cal.setTime(dateFormat.parse(date));
			} catch (ParseException e) {
				System.err.println("Exception: " + e.getMessage());
			}
			return cal.getTimeInMillis();
		}
	}
	
	/**
	 * Notify changed notifies the observers, that the database has changed.
	 *
	 * @param value the value
	 */
	public void notifyChanged(String value) {
		if (!value.equals(avg))
			changed();
	}
	
}

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
package ch.ethz.inf.vs.californium.coap;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.Message.messageType;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.californium.layers.TransactionLayer;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * The TokenManager stores all tokens currently used in transfers. New transfers
 * can acquire unique tokens from the manager.
 * 
 * @author Matthias Kovatsch
 */
public class ObservingManager {

// Logging /////////////////////////////////////////////////////////////////////
	
	private static final Logger LOG = Logger.getLogger(ObservingManager.class.getName());
	

// Inner class /////////////////////////////////////////////////////////////////
	
	private class ObservingRelationship {
		public String clientID;
		public String resourcePath;
		public GETRequest request;
		public int lastMID;
		
		public ObservingRelationship(GETRequest request) {
			
			request.setMID(-1);
			
			this.clientID = request.getPeerAddress().toString();
			this.resourcePath = request.getUriPath();
			this.request = request;
			this.lastMID = -1;
		}
	}
	
// Static Attributes ///////////////////////////////////////////////////////////
	
	private static ObservingManager singleton = new ObservingManager();

// Members /////////////////////////////////////////////////////////////////////

	/** Maps a resource path string to the resource's observers stored by client address string. */
	private Map<String, Map<String, ObservingRelationship>> observersByResource = new HashMap<String, Map<String, ObservingRelationship>>();
	
	/** Maps a peer address string to the clients relationships stored by resource path. */
	private Map<String, Map<String, ObservingRelationship>> observersByClient = new HashMap<String, Map<String, ObservingRelationship>>();
	
	private int checkInterval = Properties.std.getInt("OBSERVING_REFRESH_INTERVAL");
	private Map<String, Integer> intervalByResource = new HashMap<String, Integer>();
	
// Constructors ////////////////////////////////////////////////////////////////
	
	/**
	 * Default singleton constructor.
	 */
	private ObservingManager() {
	}
	
	public static ObservingManager getInstance() {
		return singleton;
	}
	
// Methods /////////////////////////////////////////////////////////////////////
	
	public void setRefreshInterval(int interval) {
		this.checkInterval = interval;
	}
	
	public void notifyObservers(LocalResource resource) {

		Map<String, ObservingRelationship> resourceObservers = observersByResource.get(resource.getPath());
		
		if (resourceObservers!=null && resourceObservers.size()>0) {
			
			LOG.info(String.format("Notifying observers: %d @ %s", resourceObservers.size(), resource.getPath()));
			
			int check = -1;
			
			// get/initialize
			if (!intervalByResource.containsKey(resource.getPath())) {
				check = checkInterval;
			} else {
				check = intervalByResource.get(resource.getPath()) - 1;
			}
			// update
			if (check <= 0) {
				intervalByResource.put(resource.getPath(), checkInterval);
				LOG.info(String.format("Refreshing observing relationship: %s", resource.getPath()));
			} else {
				intervalByResource.put(resource.getPath(), check);
			}
			
			for (ObservingRelationship observer : resourceObservers.values()) {
				
				GETRequest request = observer.request;
						
				// check
				if (check<=0) {
					request.setType(messageType.CON);
				} else {
					request.setType(messageType.NON);
				}
				
				// execute
				resource.performGET(request);
				prepareResponse(request);
				request.sendResponse();
			}
		}
	}
	
	
	private void prepareResponse(Request request) {

		// consecutive response require new MID that must be stored for RST matching
		if (request.getResponse().getMID()==-1) {
			request.getResponse().setMID(TransactionLayer.nextMessageID());
		}
		
		// 16-bit second counter
		int secs = (int) ((System.currentTimeMillis() - request.startTime) / 1000) & 0xFFFF;
		request.getResponse().setOption(new Option(secs, OptionNumberRegistry.OBSERVE));
		
		// store MID for RST matching
		updateLastMID(request.getPeerAddress().toString(), request.getUriPath(), request.getResponse().getMID());
	}
	
	
	public synchronized void addObserver(GETRequest request, LocalResource resource) {
		
		ObservingRelationship toAdd = new ObservingRelationship(request);
		
		// get clients map for the given resource path
		Map<String, ObservingRelationship> resourceObservers = observersByResource.get(resource.getPath());
		if (resourceObservers==null) {
			// lazy creation
			resourceObservers = new HashMap<String, ObservingRelationship>();
			observersByResource.put(resource.getPath(), resourceObservers);
		}
		// get resource map for given client address
		Map<String, ObservingRelationship> clientObservees = observersByClient.get(request.getPeerAddress().toString());
		if (clientObservees==null) {
			// lazy creation
			clientObservees = new HashMap<String, ObservingRelationship>();
			observersByClient.put(request.getPeerAddress().toString(), clientObservees);
		}
		
		// save relationship for notifications triggered by resource
		resourceObservers.put(request.getPeerAddress().toString(), toAdd);
		// save relationship for actions triggered by client
		clientObservees.put(resource.getPath(), toAdd);
		
		LOG.info(String.format("Established observing relationship: %s @ %s", request.getPeerAddress().toString(), resource.getPath()));
		
		// update response
		request.getResponse().setOption(new Option(0, OptionNumberRegistry.OBSERVE));
		
	}
	
	public synchronized void removeObserver(String clientID) {

		Map<String, ObservingRelationship> clientObservees = observersByClient.get(clientID);
		
		if (clientObservees!=null) {
			
			for (Map<String, ObservingRelationship> entry : observersByResource.values()) {
				entry.remove(clientID);
			}
			observersByClient.remove(clientID);
			
			LOG.info(String.format("Terminated all observing relationships for client: %s", clientID));
			
		}
	}

	/**
	 * Remove an observer by missing Observe option in GET.
	 * 
	 * @param clientID the peer address as string
	 * @param resource the resource to un-observe.
	 */
	public void removeObserver(String clientID, LocalResource resource) {
		
		Map<String, ObservingRelationship> resourceObservers = observersByResource.get(resource.getPath());
		Map<String, ObservingRelationship> clientObservees = observersByClient.get(clientID);
		
		if (resourceObservers!=null && clientObservees!=null) {
			if (resourceObservers.remove(clientID)!=null && clientObservees.remove(resource.getPath())!=null) {
				LOG.info(String.format("Terminated observing relationship by GET: %s @ %s", clientID, resource.getPath()));
				return;
			}
		}
		
		// should not be called if not existent
		LOG.warning(String.format("Cannot find observing relationship: %s @ %s", clientID, resource.getPath()));
	}
	
	/**
	 * Remove an observer by MID from RST.
	 * 
	 * @param clientID the peer address as string
	 * @param mid the MID from the RST
	 */
	public void removeObserver(String clientID, int mid) {
		
		ObservingRelationship toRemove = null;

		Map<String, ObservingRelationship> clientObservees = observersByClient.get(clientID);
		
		if (clientObservees!=null) {
			for (ObservingRelationship entry : clientObservees.values()) {
				if (mid==entry.lastMID && clientID.equals(entry.clientID)) {
					// found it
					toRemove = entry;
					break;
				}
			}
		}
		
		if (toRemove!=null) {
			Map<String, ObservingRelationship> resourceObservers = observersByResource.get(toRemove.resourcePath);
			
			// FIXME Inconsistent state check
			if (resourceObservers==null) {
				LOG.severe(String.format("FIXME: ObservingManager has clientObservee, but no resourceObservers (%s @ %s)", clientID, toRemove.resourcePath));
			}
			
			if (resourceObservers.remove(clientID)!=null && clientObservees.remove(toRemove.resourcePath)!=null) {
				LOG.info(String.format("Terminated observing relationship by RST: %s @ %s", clientID, toRemove.resourcePath));
				return;
			}
		}
		
		LOG.warning(String.format("Cannot find observing relationship by MID: %s|%d", clientID, mid));
	}

	public boolean isObserved(String clientID, LocalResource resource) {
		return observersByClient.containsKey(clientID) &&
				observersByClient.get(clientID).containsKey(resource.getPath());
	}

	public void updateLastMID(String clientID, String path, int mid) {
		
		Map<String, ObservingRelationship> clientObservees = observersByClient.get(clientID);
		
		if (clientObservees!=null) {
			ObservingRelationship toUpdate = clientObservees.get(path);
			if (toUpdate!=null) {
				toUpdate.lastMID = mid;
				
				LOG.finer(String.format("Updated last MID for observing relationship: %s @ %s", clientID, toUpdate.resourcePath));
				return;
			}
		}
		
		LOG.warning(String.format("Cannot find observing relationship to update MID: %s @ %s", clientID, path));
	}
}

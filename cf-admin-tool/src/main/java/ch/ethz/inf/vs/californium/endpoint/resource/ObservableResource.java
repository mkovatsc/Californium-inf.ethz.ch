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
package ch.ethz.inf.vs.californium.endpoint.resource;

import java.io.IOException;
import java.util.Date;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Message.messageType;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;
import ch.ethz.inf.vs.californium.util.Properties;

/**
 * This resource registers itself as an observer on the specified uri,
 * the resource is observable
 * It is used to handle more observers on limited devices
 * 
 * 
 * @author Nico Eigenmann
 */
public class ObservableResource extends LocalResource {

	// The current time represented as string
	private String ep;
	private String path;
	private Response lastResponse;
	private GETRequest observeRequest;
	private ResponseHandler observeHandler;
	private ResponseHandler psPostHandler;
	private ResponseHandler psPutHandler;
	private ObservableNodeResource parent;
	private boolean persistingCreated;
	private boolean persistingRunning;
	private int observeNrLast;
	private Date lastHeardOf;
		
	/*
	 * Constructor for a new ObservableResource
	 */
	public ObservableResource(String identifier, String uri, ObservableNodeResource par) {
		super(identifier);
		
		
		
		isObservable(true);
		
		ep = par.getName();
		path = identifier;
		
		parent = par;
		persistingCreated = false;
		persistingRunning = false;
		
		lastHeardOf = new Date(0);
		
		observeRequest = new GETRequest();
		observeRequest.setURI(uri);
		observeRequest.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
		observeRequest.setToken(TokenManager.getInstance().acquireToken());
		observeHandler = new ObserveReceiver();
		psPostHandler =  new PSRequestReceiver();
		psPutHandler = new PSRunReceiver();
		
		observeRequest.registerResponseHandler(observeHandler);
		
		try {
			observeRequest.execute();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}

	
	/*
	 * Defines a new timer task to return the current time
	 */
	private class ObserveReceiver implements ResponseHandler {
		
				
		public ObserveReceiver(){
			super();
			
		}

		@Override
		public void handleResponse(Response response){
			if(response.getType()==messageType.RST || response.getCode()==CodeRegistry.RESP_NOT_FOUND ||
					response.getCode()==CodeRegistry.RESP_METHOD_NOT_ALLOWED ){
				remove();
				return;
			}
			if(!response.getOptions(OptionNumberRegistry.OBSERVE).isEmpty()){
				
				if (!response.isAcknowledgement() && observeNrLast<0){
					observeNrLast = response.getFirstOption(OptionNumberRegistry.OBSERVE).getIntValue();
					parent.receivedActualAdd(-1);
				}
				int observeNrNew = response.getFirstOption(OptionNumberRegistry.OBSERVE).getIntValue();
				parent.setLastHeardOf();
				parent.receivedActualAdd(1);
				parent.receivedIdealAdd(observeNrNew - observeNrLast);
				observeNrLast = observeNrNew;
				parent.setLastHeardOf();
				lastHeardOf =  new Date();
				
				if (parent.hasPersisting() && !persistingCreated){
					POSTRequest psRequest = new POSTRequest();
					psRequest.setURI(parent.getPsUri());
					psRequest.registerResponseHandler(psPostHandler);
					String payload;
					payload = "topid="+ep+"\n" +
							"resid="+path+"\n" +
							"deviceroot=coap://localhost:"+Properties.std.getInt("DEFAULT_PORT")+"/observable\n"+
							"deviceres=/"+ep+"/"+path+"\n" +
							"type=string";
										
					psRequest.setPayload(payload);
					try {
						psRequest.execute();
					
					} catch (IOException e) {
						LOG.severe("PersistingService Registration failed: "+ep+path);
					}
					
				}
				if(persistingCreated && !persistingRunning){
					PUTRequest psRunRequest = new PUTRequest();
					psRunRequest.setURI(parent.getPsUri()+"/"+ep+"/"+path+"/running");
					psRunRequest.setPayload("true");
					psRunRequest.registerResponseHandler(psPutHandler);
					try {
						psRunRequest.execute();
					} catch (IOException e) {
						LOG.severe("PersistingService Running failed: "+ep+path);
						e.printStackTrace();
					}
				}
				
			}
			lastResponse = response;
			changed();
			
			
		}
		
		
	}

	private class PSRequestReceiver implements ResponseHandler {
	
	
		public PSRequestReceiver() {
			super();
		}
		
		@Override
		public void handleResponse(Response response){
			
			if(response.getCode()==CodeRegistry.RESP_CREATED || response.getCode()==CodeRegistry.RESP_VALID){
				persistingCreated=true;
				LOG.finest("PersistingService Registration successful: "+ep+path);
				PUTRequest psRunRequest = new PUTRequest();
				psRunRequest.setURI(parent.getPsUri()+"/"+ep+"/"+path+"/running");
				psRunRequest.setPayload("true");
				psRunRequest.registerResponseHandler(new PSRunReceiver());
				try {
					psRunRequest.execute();
				} catch (IOException e) {
					LOG.severe("PersistingService Running failed: "+ep+path);
					e.printStackTrace();
				}
			}
			else{
				persistingCreated=true;
				LOG.severe("PersistingService Registration failed: "+ep+path);
			}
			
		}
	}
	
	private class PSRunReceiver implements ResponseHandler{

		@Override
		public void handleResponse(Response response) {
			if(response.getCode()==CodeRegistry.RESP_CHANGED){
				persistingRunning=true;
				LOG.finest("PersistingService Running successful: "+ep+path);
			}
			else{
				LOG.severe("PersistingService Running failed: "+ep+path);
			}
			
		}
		
		
	}
	

	public void resendObserveRegistration(boolean force){
		if((lastHeardOf.getTime()< new Date().getTime()-1800*1000) || force){
			if (parent.getLastHeardOf().getTime() < new Date().getTime()-24*3600*1000){
				parent.resetLastHeardOf();
				parent.resetLossRate();				
			}
			observeNrLast = -1;
			try {
				observeRequest.execute();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}
	
	
	@Override
	public void performDELETE(DELETERequest request){
		
		//Trying To unregister once (send Get Request without Observe Option)
		GETRequest unRequest = new GETRequest();
		unRequest.setURI("coap://"+parent.getContext()+"/"+getName());
		unRequest.setType(Message.messageType.NON);
		unRequest.setToken(observeRequest.getToken());
		unRequest.enableResponseQueue(true);
		try {
			unRequest.execute();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		remove();
		
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	

	@Override
	public void performGET(GETRequest request) {

		// create response
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		if(lastResponse!=null){
			response.addOptions(lastResponse.getOptions());
			response.setPayload(lastResponse.getPayload());
		}

		// complete the request
		request.respond(response);
	}
	
	public String getEp(){
		return ep;
	}
	
	public String getLastPayload(){
		if (lastResponse!=null){
			return lastResponse.getPayloadString();
		}
		return "";
	}

	
}

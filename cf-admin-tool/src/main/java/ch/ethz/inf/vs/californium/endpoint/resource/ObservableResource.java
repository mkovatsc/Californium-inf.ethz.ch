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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Message.messageType;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
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
	private int observeCount;
	private int observeNrLast;
	private int observeNrStart;
	private Date observeLast;
	private ObserveTopResource parent;
	
	/*
	 * Constructor for a new ObservableResource
	 */
	public ObservableResource(String identifier, String uri, ObserveTopResource top) {
		super(identifier);
		
		observeCount = 0;
		observeNrStart = -1;
		
		lastResponse = null;
		
		ep = identifier.substring(0, identifier.indexOf("/"));
		path = identifier.substring(identifier.indexOf("/"));
		
		isObservable(true);
		
		parent = top;
		
		observeRequest = new GETRequest();
		observeRequest.setURI(uri);
		observeRequest.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
		
		observeHandler = new ObserveReceiver();
		
		observeRequest.registerResponseHandler(observeHandler);
		
		try {
			observeRequest.execute();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		observeLast = null;
		
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
				observeCount++;
				observeNrLast = response.getFirstOption(OptionNumberRegistry.OBSERVE).getIntValue();

				if (observeNrStart < 0 && !response.isAcknowledgement()){
					observeNrStart = observeNrLast;
					observeCount = 0;
				}
				if (observeNrStart < 0){ 
					if (parent.hasPersisting()){
						POSTRequest psRequest = new POSTRequest();
						Response psResponse = null;
						psRequest.setURI(parent.getPsUri());
						psRequest.enableResponseQueue(true);
						String payload;
						payload = "topid="+ep+"\n" +
								"deviceroot=coap://localhost:"+Properties.std.getInt("DEFAULT_PORT")+"/observable\n"+
										"resid="+path+"\n" +
										"deviceres=/"+ep+path+"\n" +
										"type=";
						List<LinkAttribute> attribs = ObservableResource.this.getAttributes(LinkFormat.RESOURCE_TYPE);
						boolean isNumber = true;
						for(LinkAttribute attr : attribs){
							if(attr.getStringValue().toLowerCase().contains("string") || attr.getStringValue().toLowerCase().contains("text")){
								isNumber=false;
								break;
							}
						}
						if(path.contains("debug")){
							isNumber=false;
						}
						if(isNumber){
							payload+="number";
						}
						else{
							payload+="string";
						}
						
						psRequest.setPayload(payload);
						try {
							psRequest.execute();
							psResponse = psRequest.receiveResponse();
						} catch (IOException e) {
							LOG.severe("PersistingService Registration failed: "+ep+path);
						} catch (InterruptedException e) {
							LOG.severe("PersistingService Registration failed: "+ep+path);
						}
						if(psResponse != null && psResponse.getCode()==CodeRegistry.RESP_CREATED){
							LOG.finest("PersistingService Registration successful: "+ep+path);
							PUTRequest psRunRequest = new PUTRequest();
							Response psRunResponse = null;
							psRunRequest.setURI(parent.getPsUri()+"/"+ep+"/"+path+"/running");
							psRunRequest.enableResponseQueue(true);
							psRunRequest.setPayload("true");
							try {
								psRunRequest.execute();
								psRunResponse = psRunRequest.receiveResponse();
							} catch (IOException e) {
								LOG.severe("PersistingService Running failed: "+ep+path);
								e.printStackTrace();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								LOG.severe("PersistingService Running failed: "+ep+path);
							}
							if(psRunResponse != null && psRunResponse.getCode()==CodeRegistry.RESP_CONTENT){
								LOG.finest("PersistingService Running successful: "+ep+path);
							
							}
							else{
								LOG.severe("PersistingService Running failed: "+ep+path);
							}
						}
						else{
							LOG.severe("PersistingService Registration failed: "+ep+path);
						}
					}
				}
				
			}
			observeLast = new Date();
			lastResponse = response;
			changed();
			
			
		}
		
		
	}
	

	public void resendObserveRegistration(){
		if(observeLast.getTime()<(new Date().getTime()-25*3600*1000)){			
			observeCount = 0;
			observeNrStart = -1;
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
		unRequest.setURI("coap://"+ep+"/"+getName());
		unRequest.setType(Message.messageType.NON);
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
			response.setContentType(lastResponse.getContentType());
			response.setPayload(lastResponse.getPayload());
		}

		// complete the request
		request.respond(response);
	}
	
	public int getPacketsReceivedActual(){
		return observeCount;
	}
	
	public int getPacketsReceivedIdeal(){
		return (observeNrStart < 0) ? 0 :observeNrLast-observeNrStart+1;
	
	}
	
	public String getEp(){
		return ep;
	}
	
	public Date getLastHeardOf(){
		return observeLast;
	}
	
	public String getLastPayload(){
		if (lastResponse!=null){
			return lastResponse.getPayloadString();
		}
		return "";
	}
	
}

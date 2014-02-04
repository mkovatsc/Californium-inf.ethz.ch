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
package ch.ethz.inf.vs.californium.examples.resources;

import java.util.Arrays;
import java.util.LinkedList;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.server.resources.CoapExchange;
import ch.ethz.inf.vs.californium.server.resources.Resource;
import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

/**
 * This resource allows to store an arbitrary payload in any subresource. If the
 * target subresource does not yet exist it will be created. Therefore, such a
 * resource can be though off as having all possible children.
 * <p>
 * <ul>
 * <li />A GET request receives the currently stored data within the target
 * resource.
 * <li />A POST request creates the specified resources from the payload.
 * <li />A PUT request stores the payload within the target resource.
 * <li />A DELETE request deletes the target resource.
 * </ul>
 * <p>
 * Assume a single instance of this resource called "storage". Assume a client
 * sends a PUT request with Payload "foo" to the URI storage/A/B/C. When the
 * resource storage receives the request, it creates the resources A, B and C
 * and delivers the request to the resource C. Resource C will process the PUT
 * request and stare "foo". If the client sends a consecutive GET request to the
 * URI storage/A/B/C, resource C will respond with the payload "foo".
 * 
 * @author Martin Lanter
 */
public class StorageResource extends ResourceBase {

	private String content;
	
	public StorageResource(String name) {
		super(name);
	}
	
	@Override
	public void handleGET(CoapExchange exchange) {
		if (content != null) {
			exchange.respond(content);
		} else {
			String subtree = LinkFormat.serializeTree(this);
			exchange.respond(ResponseCode.CONTENT, subtree, MediaTypeRegistry.APPLICATION_LINK_FORMAT);
		}
	}

	@Override
	public void handlePOST(CoapExchange exchange) {
		String payload = exchange.getRequestText();
		String[] parts = payload.split("\\?");
		String[] path = parts[0].split("/");
		Resource resource = create(new LinkedList<String>(Arrays.asList(path)));
		
		Response response = new Response(ResponseCode.CREATED);
		response.getOptions().setLocationPath(resource.getURI());
		exchange.respond(response);
	}

	@Override
	public void handlePUT(CoapExchange exchange) {
		content = exchange.getRequestText();
		exchange.respond(ResponseCode.CHANGED);
	}

	@Override
	public void handleDELETE(CoapExchange exchange) {
		this.delete();
		exchange.respond(ResponseCode.DELETED);
	}

	/**
	 * Find the requested child. If the child does not exist yet, create it.
	 */
	@Override
	public Resource getChild(String name) {
		Resource resource = super.getChild(name);
		if (resource == null) {
			resource = new StorageResource(name);
			add(resource);
		}
		return resource;
	}
	
	/**
	 * Create a resource hierarchy with according to the specified path.
	 * @param path the path
	 * @return the lowest resource from the hierarchy
	 */
	private Resource create(LinkedList<String> path) {
		String segment;
		do {
			if (path.size() == 0)
				return this;
		
			segment = path.removeFirst();
		} while (segment.isEmpty() || segment.equals("/"));
		
		StorageResource resource = new StorageResource(segment);
		add(resource);
		return resource.create(path);
	}

}

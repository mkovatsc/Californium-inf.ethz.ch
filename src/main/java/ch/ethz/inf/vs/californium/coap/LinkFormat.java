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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import ch.ethz.inf.vs.californium.endpoint.RemoteResource;
import ch.ethz.inf.vs.californium.endpoint.Resource;

/**
 * This class provides link format definitions as specified in
 * draft-ietf-core-link-format-06
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class LinkFormat {

// Logging /////////////////////////////////////////////////////////////////////
	
	protected static final Logger LOG = Logger.getLogger(LinkFormat.class.getName());

// Constants ///////////////////////////////////////////////////////////////////

	public static final String RESOURCE_TYPE         = "rt";
	public static final String INTERFACE_DESCRIPTION = "if";
	public static final String CONTENT_TYPE          = "ct";
	public static final String MAX_SIZE_ESTIMATE     = "sz";
	public static final String TITLE                 = "title";
	public static final String OBSERVABLE            = "obs";

	public static final Pattern DELIMITER            = Pattern.compile("\\s*,+\\s*"); // generous parsing

// Serialization ///////////////////////////////////////////////////////////////
	
	public static String serialize(Resource resource, List<Option> query, boolean recursive) {
	
		StringBuilder linkFormat = new StringBuilder();
		
		// skip hidden and empty root in recursive mode, always skip non-matching resources
		if ((!resource.isHidden() && (!resource.getName().equals("")) || !recursive) && matches(resource, query)) {
			
			LOG.finer("Serializing resource link: " + resource.getPath());
			
			linkFormat.append("<");
			linkFormat.append(resource.getPath());
			linkFormat.append(">");
			
			for (LinkAttribute attrib : resource.getAttributes()) {
				linkFormat.append(';');
				linkFormat.append(attrib.serialize());
			}
		}
		
		if (recursive) {
			// Loop over all sub-resources
			for (Resource sub : resource.getSubResources()) {

				String next = LinkFormat.serialize(sub, query, true);
				
				// delimiter
				if (!next.equals("")) {
					if (linkFormat.length()>3) linkFormat.append(',');
					linkFormat.append(next);
				}
			}
		}
		
		return linkFormat.toString();
	}

	/**
	 * This method creates a {@link RemoteResource} tree from a CoRE Link Format
	 * string. 
	 * 
	 * @param linkFormatString The link format representation of the resources
	 * @return The resource set
	 */
	public static RemoteResource parse(String linkFormat) {
		
		Scanner scanner = new Scanner(linkFormat);
		RemoteResource root = new RemoteResource("");
		
		String path = null;
		while ((path = scanner.findInLine("</[^>]*>")) != null) {
			
			// Trim <...>
			path = path.substring(1, path.length() - 1);
			
			LOG.finer(String.format("Parsing link resource: %s", path));

			// Retrieve specified resource, create if necessary
			RemoteResource resource = new RemoteResource(path);
			
			// Read link format attributes
			LinkAttribute attr = null;
			while (scanner.findWithinHorizon(LinkFormat.DELIMITER, 1)==null && (attr = LinkAttribute.parse(scanner))!=null) {
				addAttribute(resource.getAttributes(), attr);
			}
			
			root.add(resource);
		}
		
		return root;
	}

// Methods /////////////////////////////////////////////////////////////////////
	
	public static boolean isSingle(String name) {
		return name.matches(String.format("%s|%s|%s", TITLE, MAX_SIZE_ESTIMATE, OBSERVABLE));
	}
	
	/**
	 * Enforces the rules defined in the CoRE Link Format when adding a new
	 * attribute to a set. "title" for instance may only occur once, while "ct"
	 * may occur several times.
	 * 
	 * @param attributes the attribute set to extend
	 * @param add the new attribute
	 * @return The success of adding
	 */
	public static boolean addAttribute(Set<LinkAttribute> attributes, LinkAttribute add) {
		
		if (isSingle(add.getName())) {
			for (LinkAttribute attrib : attributes) {
				if (attrib.getName().equals(add.getName())) {
					LOG.finest(String.format("Found existing singleton attribute: %s", attrib.getName()));
					return false;
				}
			}
		}
		
		// special rules
		if (add.getName().equals("ct") && add.getIntValue()<0) return false;
		if (add.getName().equals("sz") && add.getIntValue()<0) return false;
		
		LOG.finest(String.format("Added resource attribute: %s (%s)", add.getName(), add.getValue()));
		return attributes.add(add);
	}
	
	public static List<String> getStringValues(List<LinkAttribute> attributes) {
		List<String> values = new ArrayList<String>();
		for (LinkAttribute attrib : attributes) {
			values.add(attrib.getStringValue());
		}
		return values;
	}
	
	public static List<Integer> getIntValues(List<LinkAttribute> attributes) {
		List<Integer> values = new ArrayList<Integer>();
		for (LinkAttribute attrib : attributes) {
			values.add(attrib.getIntValue());
		}
		return values;
	}
	
// Attribute management ////////////////////////////////////////////////////////
	
	public static boolean matches(Resource resource, List<Option> query) {
		
		if (resource==null) return false;
		if (query==null || query.size()==0) return true;
		
		for (Option q : query) {
			String s = q.getStringValue();
			int delim = s.indexOf("=");
			if (delim != -1) {
				
				// split name-value-pair
				String attrName = s.substring(0, delim);
				String expected = s.substring(delim+1);

				// lookup attribute value
				for (LinkAttribute attrib : resource.getAttributes(attrName)) {
					String actual = attrib.getValue().toString();
				
					// get prefix length according to "*"
					int prefixLength = expected.indexOf('*');
					if (prefixLength >= 0 && prefixLength < actual.length()) {
				
						// reduce to prefixes
						expected = expected.substring(0, prefixLength);
						actual = actual.substring(0, prefixLength);
					}
					
					// compare strings
					if (expected.equals(actual)) {
						return true;
					}
				}
			} else {
				// flag attribute
				if (resource.getAttributes(s).size()>0) {
					return true;
				}
			}
		}
		return false;
	}
	
}

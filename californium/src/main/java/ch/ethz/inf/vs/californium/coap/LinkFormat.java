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
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import ch.ethz.inf.vs.californium.endpoint.resources.RemoteResource;
import ch.ethz.inf.vs.californium.endpoint.resources.Resource;

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
	public static final String LINK                  = "href";

	//for Resource Directory**********************************
	public static final String HOST		     		 = "h";
	public static final String LIFE_TIME     		 = "lt";
	public static final String INSTANCE		   		 = "ins";
	public static final String DOMAIN	     		 = "d";
	public static final String CONTEXT		   		 = "con";
	public static final String END_POINT     		 = "ep";
	//********************************************************
	
	public static final Pattern DELIMITER            = Pattern.compile("\\s*,+\\s*"); // generous parsing

// Serialization ///////////////////////////////////////////////////////////////
	
	public static String serialize(String key, String delimiter, SortedSet<String> values) {
		
		StringBuilder linkFormat = new StringBuilder();
		boolean quotes = false;
		
		linkFormat.append(key);
		
		if (values==null || values.first()==null) {
			throw new RuntimeException("Values null");
		}
		
		if (values.isEmpty() || values.first()==null || values.first().equals("")) return linkFormat.toString();
		
		linkFormat.append(delimiter);
		
		if (values.size()>1 || !values.first().matches("^[0-9]+$")) {
			linkFormat.append('"');
			quotes = true;
		}
		
		Iterator<String> it = values.iterator();
		while (it.hasNext()) {
			linkFormat.append(it.next());
			
			if (it.hasNext()) {
				linkFormat.append(' ');
			}
		}
		
		if (quotes) {
			linkFormat.append('"');
		}
		
		return linkFormat.toString();
	}
	
	public static String serialize(Resource resource, List<Option> query, boolean recursive) {
	
		StringBuilder linkFormat = new StringBuilder();
		
		// skip hidden and empty root in recursive mode, always skip non-matching resources
		if ((!resource.isHidden() && (!resource.getName().equals("") || resource.getAttributes().size()>0) || !recursive) && matches(resource, query)) {
			
			LOG.finer("Serializing resource link: " + resource.getPath());
			
			linkFormat.append("<");
			linkFormat.append(resource.getPath());
			linkFormat.append(">");
			
			for (String key : resource.getAttributes().keySet()) {
				linkFormat.append(';');
				linkFormat.append(serialize(key, "=",  resource.getAttributes(key)));
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

		RemoteResource root = new RemoteResource("");
		
		if (linkFormat!=null) {
			Scanner scanner = new Scanner(linkFormat);
			
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
					resource.setAttribute(attr.getName(), attr.getValue());
				}
				
				root.add(resource);
			}
		}
		
		return root;
	}

// Methods /////////////////////////////////////////////////////////////////////
	
	public static boolean isSingle(String name) {
		return name.matches(String.format("%s|%s|%s", TITLE, MAX_SIZE_ESTIMATE, OBSERVABLE));
	}
	
	public static List<String> getStringValues(Set<String> attributes) {
		List<String> values = new ArrayList<String>();
		if (attributes!=null) {
			for (String attrib : attributes) {
				values.add(attrib);
			}
		}
		return values;
	}
	
	public static List<Integer> getIntValues(Set<String> attributes) {
		List<Integer> values = new ArrayList<Integer>();
		if (attributes!=null) {
			for (String value : attributes) {
				values.add(Integer.parseInt(value));
			}
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

				if (attrName.equals(LinkFormat.LINK)) {
					if (expected.endsWith("*")) {
						return resource.getPath().startsWith(expected.substring(0, expected.length()-1));
					} else {
						return resource.getPath().equals(expected);
					}
				} else if (resource.getAttributes().containsKey(attrName)) {
					// lookup attribute value
					for (String actual : resource.getAttributes(attrName)) {
					
						// get prefix length according to "*"
						int prefixLength = expected.indexOf('*');
						if (prefixLength >= 0 && prefixLength < actual.length()) {
					
							// reduce to prefixes
							expected = expected.substring(0, prefixLength);
							actual = actual.substring(0, prefixLength);
						}
						
						// handle case like rt=[Type1 Type2]
						if (actual.indexOf(" ") > -1) { // if contains white space
							String[] parts = actual.split(" ");
							for (String part : parts) { // check each part for match
								if (part.equals(expected)) {
									return true;
								}
							}
						}
						
						// compare strings
						if (expected.equals(actual)) {
							return true;
						}
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

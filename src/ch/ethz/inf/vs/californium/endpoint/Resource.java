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
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.endpoint;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.RequestHandler;

/**
 * This class provides resource functionality to manage its attributes and
 * composed trees of resources. A server will use concrete
 * {@link LocalResource}s while a client will use {@link RemoteResource}s to
 * manage discovered resources of a remote server.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public abstract class Resource implements RequestHandler, Comparable<Resource> {

// Logging /////////////////////////////////////////////////////////////////////
	
	protected static final Logger LOG = Logger.getLogger(Resource.class.getName());

// Members /////////////////////////////////////////////////////////////////////

	/** The resource's identifier. */
	private String resourceIdentifier;
	
	/** The current parent of the resource. */
	protected Resource parent;

	/** The current sub-resources of the resource. A map to remove sub-resources by identifier. */
	protected SortedMap<String, Resource> subResources;

	/** The total number of sub-resources down from this resource. */
	private int totalSubResourceCount;

	/** Determines whether the resource is hidden in a resource discovery. */
	protected boolean hidden;

	/** Contains the resource's attributes specified in the CoRE Link Format. */
	protected TreeSet<LinkAttribute> attributes;

// Constructors ////////////////////////////////////////////////////////////////

	public Resource(String resourceIdentifier) {
		this(resourceIdentifier, false);
	}

	public Resource(String resourceIdentifier, boolean hidden) {
		
		// not removing surrounding slashes here, will be split up by endpoint
		
		this.resourceIdentifier = resourceIdentifier;
		this.attributes = new TreeSet<LinkAttribute>();

		this.hidden = hidden;
	}

// Methods /////////////////////////////////////////////////////////////////////

	/** 
	 * This method returns the resource name or path.
	 * 
	 * @param absolute return complete path
	 * @return The current resource URI
	 */
	protected String getResourceIdentifier(boolean absolute) {
		if (absolute && parent != null) {
	
			StringBuilder builder = new StringBuilder();
			builder.append(parent.getResourceIdentifier(absolute));
			builder.append('/');
			builder.append(resourceIdentifier);
	
			return builder.toString();
		} else {
			return resourceIdentifier;
		}
	}
	
	/**
	 * Returns the full resource path.
	 * 
	 * @return The path of this resource
	 */
	public String getPath() {
		return getResourceIdentifier(true);
	}

	/**
	 * Returns the resource name of this resource.
	 * 
	 * @return The name
	 */
	public String getName() {
		return getResourceIdentifier(false);
	}
	
	/**
	 * This method sets the resource name of this resource.
	 * 
	 * @param resourceURI the new name
	 */
	public void setName(String resourceIdentifier) {
		this.resourceIdentifier = resourceIdentifier;
	}

// Methods /////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns all attributes set for this resource.
	 * 
	 * @return the full set of attributes
	 */
	public Set<LinkAttribute> getAttributes() {
		return attributes;
	}

	/**
	 * Returns all attributes of the given name
	 * 
	 * @param name the attribute name, e.g.: "title", "ct"
	 * @return the set of attributes with the given name
	 */
	public List<LinkAttribute> getAttributes(String name) {
		ArrayList<LinkAttribute> ret = new ArrayList<LinkAttribute>();
		for (LinkAttribute attrib : attributes) {
			if (attrib.getName().equals(name)) {
				ret.add(attrib);
			}
		}
		return ret;
	}
	
	/**
	 * Adds the given attribute to the resource depending on the Link Format
	 * definition. "title" for instance may only occur once.
	 * 
	 * @param attrib the attribute to add
	 * @return the success of adding 
	 */
	public boolean setAttribute(LinkAttribute attrib) {
		// Adds depending on the Link Format rules
		return LinkFormat.addAttribute(attributes, attrib);
	}
	
	/**
	 * Removes all attributes with the given name.
	 * 
	 * @param name the name to remove
	 * @return the success of clearing
	 */
	public boolean clearAttribute(String name) {
		boolean cleared = false;
		for (LinkAttribute attrib : attributes) {
			if (attrib.getName()==name) {
				cleared |= attributes.remove(attrib);
			}
		}
		return cleared;
	}
	
// Convenience methods /////////////////////////////////////////////////////////
	
	/**
	 * This method returns the resource title of this resource.
	 * 
	 * @return The current resource title
	 */
	public String getTitle() {
		List<LinkAttribute> title = getAttributes(LinkFormat.TITLE);
		return title.isEmpty() ? null : title.get(0).getStringValue();
	}
	
	/**
	 * This method sets the resource title of this resource.
	 * 
	 * @param resourceTitle the resource title
	 */
	public void setTitle(String resourceTitle) {
		clearAttribute(LinkFormat.TITLE);
		setAttribute(new LinkAttribute(LinkFormat.TITLE, resourceTitle));
	}

	/**
	 * This method returns the values of the resource type attributes.
	 * 
	 * @return The list of set resource types
	 */
	public List<String> getResourceType() {
		return LinkFormat.getStringValues(getAttributes(LinkFormat.RESOURCE_TYPE));
	}

	/**
	 * This method sets the resource type of this resource.
	 * 
	 * @param resourceType the resource type 
	 */
	public void setResourceType(String resourceType) {
		setAttribute(new LinkAttribute(LinkFormat.RESOURCE_TYPE, resourceType));
	}
	
	/**
	 * This method returns the values of the interface description attributes.
	 * 
	 * @return The list of set interface descriptions
	 */
	public List<String> getInterfaceDescription() {
		return LinkFormat.getStringValues(getAttributes(LinkFormat.INTERFACE_DESCRIPTION));
	}

	/**
	 * This method adds a interface description to this resource.
	 * 
	 * @param description the resource interface description
	 */
	public void setInterfaceDescription(String description) {
		setAttribute(new LinkAttribute(LinkFormat.INTERFACE_DESCRIPTION, description));
	}

	/**
	 * This method returns the content type code of this resource.
	 * 
	 * @return The current resource content type code
	 */
	public List<Integer> getContentTypeCode() {
		return LinkFormat.getIntValues(getAttributes(LinkFormat.CONTENT_TYPE));
	}

	/**
	 * This method sets the content-type code of this resource.
	 * 
	 * @param code the resource content-type
	 */
	public void setContentTypeCode(int code) {
		setAttribute(new LinkAttribute(LinkFormat.CONTENT_TYPE, code));
	}

	/**
	 * This method returns the maximum size estimate of this resource.
	 * 
	 * @return The current resource maximum size estimate
	 */
	public int getMaximumSizeEstimate() {
		List<LinkAttribute> sz = getAttributes(LinkFormat.MAX_SIZE_ESTIMATE);
		return sz.isEmpty() ? -1 : sz.get(0).getIntValue();
	}

	/**
	 * This method sets the maximum size estimate of this resource.
	 * 
	 * @param maximumSize the resource maximum size estimate
	 */
	public void setMaximumSizeEstimate(int size) {
		setAttribute(new LinkAttribute(LinkFormat.MAX_SIZE_ESTIMATE, size));
	}

	/**
	 * This method returns the observable flag of this resource.
	 * 
	 * @return The current resource observable flag
	 */
	public boolean isObservable() {
		return getAttributes(LinkFormat.OBSERVABLE).size()>0;
	}

	/**
	 * This method sets the observable flag of this resource.
	 * 
	 * @param maximumSizeExtimate the resource maximum size estimate
	 */	
	public void isObservable(boolean observable) {
		if (observable) {
			setAttribute(new LinkAttribute(LinkFormat.OBSERVABLE));
		} else {
			clearAttribute(LinkFormat.OBSERVABLE);
		}
	}
	
	public boolean isHidden() {
		return hidden;
	}
	
	public void isHidden(boolean change) {
		hidden = change;
	}

// Sub-resource management /////////////////////////////////////////////////////

	/**
	 * Removes this resource from its parent.
	 */
	public void remove() {
		if (parent != null) {
			parent.removeSubResource(this);
		}
	}

	/**
	 * Counts the direct children of this resource.
	 * 
	 * @return The number of child resources
	 */
	public int subResourceCount() {
		return subResources != null ? subResources.size() : 0;
	}

	/**
	 * Counts the total number of sub-resources.
	 * 
	 * @return The total number of sub-resources
	 */
	public int totalSubResourceCount() {
		return totalSubResourceCount;
	}

	/**
	 * Looks recursively for the resource specified by resourcePath. If the flag
	 * create is set, a new resource of the same type as this will be created at
	 * the given path.
	 * 
	 * @param resourcePath the path to the resource of interest
	 * @param create flag to create resource if not existing
	 * @return The Resource of interest or null if not found and create is false
	 */
	public Resource getResource(String resourcePath, boolean create) {
		
		int pos = resourcePath.indexOf('/');
		String head = null;
		String tail = null;
		
		// slash in the middle
		if (pos != -1 && pos < resourcePath.length() - 1) {
			head = resourcePath.substring(0, pos);
			tail = resourcePath.substring(pos + 1);
		} else {
			head = resourcePath;
		}

		if (head.equals(this.resourceIdentifier)) {
			if (tail!=null) {
				Resource sub = null;
				for (Resource check : getSubResources()) {
					if ((sub = check.getResource(tail, create))!=null) {
						return sub;
					}
				}

				// resource not found, create it?
				if (create) {
					try {
						
						// Instantiate a new Resource sub-type using the identifier, hidden constructor
						sub = getClass().getConstructor(String.class, Boolean.class).newInstance(tail, false);
						addSubResource(sub);
						
					} catch (Exception e) {
						LOG.severe(String.format("Cannot instantiate new sub-resource [%s]: %s", tail, e.getMessage()));
					}
					return sub;
				}
				
			} else {
				return this;
			}
		}
		
		return null;
	}
	
	public Resource getResource(String resourcePath) {
		return getResource(resourcePath, false);
	}

	/**
	 * Returns the sorted set of sub-resources.
	 * 
	 * @return the sub-resource set
	 */
	public Set<Resource> getSubResources() {
		
		if (subResources==null) {
			return Collections.emptySet();
		}
		
		// sorted sub-resources
		TreeSet<Resource> subs = new TreeSet<Resource>(); 
		for (Resource sub : subResources.values()) {
			subs.add(sub);
		}
		
		return subs;
	}

	public void addSubResource(Resource resource) {
		if (resource != null) {
			
			// lazy creation
			if (subResources == null) {
				subResources = new TreeMap<String, Resource>();
			}
			
			subResources.put(resource.resourceIdentifier, resource);

			resource.parent = this;

			// update number of sub-resources in the tree
			Resource p = resource.parent;
			while (p != null) {
				++p.totalSubResourceCount;
				p = p.parent;
			}
		}
	}

	public void removeSubResource(Resource resource) {
		if (resource != null) {
			subResources.remove(resource.resourceIdentifier);

			// update number of sub-resources in the tree
			Resource p = resource.parent;
			while (p != null) {
				--p.totalSubResourceCount;
				p = p.parent;
			}

			resource.parent = null;
		}
	}

	public void removeSubResource(String resourcePath) {
		removeSubResource(getResource(resourcePath, false));
	}

	/**
	 * When implementing this method, {@link #addSubResource(Resource)} should be
	 * used to keep sub-resource counting consistent.
	 * 
	 * @param request the request carrying the data for creation
	 * @param newIdentifier the name of the new sub-resource
	 */
	public abstract void createSubResource(Request request, String newIdentifier);

	public int compareTo(Resource o) {
		return getPath().compareTo(o.getPath());
	}
	
	public void prettyPrint(PrintStream out, int intend) {

		for (int i = 0; i < intend; i++) {
			out.append(' ');
		}

		out.printf("+[%s]", resourceIdentifier);

		String title = getTitle();
		if (title != null) {
			out.printf(" %s", title);
		}

		out.println();
		
		for (LinkAttribute attrib : getAttributes()) {
			
			if (attrib.getName().equals(LinkFormat.TITLE)) continue;
			
			for (int i = 0; i < intend+3; i++) {
				out.append(' ');
			}
			out.printf("- %s\n", attrib.serialize());
		}

		if (subResources != null) {
			for (Resource sub : subResources.values()) {
				sub.prettyPrint(out, intend + 2);
			}
		}
	}

	public void prettyPrint() {
		prettyPrint(System.out, 0);
	}

}

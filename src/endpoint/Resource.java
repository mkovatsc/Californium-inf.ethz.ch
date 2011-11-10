package endpoint;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import coap.LinkFormat;
import coap.Option;
import coap.Request;
import coap.RequestHandler;

/*
 * This class describes the functionality of a CoAP resource
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public abstract class Resource implements RequestHandler {

	// Constructors ////////////////////////////////////////////////////////////

	/*
	 * This is a constructor for a new resource
	 */
	public Resource() {
		this(null);
	}

	public Resource(String resourceIdentifier) {
		this(resourceIdentifier, false);
	}

	public Resource(String resourceIdentifier, boolean hidden) {
		this.resourceIdentifier = resourceIdentifier;
		this.attributes = new TreeMap<String, LinkFormat.Attribute>();

		this.hidden = hidden;
	}

	// Methods /////////////////////////////////////////////////////////////////

	/*
	 * This method sets the resource identifier of the current resource
	 * 
	 * @param resourceURI The resource identifier
	 */
	public void setResourceIdentifier(String resourceIdentifier) {
		this.resourceIdentifier = resourceIdentifier;
	}

	/*
	 * This method sets the resource type of the current resource
	 * 
	 * @param resourceType The resource type
	 */
	public void setResourceType(String resourceType) {
		setAttributeValue(LinkFormat.RESOURCE_TYPE, resourceType);
	}
	
	/*
	 * This method sets the resource title of the current resource
	 * 
	 * @param resourceTitle The resource title
	 */
	public void setResourceTitle(String resourceTitle) {
		setAttributeValue(LinkFormat.TITLE, resourceTitle);
	}
	
	/*
	 * This method sets the interface description of the current resource
	 * 
	 * @param interfaceDescription The resource interface description
	 */
	public void setInterfaceDescription(String interfaceDescription) {
		setAttributeValue(LinkFormat.INTERFACE_DESCRIPTION, interfaceDescription);
	}

	/*
	 * This method sets the content type code of the current resource
	 * 
	 * @param contentTypeCode The resource contentTypeCode
	 */
	public void setContentTypeCode(int contentTypeCode) {
		setAttributeValue(LinkFormat.CONTENT_TYPE, contentTypeCode);
	}

	/*
	 * This method sets the maximum size estimate of the current resource
	 * 
	 * @param maximumSizeExtimate The resource maximum size estimate
	 */
	public void setMaximumSizeEstimate(int maximumSizeEstimate) {
		setAttributeValue(LinkFormat.MAX_SIZE_ESTIMATE, maximumSizeEstimate);
	}

	/*
	 * This method sets the observable flag of the current resource
	 * 
	 * @param maximumSizeExtimate The resource maximum size estimate
	 */	
	public void setObservable(boolean observable) {
		setAttributeValue(LinkFormat.OBSERVABLE, observable);
	}

	// Functions ///////////////////////////////////////////////////////////////
	

	/*
	 * This method returns the resource URI of the current resource
	 * 
	 * @return The current resource URI
	 */
	public String getResourceIdentifier(boolean absolute) {
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

	public String getResourceIdentifier() {
		return getResourceIdentifier(false);
	}

	public String getResourcePath() {
		return getResourceIdentifier(true);
	}

	/*
	 * This method returns the resource type of the current resource
	 * 
	 * @return The current resource type
	 */
	public String getResourceType() {
		Object value = getAttributeValue(LinkFormat.RESOURCE_TYPE);
		return value instanceof String ? (String)value : null;
	}
	
	/*
	 * This method returns the resource title of the current resource
	 * 
	 * @return The current resource title
	 */
	public String getResourceTitle() {
		Object value = getAttributeValue(LinkFormat.TITLE);
		return value instanceof String ? (String)value : null;
	}

	/*
	 * This method returns the interface description of the current resource
	 * 
	 * @return The current resource interface description
	 */
	public String getInterfaceDescription() {
		Object value = getAttributeValue(LinkFormat.INTERFACE_DESCRIPTION);
		return value instanceof String ? (String)value : null;
	}

	/*
	 * This method returns the content type code of the current resource
	 * 
	 * @return The current resource content type code
	 */
	public int getContentTypeCode() {
		Object value = getAttributeValue(LinkFormat.CONTENT_TYPE);
		return value instanceof Integer ? (Integer)value : null;
	}

	/*
	 * This method returns the maximum size estimate of the current resource
	 * 
	 * @return The current resource maximum size estimate
	 */
	public int getMaximumSizeEstimate() {
		Object value = getAttributeValue(LinkFormat.MAX_SIZE_ESTIMATE);
		return value instanceof Integer ? (Integer)value : null;
	}
	
	/*
	 * This method returns the observable flag of the current resource
	 * 
	 * @return The current resource observable flag
	 */
	public boolean isObservable() {
		Object value = getAttributeValue(LinkFormat.OBSERVABLE);
		return value instanceof Boolean ? (Boolean)value : null;
	}
	
	// Serialization ///////////////////////////////////////////////////////////
	
	/*
	 * This method returns a resource set from a link format string
	 * 
	 * @param linkFormatString The link format representation of the resources
	 * 
	 * @return The resource set
	 */
	public void addLinkFormat(String linkFormat) {
		
		Scanner scanner = new Scanner(linkFormat);
		
		String identifier = null;
		while ((identifier = scanner.findInLine("</.*?>")) != null) {
			
			// Trim </...>
			identifier = identifier.substring(2, identifier.length() - 1);

			// Retrieve specified resource, create if necessary
			Resource resource = subResource(identifier, true);
			
			// Read link format attributes
			resource.readAttributes(scanner);
		}
	}

	/*
	 * This method returns a link format string for the current resource
	 * 
	 * @return The link format string representing the current resource
	 */
	public String toLinkFormatItem() {
		StringBuilder linkFormat = new StringBuilder();
		
		linkFormat.append("<");
		linkFormat.append(getResourceIdentifier(true));
		linkFormat.append(">");

		writeAttributes(linkFormat);
		
		return linkFormat.toString();
	}

	/*
	 * This method returns a link format string for the current sub-resource set
	 * 
	 * @param query A list of query options used for Query Filtering
	 * @return The link format string representing the current sub-resource set
	 */
	public String toLinkFormat(List<Option> query) {

		// Create new StringBuilder
		StringBuilder builder = new StringBuilder();

		// Build the link format
		buildLinkFormat(builder, query);

		// Remove last delimiter
		if (builder.length() > 0) {
			builder.deleteCharAt(builder.length() - 1);
		}

		return builder.toString();
	}
	
	public String toLinkFormat() {
		return toLinkFormat(null);
	}

	private void buildLinkFormat(StringBuilder builder, List<Option> query) {

		if (subResources != null) {
			// Loop over all sub-resources
			for (Resource resource : subResources.values()) {

				if (!resource.hidden && resource.matches(query)) {

					// Convert Resource to string representation and add
					// delimiter
					builder.append(resource.toLinkFormatItem());
					builder.append(',');
				}

				// Recurse
				resource.buildLinkFormat(builder, query);
			}
		}
	}

	
	// Attribute management ////////////////////////////////////////////////////
	
	public void setAttributeValue(String name, Object value) {
		LinkFormat.Attribute attr = new LinkFormat.Attribute(name, value);
		attributes.put(name, attr);
	}
	
	public Object getAttributeValue(String name) {
		LinkFormat.Attribute attr = attributes.get(name);
		return attr != null ? attr.value() : null;
	}

	private void readAttributes(Scanner scanner) {
		LinkFormat.Attribute attr = null;
		attributes.clear();
		while (
			scanner.findWithinHorizon(LinkFormat.DELIMITER, 1) == null && 
			(attr = LinkFormat.Attribute.parse(scanner)) != null
		) {
			attributes.put(attr.name(), attr);
			scanner.skip(LinkFormat.SEPARATOR);
		}
	}
	
	public void writeAttributes(StringBuilder builder) {
		for (LinkFormat.Attribute attr: attributes.values()) {
			builder.append(";");
			attr.serialize(builder);
		}
	}
	
	public boolean matches(List<Option> query) {
		
		if (query == null) return true;
		
		for (Option q : query) {
			String s = q.getStringValue();
			int delim = s.indexOf("=");
			if (delim != -1) {
				
				// split name-value-pair
				String attrName = s.substring(0, delim);
				String expected = s.substring(delim+1);

				// lookup attribute value
				Object value = getAttributeValue(attrName);
				if (value == null) return false;
				String actual = value.toString();
				
				// get prefix length according to "*"
				int prefixLength = expected.indexOf('*');
				if (prefixLength >= 0 && prefixLength < actual.length()) {
				
					// reduce to prefixes
					expected = expected.substring(0, prefixLength);
					actual = actual.substring(0, prefixLength);
				}
				
				// compare strings
				if (!expected.equals(actual)) {
					return false;
				}
			} else {
				// flag attribute
				if (getAttributeValue(s) == null) {
					return false;
				}
			}
		}
		return true;
	}

	// Sub-resource management /////////////////////////////////////////////////

	public int subResourceCount() {
		return subResources != null ? subResources.size() : 0;
	}

	public int totalSubResourceCount() {
		return totalSubResourceCount;
	}

	public Resource subResource(String resourceIdentifier, boolean create) {

		int pos = resourceIdentifier.indexOf('/');
		String head = null;
		String tail = null;
		if (pos != -1 && pos < resourceIdentifier.length() - 1) {

			head = resourceIdentifier.substring(0, pos);
			tail = resourceIdentifier.substring(pos + 1);
		} else {
			head = resourceIdentifier;
			tail = null;
		}

		Resource resource = subResources != null ? subResources.get(head)
				: null;

		if (resource == null && create) {
			try {
				resource = getClass().newInstance();

				resource.setResourceIdentifier(head);
				addSubResource(resource);

			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (resource != null && tail != null) {
			return resource.subResource(tail, create);
		} else {
			return resource;
		}

	}

	public Resource subResource(String resourceIdentifier) {
		return subResource(resourceIdentifier, false);
	}

	public Resource getResource(String resourceIdentifier) {
		int pos = resourceIdentifier.indexOf('/');
		String head = null;
		String tail = null;
		if (pos != -1 && pos < resourceIdentifier.length() - 1) {

			head = resourceIdentifier.substring(0, pos);
			tail = resourceIdentifier.substring(pos + 1);
		} else {
			head = resourceIdentifier;
			tail = null;
		}

		if (head.equals(this.resourceIdentifier)) {
			return tail != null ? subResource(tail) : this;
		} else {
			return null;
		}
	}

	public Resource[] getSubResources() {
		Resource[] resources;
		if (subResources != null) {
			resources = new Resource[subResources.size()];
		} else {
			return new Resource[0];
		}

		// Get set representation of hash map
		Set<Entry<String, Resource>> content = subResources.entrySet();

		// Get iterator on set
		Iterator<Entry<String, Resource>> it = content.iterator();

		// Loop over all set elements
		int pos = 0;
		while (it.hasNext()) {
			Entry<String, Resource> currentEntry = it.next();
			resources[pos] = currentEntry.getValue();
			pos++;
		}
		return resources;
	}

	public void addSubResource(Resource resource) {
		if (resource != null) {
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

	public void remove() {
		if (parent != null) {
			parent.removeSubResource(this);
		}
	}

	public void removeSubResource(String resourceIdentifier) {
		removeSubResource(subResource(resourceIdentifier));
	}

	public abstract void createNew(Request request, String newIdentifier);

	public void log(PrintStream out, int intend) {
		
		for (int i = 0; i < intend; i++) {
			out.append(' ');
		}
		
		out.printf("+[%s]", resourceIdentifier);
		
		String title = getResourceTitle();
		if (title != null) {
			out.printf(" %s", title);
		}
		
		out.println();
		
		if (subResources != null) {
			for (Resource sub : subResources.values()) {
				sub.log(out, intend + 2);
			}
		}
	}

	public void log() {
		log(System.out, 0);
	}

	// Attributes //////////////////////////////////////////////////////////////

	// Contains the resource's attributes as specified by CoRE Link Format
	protected Map<String, LinkFormat.Attribute> attributes;
	
	// The current resource's parent
	protected Resource parent;

	// The current resource's sub-resources
	protected Map<String, Resource> subResources;

	// The total number of sub-resources in the current resource
	private int totalSubResourceCount;

	// The current resource's identifier
	private String resourceIdentifier;

	// Determines whether the resource is hidden from a resource discovery
	protected boolean hidden;

}

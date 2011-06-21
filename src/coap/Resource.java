package coap;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

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
		this.resourceType = new String();
		this.resourceTitle = new String();
		this.interfaceDescription = new String();
		this.contentTypeCode = -1;
		this.maximumSizeEstimate = -1;
		this.observable = false;
		this.hidden = hidden;
	}

	// Procedures //////////////////////////////////////////////////////////////

	/*
	 * This method sets an extension attribute given in a string of the form
	 * "...=..."
	 * 
	 * @param linkExtension The link extension string specifying a link
	 * extension and a value for it
	 */
	public void populateAttributeFromLinkExtension(String linkExtension) {
		String[] elements = linkExtension.split("=");

		String extension = elements[0];
		String value = elements[1];

		if (extension.equals("n")) {
			//For backwards compatibility. The use of 'n' is deprecated and 'rt'
			//is used instead
			setResourceType(value.substring(1, value.length() - 1));
		} else if (extension.equals("d")) {
			//For backwards compatibility. The use of 'd' is deprecated and 'if'
			//is used instead
			setInterfaceDescription(value.substring(1, value.length() - 1));
		} else if (extension.equals("if")) {
			setInterfaceDescription(value.substring(1, value.length() - 1));
		} else if (extension.equals("ct")) {
			setContentTypeCode(Integer.parseInt(value));
		} else if (extension.equals("sz")) {
			setMaximumSizeEstimate(Integer.parseInt(value));
		} else if (extension.equals("obs")) {
			setObservable(Boolean.parseBoolean(value));
		} else if (extension.equals("rt")) {
			setResourceType(value.substring(1, value.length() - 1));
		} else if (extension.equals("title")) {
			setResourceTitle(value.substring(1, value.length() - 1));
		}
	}

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
		this.resourceType = resourceType;
	}
	
	/*
	 * This method sets the resource title of the current resource
	 * 
	 * @param resourceTitle The resource title
	 */
	public void setResourceTitle(String resourceTitle) {
		this.resourceTitle = resourceTitle;
	}
	
	/*
	 * This method sets the interface description of the current resource
	 * 
	 * @param interfaceDescription The resource interface description
	 */
	public void setInterfaceDescription(String interfaceDescription) {
		this.interfaceDescription = interfaceDescription;
	}

	/*
	 * This method sets the content type code of the current resource
	 * 
	 * @param contentTypeCode The resource contentTypeCode
	 */
	public void setContentTypeCode(int contentTypeCode) {
		this.contentTypeCode = contentTypeCode;
	}

	/*
	 * This method sets the maximum size estimate of the current resource
	 * 
	 * @param maximumSizeExtimate The resource maximum size estimate
	 */
	public void setMaximumSizeEstimate(int maximumSizeEstimate) {
		this.maximumSizeEstimate = maximumSizeEstimate;
	}

	/*
	 * This method sets whether the current resource is observable
	 * 
	 * @param observable The boolean value whether the current resource is
	 * observable
	 */
	public void setObservable(boolean observable) {
		this.observable = observable;
	}

	/*
	 * This method sets attributes of a given resource according to data from a
	 * given link extension string
	 * 
	 * @param res The resource containing the attributes which should be set
	 * 
	 * @param linkExtension The string with the link extension data
	 */
	private static void populateAttributeFromLinkExtension(Resource res,
			String linkExtension) {
		// "extension=value" is split to [extension, value]
		String[] elements = linkExtension.split("=");

		// Set extension string fo first array element (containing extension)
		String extension = elements[0];

		// Set value string if available
		String value = new String();
		if (elements.length > 1) {
			value = elements[1];
		}
		// Set attribute according to extension
		if (extension.equals("n")) {
			//For backwards compatibility. The use of 'n' is deprecated and 'rt'
			//is used instead
			res.setResourceType(value.substring(1, value.length() - 1));
		} else if (extension.equals("rt")) {
			res.setResourceType(value.substring(1, value.length() - 1));
		} else if (extension.equals("d")) {
			//For backwards compatibility. The use of 'd' is deprecated and 'if'
			//is used instead
			res.setInterfaceDescription(value.substring(1, value.length() - 1));
		} else if (extension.equals("if")) {
			res.setInterfaceDescription(value.substring(1, value.length() - 1));
		} else if (extension.equals("ct")) {
			res.setContentTypeCode(Integer.parseInt(value));
		} else if (extension.equals("sz")) {
			res.setMaximumSizeEstimate(Integer.parseInt(value));
		} else if (extension.equals("obs")) {
			res.setObservable(true);
		} else if (extension.equals("title")) {
			res.setResourceTitle(value.substring(1, value.length() - 1));
		}
	}

	// Functions ///////////////////////////////////////////////////////////////

	/*
	 * This method returns a resource set from a link format string
	 * 
	 * @param linkFormatString The link format representation of the resources
	 * 
	 * @return The resource set
	 */
	public void addLinkFormat(String linkFormat) {

		// Resources are separated by comma ->tokenize input string
		StringTokenizer items = new StringTokenizer(linkFormat, ",");

		// Get resources
		while (items.hasMoreTokens()) {
			addLinkFormatItem(items.nextToken());
		}

	}

	private void addLinkFormatItem(String item) {

		StringTokenizer tokens = new StringTokenizer(item, ";");

		// Get resource URI as <....> string
		String identifier = tokens.nextToken();

		// Trim </...>
		identifier = identifier.substring(2, identifier.length() - 1);

		// Retrieve specified resource, create if necessary
		Resource resource = subResource(identifier, true);

		// Rest of tokens has form ...=...
		while (tokens.hasMoreTokens()) {
			populateAttributeFromLinkExtension(resource, tokens.nextToken());
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
		linkFormat.append(">;");

		if (!this.getResourceType().isEmpty()) {
			linkFormat.append("rt=\"");
			linkFormat.append(this.getResourceType());
			linkFormat.append("\";");
		}
		if (!this.getInterfaceDescription().isEmpty()) {
			linkFormat.append("if=\"");
			linkFormat.append(this.getInterfaceDescription());
			linkFormat.append("\";");
		}
		if (this.getContentTypeCode() != -1) {
			linkFormat.append("ct=");
			linkFormat.append(this.getContentTypeCode());
			linkFormat.append(";");
		}
		if (this.getMaximumSizeEstimate() != -1) {
			linkFormat.append("sz=");
			linkFormat.append(this.getMaximumSizeEstimate());
			linkFormat.append(";");
		}
		if (this.isObservable()) {
			linkFormat.append("obs;");
		}
		if (!this.getResourceTitle().isEmpty()) {
			linkFormat.append("title=\"");
			linkFormat.append(this.getResourceTitle());
			linkFormat.append("\";");
		}
		// Remove last semicolon
		linkFormat.deleteCharAt(linkFormat.length() - 1);

		return linkFormat.toString();
	}

	/*
	 * This method returns a link format string for the current sub-resource set
	 * 
	 * @return The link format string representing the current sub-resource set
	 */
	public String toLinkFormat() {

		// Create new StringBuilder
		StringBuilder builder = new StringBuilder();

		// Build the link format
		buildLinkFormat(builder);

		// Remove last delimiter
		if (builder.length() > 0) {
			builder.deleteCharAt(builder.length() - 1);
		}

		return builder.toString();
	}

	private void buildLinkFormat(StringBuilder builder) {

		if (subResources != null) {
			// Loop over all sub-resources
			for (Resource resource : subResources.values()) {

				if (!resource.hidden) {

					// Convert Resource to string representation and add
					// delimiter
					builder.append(resource.toLinkFormatItem());
					builder.append(',');
				}

				// Recurse
				resource.buildLinkFormat(builder);
			}
		}
	}

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
		return resourceType;
	}
	
	/*
	 * This method returns the resource title of the current resource
	 * 
	 * @return The current resource title
	 */
	public String getResourceTitle() {
		return resourceTitle;
	}

	/*
	 * This method returns the interface description of the current resource
	 * 
	 * @return The current resource interface description
	 */
	public String getInterfaceDescription() {
		return interfaceDescription;
	}

	/*
	 * This method returns the content type code of the current resource
	 * 
	 * @return The current resource content type code
	 */
	public int getContentTypeCode() {
		return contentTypeCode;
	}

	/*
	 * This method returns the maximum size estimate of the current resource
	 * 
	 * @return The current resource maximum size estimate
	 */
	public int getMaximumSizeEstimate() {
		return maximumSizeEstimate;
	}

	/*
	 * This method returns whether the current resource is observable or not
	 * 
	 * @return Boolean value whether the current resource is observable
	 */
	public boolean isObservable() {
		return observable;
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

	public abstract void createNew(PUTRequest request, String newIdentifier);

	public void log(PrintStream out, int intend) {
		for (int i = 0; i < intend; i++)
			out.append(' ');
		out.printf("+[%s] %s\n", resourceIdentifier, resourceType);
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

	// The current resource's parent
	protected Resource parent;

	// The current resource's sub-resources
	protected Map<String, Resource> subResources;

	// The total number of sub-resources in the current resource
	private int totalSubResourceCount;

	// The current resource's identifier
	private String resourceIdentifier;

	// The current resource's type
	private String resourceType;
	
	// The current resource's title
	private String resourceTitle;

	// The current resource's interface description
	private String interfaceDescription;

	// The current resource's content type code
	private int contentTypeCode;

	// The current resource's maximum size estimate
	private int maximumSizeEstimate;

	// The current resource's observability
	private boolean observable;

	// Determines whether the resource is hidden from a resource discovery
	protected boolean hidden;

}

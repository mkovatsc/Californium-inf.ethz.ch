package ch.ethz.inf.vs.californium.resources.proxy;



import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class RDResource extends ResourceBase {

	public RDResource() {
		super("rd");
	}

	public RDResource(String resourceIdentifier) {
		super(resourceIdentifier);
	}

	// TODO
//	/*
//	 * Creates a new sub-resource with the given identifier in this resource, or
//	 * get it from the Option-query. Finally set the other parameters.
//	 */
//	@Override
//	public void createSubResource(Request request, String newIdentifier) {
//
//		// get name and lifetime from option query
//		LinkAttribute attr;
//		int lifeTime = Properties.std.getInt("DEFAULT_LIFE_TIME"); // default
//																	// life time
//																	// if not
//																	// specified
//		List<Option> query = request.getOptions(OptionNumberRegistry.URI_QUERY);
//		if (query != null) {
//			for (Option opt : query) {
//				attr = LinkAttribute.parse(opt.getStringValue());
//				if (attr.getName().equals(LinkFormat.HOST)) {
//					newIdentifier = attr.getValue();
//				}
//				// System.out.println(newIdentifier);}
//				if (attr.getName().equals(LinkFormat.LIFE_TIME)) {
//					lifeTime = attr.getIntValue();
//				}
//			}
//		}
//
//		Response response;
//
//		ResourceNode resource = (ResourceNode) getResource(newIdentifier);
//		// create new sub-resource
//		if (resource == null) {
//			resource = new ResourceNode(newIdentifier, lifeTime);
//			add(resource);
//
//			// create new response
//			response = new Response(CodeRegistry.RESP_CREATED);
//		} else {
//			// change the existing resource
//			resource.setLifeTime(lifeTime);
//			response = new Response(CodeRegistry.RESP_CHANGED);
//		}
//
//		// set resourse's Parameters
//		resource.setParameters(request.getPayloadString(), null);
//
//		// TODO retrieve Address for DNS
//		// request.getAddress();
//
//		// inform client about the location of the new resource
//		response.setLocationPath(resource.getPath());
//
//		// complete the request
//		request.respond(response);
//	}
//
//	/*
//	 * GETs the content of this resource directory. If the content-type of the
//	 * request is set to application/link-format or if the resource does not
//	 * store any data, the contained sub-resources are returned in link format.
//	 */
//	@Override
//	public void performGET(GETRequest request) {
//		// create response
//		Response response = new Response(CodeRegistry.RESP_CONTENT);
//
//		/*
//		 * set payload's response with the info asked in the URI_QUERY of the
//		 * request message. e.g. In URI_QUERY rt="Temperature", the response it
//		 * will be with all the nodes that provide a temperature resource.
//		 */
//
//		response.setPayload(toLinkFormat(request.getOptions(OptionNumberRegistry.URI_QUERY)), MediaTypeRegistry.APPLICATION_LINK_FORMAT);
//
//		// complete the request
//		request.respond(response);
//	}
//
//	/*
//	 * POSTs a new sub-resource to this resource. The name of the new
//	 * sub-resource is a random number if not specified in the Option-query.
//	 */
//	@Override
//	public void performPOST(POSTRequest request) {
//		// System.out.print("POST	");
//		// get a random ID if no name specified
//		int rndName;
//		do {// while it found a subResource with the same name.
//			rndName = (int) (Math.random() * 1000);
//		} while (getResource(Integer.toString(rndName)) != null);
//
//		// create the new resource(One for each node)
//		createSubResource(request, Integer.toString(rndName));
//	}
//
//	/*
//	 * the following three methods are used to print the right string to put in
//	 * the payload to respond to the GET request.
//	 */
//	public String toLinkFormat(List<Option> query) {
//
//		// Create new StringBuilder
//		StringBuilder builder = new StringBuilder();
//
//		// Build the link format
//		buildLinkFormat(this, builder, query);
//
//		// Remove last delimiter
//		if (builder.length() > 0) {
//			builder.deleteCharAt(builder.length() - 1);
//		}
//
//		return builder.toString();
//	}
//
//	public String toLinkFormatItem(Resource resource) {
//		StringBuilder linkFormat = new StringBuilder();
//
//		linkFormat.append("<coap://");
//		linkFormat.append(resource.getResourcesPath());
//		linkFormat.append(">");
//		
//		for (String key : resource.getAttributes().keySet()) {
//			linkFormat.append(';');
//			linkFormat.append(LinkFormat.serialize(key, "=",  resource.getAttributes(key)));
//		}
//
//		return linkFormat.toString();
//	}
//
//	private void buildLinkFormat(Resource resource, StringBuilder builder, List<Option> query) {
//		if (resource.totalSubResourceCount() > 0) {
//
//			// Loop over all sub-resources
//			for (Resource res : resource.getSubResources()) {
//				// System.out.println(resource.getSubResources().size());
//				// System.out.println(res.getName());
//				if (!res.isHidden() && LinkFormat.matches(res, query) && !res.getAttributes().isEmpty()) {
//
//					// Convert Resource to string representation and add
//					// delimiter
//					builder.append(toLinkFormatItem(res));
//					builder.append(',');
//				}
//				// Recurse
//				buildLinkFormat(res, builder, query);
//			}
//		}
//	}
//
//	private class ResourceNode extends LocalResource {
//
//		// Timer for lifeTime, remove current subResource if expired.
//		private Timer timer;
//		private int lastLifeTime;
//
//		public ResourceNode(String identifier, int lifeTime) {
//			super(identifier);
//			setLifeTime(lifeTime);
//		}
//
//		/*
//		 * add a new resource to the node. E.g. the resource temperature or
//		 * humidity. If the path is /readings/temp, temp will be a subResource
//		 * of readings, which is a subResource of the node.
//		 */
//		public LocalResource addNodeResource(String path) {
//			Scanner scanner = new Scanner(path);
//			scanner.useDelimiter("/");
//			String next = "";
//			boolean resourceExist = false;
//			Resource resource = this; // It's the resource that represents the
//										// Node
//			LocalResource subResource = null;
//			while (scanner.hasNext()) {
//				resourceExist = false;
//				next = scanner.next();
//				for (Resource res : resource.getSubResources()) {
//					if (res.getName().equals(next)) {
//						subResource = (LocalResource) res;
//						resourceExist = true;
//					}
//				}
//				if (!resourceExist) {
//					subResource = new LocalResource(next);
//					resource.add(subResource);
//				}
//				resource = subResource;
//			}
//			subResource.setResourcesPath(this.getName() + path);
//			return subResource;
//		}
//
//		/*
//		 * DELETEs this node resource
//		 */
//		@Override
//		public void performDELETE(DELETERequest request) {
//			// remove this resource
//			remove();
//			request.respond(CodeRegistry.RESP_DELETED);
//		}
//
//		/*
//		 * PUTs content to this resource. PUT is a periodic request from the
//		 * node to update the lifetime.
//		 */
//		@Override
//		public void performPUT(PUTRequest request) {
//			// System.out.println("PUT	"+this.getResourceIdentifier());
//			setLifeTime(lastLifeTime);// default life time if not specified
//			setParameters(request.getPayloadString(), request.getOptions(OptionNumberRegistry.URI_QUERY));
//
//			// complete the request
//			request.respond(CodeRegistry.RESP_CHANGED);
//		}
//
//		/*
//		 * set either a new lifetime (for new resources, POST request) or update
//		 * the lifetime (for PUT request)
//		 */
//		public void setLifeTime(int lifeTime) {
//			lastLifeTime = lifeTime;
//			if (timer != null) {
//				timer.cancel();// delete the previous timer before it expire.
//			}
//			timer = new Timer();
//			timer.schedule(new Task(this), lifeTime * 1000);// from sec to ms
//		}
//
//		/*
//		 * This method is performed both for POST and for PUT request. set the
//		 * Attribute of the resource (the node)
//		 */
//		public void setParameters(String payload, List<Option> query) {
//			// scannering of the payload for setting parameters
//			attributes.clear();
//			Scanner scanner = new Scanner(payload);
//			LinkAttribute attr;
//
//			/*
//			 * get lifetime from option query - only for PUT request.
//			 */
//			if (query != null) {
//				for (Option opt : query) {
//					attr = LinkAttribute.parse(opt.getStringValue());
//					if (attr.getName().equals(LinkFormat.LIFE_TIME)) {
//						setLifeTime(attr.getIntValue());
//					}
//				}
//			}
//
//			/*
//			 * Create a new subResource for each resource the node wants
//			 * register. Each resource is separated by ",". E.g. A node can
//			 * register a resource for reading the temperature and another one
//			 * for reading the humidity.
//			 */
//			scanner.useDelimiter(",");
//			List<String> pathResources = new ArrayList<String>();
//			while (scanner.hasNext()) {
//				pathResources.add(scanner.next());
//			}
//
//			for (String p : pathResources) {
//				scanner = new Scanner(p);
//
//				/*
//				 * get the path of the endpoint's resource. E.g. from
//				 * </readings/temp> it will select /readings/temp.
//				 */
//				String path = "", pathTemp = "";
//				while ((pathTemp = scanner.findInLine("</.*?>")) != null) {
//					path = pathTemp.substring(1, pathTemp.length() - 1);
//				}
//				LocalResource resource = addNodeResource(path);
//
//				/*
//				 * Since created the subResource, get all the attributes from
//				 * the payload. Each parameter is separated by a ";".
//				 */
//				scanner.useDelimiter(";");
//				while (scanner.hasNext()) {
//					LinkAttribute attrib = LinkAttribute.parse(scanner.next());
//					resource.setAttribute(attrib.getName(), attrib.getValue());
//					// attr = LinkAttribute.parse(scanner.next());
//					// String name = attr.getName();
//					// if(name.equals(LinkFormat.RESOURCE_TYPE)){resource.setResourceType(attr.getStringValue());}
//					// if(name.equals(LinkFormat.INSTANCE)){resource.setInstance(attr.getStringValue());}
//					// if(name.equals(LinkFormat.DOMAIN)){resource.setDomain(attr.getStringValue());}
//					// if(name.equals(LinkFormat.CONTEXT)){resource.setContext(attr.getStringValue());}
//				}
//				resource.setAttribute(LinkFormat.END_POINT, getName());
//			}
//		}
//
//		// public void setEtag(String etag){
//		// //TODO for validation purpose
//		// this.etag=etag;
//		// }
//
//		class Task extends TimerTask {
//			ResourceNode resource;
//
//			public Task(ResourceNode resource) {
//				super();
//				this.resource = resource;
//			}
//
//			@Override
//			public void run() {
//				resource.remove();
//			}
//		}
//	}
}

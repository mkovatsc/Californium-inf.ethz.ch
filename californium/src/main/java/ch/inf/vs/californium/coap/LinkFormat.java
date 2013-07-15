package ch.inf.vs.californium.coap;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import ch.inf.vs.californium.resources.CalifonriumLogger;
import ch.inf.vs.californium.resources.Resource;
import ch.inf.vs.californium.resources.ResourceAttributes;

/**
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class LinkFormat {

	public final static Logger LOGGER = CalifonriumLogger.getLogger(LinkFormat.class);
	
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
	
//	public static String serializeTree(Resource resource, List<String> queries, boolean recursive) {
//		
//		StringBuilder linkFormat = new StringBuilder();
//		
//		ResourceAttributes attributes = resource.getAttributes();
//		
//		// skip hidden and empty root in recursive mode, always skip non-matching resources
//		if (   (!resource.isHidden() 
//				&& (!resource.getName().equals("") || attributes.getCount()>0) || !recursive) 
//				&& matches(resource, queries)
//				) {
//			
//			LOGGER.finer("Serializing resource link: " + resource.getPath());
//			
//			linkFormat.append("<");
//			linkFormat.append(resource.getPath());
//			linkFormat.append(">");
//			
//			for (String key : attributes.getAttributeKeySet()) {
//				// Make a copy to not depend on thread-safety
//				List<String> values = attributes.getAttributeValues(key);
//				if (values == null) continue;
//				linkFormat.append(';');
//				linkFormat.append(serializeAttribute(key,  new LinkedList<>(values)));
//			}
//		}
//		
//		if (recursive) {
//			// Loop over all sub-resources
//			for (Resource sub : resource.getChildren()) {
//
//				String next = LinkFormat.serializeTree(sub, queries, true);
//				
//				// delimiter
//				if (!next.equals("")) {
//					if (linkFormat.length()>3) linkFormat.append(',');
//					linkFormat.append(next);
//				}
//			}
//		}
//		
//		return linkFormat.toString();
//	}
	
	public static String serializeAttributes(Resource resource) {
		ResourceAttributes attributes = resource.getAttributes();
		StringBuffer buffer = new StringBuffer();
		for (String attr:attributes.getAttributeKeySet()) {
			List<String> values = attributes.getAttributeValues(attr);
			if (values == null) continue;
			buffer.append(";");
			// Make a copy to not  depend on thread-safety
			buffer.append(serializeAttribute(attr, new LinkedList<>(values)));
		}
		return buffer.toString();
	}
	
	public static String serializeAttribute(String key, List<String> values) {
		
		String delimiter = "=";
		
		StringBuilder linkFormat = new StringBuilder();
		boolean quotes = false;
		
		linkFormat.append(key);
		
		if (values==null) {
			throw new RuntimeException("Values null");
		}
		
		if (values.isEmpty() || values.get(0).equals("")) 
			return linkFormat.toString();
		
		linkFormat.append(delimiter);
		
		if (values.size()>1 || !values.get(0).matches("^[0-9]+$")) {
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
	
	public static boolean matches(Resource resource, List<String> queries) {
		
		if (resource==null) return false;
		if (queries==null || queries.size()==0) return true;
		
		ResourceAttributes attributes = resource.getAttributes();
		String path = resource.getPath()+resource.getName();
		
		for (String s : queries) {
			int delim = s.indexOf("=");
			if (delim != -1) {
				
				// split name-value-pair
				String attrName = s.substring(0, delim);
				String expected = s.substring(delim+1);

				if (attrName.equals(LinkFormat.LINK)) {
					if (expected.endsWith("*")) {
						return path.startsWith(expected.substring(0, expected.length()-1));
					} else {
						return path.equals(expected);
					}
				} else if (attributes.containsAttribute(attrName)) {
					// lookup attribute value
					for (String actual : attributes.getAttributeValues(attrName)) {
					
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
				if (attributes.getAttributeValues(s).size()>0) {
					return true;
				}
			}
		}
		return false;
	}
}

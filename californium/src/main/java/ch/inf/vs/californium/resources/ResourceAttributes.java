package ch.inf.vs.californium.resources;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ch.inf.vs.californium.coap.LinkFormat;

public class ResourceAttributes {

	/** Contains the resource's attributes specified in the CoRE Link Format. */
	private final ConcurrentMap<String, AttributeValues> attributes;
	
	public ResourceAttributes() {
		attributes = new ConcurrentHashMap<>();
	}
	
	public int getCount() {
		return attributes.size();
	}
	
	public String getTitle() {
		return attributes.get(LinkFormat.TITLE).getFirst();
	}

	public void setTitle(String title) {
		findAttributeValues(LinkFormat.TITLE).setOnly(title);
	}
	
	public void addResourceType(String type) {
		findAttributeValues(LinkFormat.RESOURCE_TYPE).add(type);
	}
	
	public void clearResourceType() {
		attributes.remove(LinkFormat.RESOURCE_TYPE);
	}
	
	public List<String> getResourceTypes() {
		return getAttributeValues(LinkFormat.RESOURCE_TYPE);
	}
	
	public void addInterfaceDescription(String description) {
		findAttributeValues(LinkFormat.INTERFACE_DESCRIPTION).add(description);
	}
	
	public List<String> getInterfaceDescriptions() {
		return getAttributeValues(LinkFormat.INTERFACE_DESCRIPTION);
	}
	
	public void setMaximumSizeEstimate(String size) {
		findAttributeValues(LinkFormat.MAX_SIZE_ESTIMATE).setOnly(size);
	}
	
	public void setMaximumSizeEstimate(int size) {
		findAttributeValues(LinkFormat.MAX_SIZE_ESTIMATE).setOnly(Integer.toString(size));
	}
	
	public String getMaximumSizeEstimate() {
		return findAttributeValues(LinkFormat.MAX_SIZE_ESTIMATE).getFirst();
	}
	
	public void addContentType(String type) {
		findAttributeValues(LinkFormat.CONTENT_TYPE).add(type);
	}
	
	public void addContentType(int type) {
		findAttributeValues(LinkFormat.CONTENT_TYPE).add(Integer.toString(type));
	}
	
	public List<String> getContentTypes() {
		return getAttributeValues(LinkFormat.CONTENT_TYPE);
	}
	
	public void clearContentType() {
		attributes.remove(LinkFormat.CONTENT_TYPE);
	}
	
	public void setObservable() {
		findAttributeValues(LinkFormat.OBSERVABLE).setOnly("");
	}
	
	public boolean hasObservable() {
		return getAttributeValues(LinkFormat.OBSERVABLE).isEmpty();
	}
	
	public void setAttribute(String attr, String value) {
		findAttributeValues(attr).setOnly(value);
	}
	
	public void addAttribute(String attr) {
		addAttribute(attr, "");
	}
	
	public void addAttribute(String attr, String value) {
		findAttributeValues(attr).add(value);
	}
	
	public void clearAttribute(String attr) {
		attributes.remove(attr);
	}
	
	public boolean containsAttribute(String attr) {
		return attributes.containsKey(attr);
	}
	
	public Set<String> getAttributeKeySet() {
		return attributes.keySet();
	}
	
	public List<String> getAttributeValues(String attr) {
		AttributeValues list = attributes.get(attr);
		if (list != null) return list.getAll();
		else return Collections.emptyList();
	}
	
	private AttributeValues findAttributeValues(String attr) {
		AttributeValues list = attributes.get(attr);
		if (list == null) {
			list = new AttributeValues();
			AttributeValues prev = attributes.putIfAbsent(attr, list);
			if (prev != null) return prev;
		}
		return list;
	}
	
	private final static class AttributeValues {
		 
		private final List<String> list = 
				Collections.synchronizedList(new LinkedList<String>());
		
		private List<String> getAll() {
			return list;
		}
		
		private void add(String value) {
			list.add(value);
		}
		
		private synchronized String getFirst() {
			if (list.isEmpty()) return null;
			else return list.get(0);
		}
		
		private synchronized void setOnly(String value) {
			list.clear();
			if (value != null)
				list.add(value);
		}
	}
}

package ch.ethz.inf.vs.californium.rd.resources;

import java.util.HashMap;
import java.util.HashSet;

import ch.ethz.inf.vs.californium.server.resources.ResourceBase;

public class RDTagResource extends ResourceBase {

	private HashMap<String, String> tagsMap;
	private RDNodeResource parentNode;
	
	public RDTagResource(String resourceIdentifier, boolean hidden, RDNodeResource parent) {
		super(resourceIdentifier, hidden);
		tagsMap = new HashMap<String,String>();
		parentNode = parent;
		
	}
	
	public boolean containsTag(String tag, String value){
		if(tagsMap.containsKey(tag.toLowerCase())){
			return tagsMap.get(tag.toLowerCase()).equals(value.toLowerCase());
		}
		return false;
	}
	
	public boolean containsMultipleTags(HashMap<String, String> tags){
		for(String tag : tags.keySet()){
			if(!containsTag(tag, tags.get(tag))){
				return false;
			}
		}
		return true;
	}
	
//	public HashSet<String> getTags(){
//		return tagsSet;
//	}
	
	public HashMap<String, String> getTags(){
		return tagsMap;
	}
	
	
	
	
	public void addTag(String tag, String value){
		tagsMap.put(tag.toLowerCase(), value.toLowerCase());
	}
	
	
	
	public void addMultipleTags(HashMap<String, String> tags){
		for(String tag : tags.keySet()){
			addTag(tag, tags.get(tag));			
		}
	}
	
	
	public void removeMultipleTags(HashSet<String> tags){
		for(String tag : tags){
			tagsMap.remove(tag.toLowerCase());
		}
	}
	
	public RDNodeResource getParentNode(){
		return parentNode;
	}

}

package ch.ethz.inf.vs.californium.endpoint.resources;

import java.util.HashSet;
import java.util.Set;

public class RDTagResource extends LocalResource {

	private HashSet<String> tagsSet;
	private RDNodeResource parentNode;
	
	public RDTagResource(String resourceIdentifier, boolean hidden, RDNodeResource parent) {
		super(resourceIdentifier, hidden);
		tagsSet = new HashSet<String>();
		parentNode = parent;
		
	}
	
	public boolean containsTag(String tag){
		return tagsSet.contains(tag.toLowerCase());
	}
	
	public boolean containsMultipleTags(HashSet<String> tags){
		for(String tag : tags){
			if(!tagsSet.contains(tag.toLowerCase())){
				return false;
			}
		}
		return true;
	}

	public HashSet<String> getTags(){
		return tagsSet;
	}
	
	public void addTag(String tag){
		tagsSet.add(tag.toLowerCase());
	}
	
	public void addMultipleTags(HashSet<String> tags){
		for(String tag : tags){
			tagsSet.add(tag.toLowerCase());
		}
	}
	
	public void removeMultipleTags(HashSet<String> tags){
		for(String tag : tags){
			tagsSet.remove(tag.toLowerCase());
		}
	}
	
	public RDNodeResource getParentNode(){
		return parentNode;
	}

}

package ch.ethz.inf.vs.californium.endpoint.resource;

import ch.ethz.inf.vs.californium.endpoint.resources.LocalResource;

public class TagTopResource extends LocalResource{
	
	public TagTopResource(){
		this("tag");
	}
	
	public TagTopResource(String identifier){
		super(identifier);
		
	}
	
	

}

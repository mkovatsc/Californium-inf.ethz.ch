package ch.ethz.inf.vs.californium.endpoint.resources;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.LinkAttribute;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;


public class RDTagTopResource extends LocalResource {
	
	private RDResource rdResource = null;
	
	public RDTagTopResource(RDResource rd){
		this("tags", rd);
	}

	public RDTagTopResource(String resourceIdentifier, RDResource rd) {
		super(resourceIdentifier);
		this.rdResource = rd;
		
	}
	
	@Override 
	public void performGET(GETRequest request){
		List<Option> query = request.getOptions(OptionNumberRegistry.URI_QUERY);
		HashMap<String, String> tags = new HashMap<String, String>();
		Response response = null;
		String resourcePath="";
		String ep="";
		for(Option option : query){
			if(option.getStringValue().startsWith("ep=")){
				ep=option.getStringValue().substring(option.getStringValue().indexOf("=")+1);
			}
			else if(option.getStringValue().startsWith("res=")){
				resourcePath=option.getStringValue().substring(option.getStringValue().indexOf("=")+1);
			}
			else{
				String tag = option.getStringValue();
				if(tag.contains("=")){
					tags.put(tag.substring(0, tag.indexOf("=")).toLowerCase().trim(),tag.substring(tag.indexOf("=")+1).toLowerCase().trim());
				}
				else{
					tags.put(tag.toLowerCase().trim(),"true");
				}
				
				/*
				if(option.getStringValue().startsWith("tags=")){
				Collections.addAll(tags, option.getStringValue().substring(option.getStringValue().indexOf("=")+1).split(","));
				*/
			}
		}
		if(resourcePath.startsWith("/")){
			resourcePath = resourcePath.substring(1);
		}
		if(!ep.isEmpty() && !resourcePath.isEmpty() && tags.isEmpty()){
			//Get Tags of resource
			RDTagResource target = null;
			for(Resource res : rdResource.getSubResources()){
				if(res.getClass() == RDNodeResource.class){
					if(((RDNodeResource) res).getEndpointIdentifier().equals(ep)){
						if(res.getResource(resourcePath).getClass()== RDTagResource.class){
							target = (RDTagResource) res.getResource(resourcePath);
							break;
						}
					}
				}
			}
			if(target!=null){
				response = new Response(CodeRegistry.RESP_CONTENT);
				String payload="";
				HashMap<String, String> tagMap = target.getTags();
				for(String tag : tagMap.keySet()){
					payload+=tag+"="+tagMap.get(tag)+",";
				}
				if(payload.endsWith(",")){
					payload = payload.substring(0,payload.length()-1);
				}
				response.setPayload(payload);
			}
			else{
				response = new Response(CodeRegistry.RESP_NOT_FOUND);
			}
		}
		else if(!tags.isEmpty() && ep.isEmpty() && resourcePath.isEmpty()){
			//Get resource with specified Tags
			Set<RDTagResource> result = getSubResourceWithTags(tags, rdResource);
			if(result.isEmpty()){
				response = new Response(CodeRegistry.RESP_NOT_FOUND);
			}
			else{
				response = new Response(CodeRegistry.RESP_CONTENT);
				response.setContentType(MediaTypeRegistry.APPLICATION_LINK_FORMAT);
				StringBuilder linkFormat = new StringBuilder();
				for(RDTagResource res : result){
					linkFormat.append("<"+res.getParentNode().getContext());
					linkFormat.append(res.getPath().substring(res.getParentNode().getPath().length()));
					linkFormat.append(">");
					for (LinkAttribute attrib : res.getAttributes()) {
						linkFormat.append(';');
						linkFormat.append(attrib.serialize());
						
					}
					linkFormat.append(",");
				}
				linkFormat.deleteCharAt(linkFormat.length()-1);
				response.setPayload(linkFormat.toString());
			}
		}
		else{
			response = new Response(CodeRegistry.RESP_BAD_REQUEST);	
		}
		request.respond(response);
	}
	
	@Override
	public void performPUT(PUTRequest request){
		String resourcePath="";
		String ep="";
		Resource target = null;
		
		HashMap<String,String> payloadMap = new HashMap<String,String>();
		String[] splittedPayload = request.getPayloadString().split("\n");
		List<Option> query = request.getOptions(OptionNumberRegistry.URI_QUERY);
		
		for(Option option : query){
			if(option.getStringValue().startsWith("ep=")){
				ep=option.getStringValue().substring(option.getStringValue().indexOf("=")+1);
			}
			if(option.getStringValue().startsWith("res=")){
				resourcePath=option.getStringValue().substring(option.getStringValue().indexOf("=")+1);
			}
		}
		for( String line:splittedPayload){
			if(line.contains(":")){
				if(line.charAt(line.indexOf(":")+1) == '{' && line.endsWith("}")){
					payloadMap.put(line.substring(0,line.indexOf(":")),line.substring(line.indexOf(":")+2,line.length()-1));
				}
				else{
					request.respond(CodeRegistry.RESP_BAD_REQUEST);
					return;
				}
			}
			else{
				request.respond(CodeRegistry.RESP_BAD_REQUEST);
				return;
			}
		}
		System.out.println(ep);
		System.out.println(resourcePath);
		if((!payloadMap.containsKey("add") && !payloadMap.containsKey("delete") )|| ep.isEmpty() || resourcePath.isEmpty()){
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
			return;
		}
		if(resourcePath.startsWith("/")){
			resourcePath = resourcePath.substring(1);
		}
		for(Resource res : rdResource.getSubResources()){
			if(res.getClass() == RDNodeResource.class){
				if(((RDNodeResource) res).getEndpointIdentifier().equals(ep)){
					target = res.getResource(resourcePath);
				}
			}
		}
		if(target==null || target.getClass()!=RDTagResource.class){
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
			return;
		}
		if(payloadMap.containsKey("delete")){
			HashSet<String> tags = new HashSet<String>();
			for(String tag :payloadMap.get("delete").split(",")){
				if(tag.contains("=")){
					tags.add(tag.substring(0, tag.indexOf("=")).toLowerCase().trim());
				}
				else{
					tags.add(tag.toLowerCase().trim());
				}
			}
			((RDTagResource) target).removeMultipleTags(tags);
		}
		if(payloadMap.containsKey("add")){
			HashMap<String, String> tags = new HashMap<String,String>();
			for(String tag : payloadMap.get("add").split(",")){
				if(tag.contains("=")){
					tags.put(tag.substring(0, tag.indexOf("=")).toLowerCase().trim(), tag.substring(tag.indexOf("=")+1).toLowerCase().trim());
				}
				else{
					tags.put(tag.toLowerCase().trim(), "true");
				}
				
			}
			((RDTagResource) target).addMultipleTags(tags);
		}
		request.respond(CodeRegistry.RESP_CHANGED);
	
	}
	
	private Set<RDTagResource> getSubResourceWithTags(HashMap<String, String> tags, Resource start){
		LinkedList<Resource> toDo = new LinkedList<Resource>();
		toDo.add(start);
		HashSet<RDTagResource> result = new HashSet<RDTagResource>();
		while(!toDo.isEmpty()){
			Resource current = toDo.pop();
			if(current.getClass() == RDTagResource.class){
				if(((RDTagResource) current).containsMultipleTags(tags)){
					result.add((RDTagResource)current);
				}
			}
			toDo.addAll(current.getSubResources());
		}
		return result;
	}
}

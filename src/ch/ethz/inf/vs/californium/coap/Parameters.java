package ch.ethz.inf.vs.californium.coap;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Represents a list of query parameters
 * 
 * @author Julian Kornberger
 *
 */
public class Parameters {
	
	public static final String NAME_VALUE_SEPARATOR = "=";
	public static final int OPTION_NUMBER = OptionNumberRegistry.URI_QUERY;
	
	/**
	 * A parameter represented by its name and value 
	 */
	public class Parameter{
		private String name;
		private String value;
		
		public Parameter(String name, String value){
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public String getValue() {
			return value;
		}
	}
	
	public final List<Parameter> parameters = new Vector<Parameter>();
	
	/**
	 * Creates a list of query parameters by parsing the request options.
	 * 
	 * @param message
	 */
	public Parameters(Message message) {
		for(Option option : message.getOptions(OPTION_NUMBER)){
			String string = option.getStringValue();
			int pos = string.indexOf(NAME_VALUE_SEPARATOR);
			Parameter parameter;
			
			if(pos == -1){
				parameter = new Parameter(string, null);
			}else{
				parameter = new Parameter(string.substring(0, pos), string.substring(pos+1));
			}
			
			parameters.add(parameter);
		}
	}
	
	/**
	 * Returns the number of query parameters.
	 * 
	 * @return
	 */
	public int size(){
		return parameters.size();
	}
	
	/**
	 * Returns the value of a request parameter as a String, or null if the parameter does not exist.
	 * 
	 * @param name
	 * @return
	 */
	public String getValue(String name){
		for(Parameter parameter : parameters){
			if(parameter.name.equals(name))
				return parameter.value;
		}
		return null;
	}
	
	/**
	 * Returns an List of String objects containing the names of the parameters contained in this request.
	 * 
	 * @param name
	 * @return
	 */
	public List<String> getValues(String name){
		List<String> result = new ArrayList<String>();
		
		for(Parameter parameter : parameters){
			if(parameter.name.equals(name))
				result.add(parameter.getValue());
		}

		return result;
	}

}

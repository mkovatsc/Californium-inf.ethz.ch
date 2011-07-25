package coap;

import java.util.Scanner;
import java.util.regex.Pattern;

import util.Log;

/*
 * This class provides link format definitions as specified in
 * draft-ietf-core-link-format-06
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class LinkFormat {

	public static final String RESOURCE_TYPE         = "rt";
	public static final String INTERFACE_DESCRIPTION = "if";
	public static final String CONTENT_TYPE          = "ct";
	public static final String MAX_SIZE_ESTIMATE     = "sz";
	public static final String TITLE                 = "title";
	public static final String OBSERVABLE            = "obs";
	
	public static final Pattern ATTRIBUTE_NAME       = Pattern.compile("\\w+");
	public static final Pattern QUOTED_STRING        = Pattern.compile("\\G\".*?\"");
	public static final Pattern CARDINAL             = Pattern.compile("\\G\\d+");
	public static final Pattern SEPARATOR            = Pattern.compile("\\s*;*\\s*");
	public static final Pattern DELIMITER            = Pattern.compile(",");
	
	public static class Attribute {
		
		public Attribute() {
			
		}
		
		public Attribute(String name, Object value) {
			this.name = name;
			this.value = value;
		}
		
		public static Attribute parse(Scanner scanner) {
			
			String name = scanner.findInLine(ATTRIBUTE_NAME);
			if (name != null) {
				
				Attribute attr = new Attribute();
				attr.name = name;
				
				scanner.skip("\\s*"); // skip whitespaces, if any
				
				// check for name-value-pair
				if (scanner.findWithinHorizon("=", 1) != null) {
					
					String value = null;
					if ((value = scanner.findInLine(QUOTED_STRING)) != null) {
						attr.value = value.substring(1, value.length()-1); // trim " "
					} else if ((value = scanner.findInLine(CARDINAL)) != null) {
						attr.value = Integer.parseInt(value);
					} else if (scanner.hasNext()){
						attr.value = scanner.next();
					} else {
						attr.value = null;
					}
					
				} else {
					// flag attribute
					attr.value = true;
				}
				
				return attr;
			}
			return null;
		}
		
		public void serialize(StringBuilder builder) {
			
			// check if there's something to write
			if (name != null && value != null) {
				
				if (value instanceof Boolean) {
					
					// flag attribute
					if ((Boolean)value) {
						builder.append(name);
					}
					
				} else {
					
					// name-value-pair
					builder.append(name);
					builder.append('=');
					if (value instanceof String) {
						builder.append('"');
						builder.append((String)value);
						builder.append('"');
					} else if (value instanceof Integer) {
						builder.append(((Integer)value));
					} else {
						Log.error(this, "Serializing attribute of unexpected type: %s (%s)",
							name, value.getClass().getName());
						builder.append(value);
					}
				}
			}
		}
		
		public static Attribute parse(String str) {
			return parse(new Scanner(str));
		}
		
		public String name() {
			return name;
		}
		
		public Object value() {
			return value;
		}
		
		@Override
		public String toString() {
			return String.format("name: %s value: %s", name, value);
		}
		
		public int getIntValue() {
			if (value instanceof Integer) {
				return (Integer)value;
			}
			return -1;
		}
		
		public String getStringValue() {
			if (value instanceof String) {
				return (String)value;
			}
			return null;
		}
		
		private String name;
		private Object value;
	}
	
}

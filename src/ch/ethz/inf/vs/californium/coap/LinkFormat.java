/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.coap;

import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class provides link format definitions as specified in
 * draft-ietf-core-link-format-06
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class LinkFormat {

// Logging /////////////////////////////////////////////////////////////////////
	
	protected static final Logger LOG = Logger.getLogger(LinkFormat.class.getName());

// Constants ///////////////////////////////////////////////////////////////////

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
						LOG.severe(String.format("Serializing attribute of unexpected type: %s (%s)",name, value.getClass().getName()));
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

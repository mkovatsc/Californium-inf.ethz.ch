package ch.ethz.inf.vs.californium.plugtests;

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
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/

import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class implements attributes of the CoRE Link Format.
 * 
 * @author Matthias Kovatsch
 */
public class LinkAttribute {

// Logging /////////////////////////////////////////////////////////////////////
	
	protected static final Logger LOG = Logger.getLogger(LinkAttribute.class.getName());

// Constants ///////////////////////////////////////////////////////////////////

	public static final Pattern SEPARATOR      = Pattern.compile("\\s*;+\\s*");
	public static final Pattern WORD           = Pattern.compile("\\w+");
	public static final Pattern QUOTED_STRING  = Pattern.compile("\\G\".*?\"");
	public static final Pattern CARDINAL       = Pattern.compile("\\G\\d+");
	
// Members /////////////////////////////////////////////////////////////////////
	
	private String name;
	private String value;

// Constructors ////////////////////////////////////////////////////////////////
	
	public LinkAttribute() {
		
	}
	
	public LinkAttribute(String name, String value) {
		this.name = name;
		this.value = value;
	}
	public LinkAttribute(String name, int value) {
		this.name = name;
		this.value = Integer.valueOf(value).toString();
	}
	public LinkAttribute(String name) {
		this.name = name;
		this.value = "";
	}

// Serialization ///////////////////////////////////////////////////////////////
	
	public static LinkAttribute parse(String str) {
		return parse(new Scanner(str));
	}
	
	public static LinkAttribute parse(Scanner scanner) {
		
		String name = scanner.findInLine(WORD);
		if (name != null) {
			
			LOG.finest(String.format("Parsed link attribute: %s", name));
			
			LinkAttribute attr = new LinkAttribute();
			attr.name = name;
			
			// check for name-value-pair
			if (scanner.findWithinHorizon("=", 1) != null) {
				
				String value = null;
				if ((value = scanner.findInLine(QUOTED_STRING)) != null) {
					attr.value = value.substring(1, value.length()-1); // trim " "
				} else if ((value = scanner.findInLine(WORD)) != null) {
					attr.value = value;
				} else if ((value = scanner.findInLine(CARDINAL)) != null) {
					attr.value = value;
				} else if (scanner.hasNext()) {
					attr.value = scanner.next();
					throw new RuntimeException("LinkAttribute scanner.next()");
				}
				
			} else {
				// flag attribute
				attr.value = "";
			}
			
			return attr;
		}
		return null;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
	
	public int getIntValue() {
		return Integer.parseInt(value);
	}

}

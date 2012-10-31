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

package ch.ethz.inf.vs.californium.controller.utility;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This class implements Californium's property registry.
 * 
 * It is used to manage CoAP- and Californium-specific constants in a central
 * place. The properties are initialized in the init() section and can be
 * overridden by a user-defined .properties file. If the file does not exist
 * upon initialization, it will be created so that a valid configuration always
 * exists.
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class Properties extends java.util.Properties {

	private static Logger logger = Logger.getLogger(Properties.class);

	/**
	 * auto-generated to eliminate warning
	 */
	private static final long serialVersionUID = -8883688751651970877L;

	/** The header for Californium property files. */
	private static final String HEADER = "Heating Controller Properties file";

	/** The name of the default properties file. */
	private static final String DEFAULT_FILENAME = "Controller.properties";

	// default properties used by the library
	public static final Properties std = new Properties(DEFAULT_FILENAME);

	public Properties(String fileName) {
		init();
		initUserDefined(fileName);
	}

	public double getDbl(String key) {
		String value = getProperty(key);
		if (value != null) {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				logger.error(String.format("Invalid double property: %s=%s", key, value));
			}
		} else {
			logger.error(String.format("Undefined double property: %s", key));
		}
		return 0.0;
	}

	public int getInt(String key) {
		String value = getProperty(key);
		if (value != null) {
			try {
				return Integer.parseInt(value.trim());
			} catch (NumberFormatException e) {
				logger.error(String.format("Invalid integer property: %s=%s", key, value));
			}
		} else {
			logger.error(String.format("Undefined integer property: %s", key));
		}
		return 0;
	}

	public String getStr(String key) {
		String value = getProperty(key);
		if (value == null) {
			logger.error(String.format("Undefined string property: %s", key));
		}
		return value;
	}

	public HashSet<String> getSensorTypes(){
		
		HashSet<String> set = new HashSet<String>();
		
		String value = getProperty("RESOURCE_TYPES");
		if (value != null) {
			Collections.addAll(set, value.split(","));
		}
		else{
			logger.error(String.format("Undefined string property: %s", "RESOURCE_TYPES"));
		}
		return set;
		
	}
	
	public void load(String fileName) throws IOException {
		InputStream in = new FileInputStream(fileName);
		load(in);
	}

	public void set(String key, double value) {
		setProperty(key, String.valueOf(value));
	}

	public void set(String key, int value) {
		setProperty(key, String.valueOf(value));
	}

	public void set(String key, String value) {
		setProperty(key, value);
	}

	public void store(String fileName) throws IOException {
		OutputStream out = new FileOutputStream(fileName);
		store(out, HEADER);
	}
	
	

	private void init() {

		
		set("RD_ADDRESS", "localhost:5683");
		
		// default CoAP port as defined in draft-ietf-core-coap-05, section 7.1:
		// MUST be supported by a server for resource discovery and
		// SHOULD be supported for providing access to other resources.
		set("DEFAULT_PORT", 5683);

		set("PIR_TEMPERATURE", 21.0);
		
		set("MIN_TEMPERATURE", 17.0);
		
		set("TOLERANCE",0.5);
		
		set("COEFFICIENT_DEFAULT",0.05);
		
		set("SCHEDULE_FILE", "schedule.txt");
		
		set("COEFFICIENT_FILE", "coefficient.txt");
		
		set("RESOURCE_TYPES", "temperature,valve");
		
		
		
	}

	private void initUserDefined(String fileName) {
		try {
			load(fileName);
		} catch (IOException e) {
			// file does not exist:
			// write default properties
			try {
				store(fileName);
			} catch (IOException e1) {
				logger.warn(String.format("Failed to create configuration file: %s", e1.getMessage()));
			}
		}
	}

}

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
package ch.ethz.inf.vs.persistingservice.parser;

import java.util.HashMap;

/**
 * The Class PayloadParser is used to parse the payload read from the coap
 * requests. 
 * <p>
 * It manages a hashmap of the payload labels and provides some useful
 * methods to access the structured payload.
 */
public class PayloadParser {

	/**
	 * The payload map structures the payload, storing the labels with their
	 * corresponding content.
	 */
	private HashMap<String, String> payloadMap = new HashMap<String, String>();
	
	/**
	 * Instantiates a new payload parser and builds the hashmap corresponding to the payload.
	 *
	 * @param payload the payload read from a coap request.
	 */
	public PayloadParser(String payload) {
		if (payload.equals("")) return;
		
		String tmp = payload.replaceAll(" ", "");
		String[] parameters = tmp.split("\n");
		
		for (int i = 0; i<parameters.length; i++) {
			payloadMap.put(parameters[i].substring(0, parameters[i].indexOf("=")), parameters[i].substring(parameters[i].indexOf("=") + 1, parameters[i].length()));
		}
	}
	
	/**
	 * Not null checks, if the payload has elements and was parsed successfully.
	 *
	 * @return true, if the payload map is not empty.
	 */
	public boolean notNull() {
		return !payloadMap.isEmpty();
	}
	
	/**
	 * Gets the string value for some label.
	 *
	 * @param label the label
	 * @return the string value stored for the label.
	 */
	public String getStringValue(String label) {
		return payloadMap.get(label);
	}
	
	/**
	 * Gets the int value for some label
	 *
	 * @param label the label
	 * @return the int value stored for the label.
	 */
	public int getIntValue(String label) {
		return Integer.valueOf(payloadMap.get(label));
	}
	
	/**
	 * Gets the boolean value for some label.
	 *
	 * @param label the label
	 * @return the boolean value stored for the label.
	 */
	public boolean getBooleanValue(String label) {
		return Boolean.parseBoolean(payloadMap.get(label));
	}
	
	/**
	 * Checks for exact labels of the payload.
	 *
	 * @param num the num
	 * @return true, if the payload has the required number of labels.
	 */
	public boolean hasExactLabels(int num) {
		return payloadMap.size() == num;
	}
	
	/**
	 * Checks for min labels of the payload.
	 *
	 * @param num the num
	 * @return true, if the payload has the minimum number of labels.
	 */
	public boolean hasMinLabels(int num) {
		return payloadMap.size() >= num;
	}
	
	/**
	 * Checks for max labels of the payload.
	 *
	 * @param num the num
	 * @return true, if the payload has the maximum number of labels.
	 */
	public boolean hasMaxLabels(int num) {
		return payloadMap.size() <= num;
	}
	
	/**
	 * Contains label checks if the payload contains the specified label.
	 *
	 * @param label the label
	 * @return true, if the payload contains the specified label.
	 */
	public boolean containsLabel(String label) {
		return payloadMap.containsKey(label);
	}
	
	/**
	 * Contains labels checks if the payload contains the specified labels.
	 *
	 * @param labels the labels
	 * @return true, if the payload contains the specified labels.
	 */
	public boolean containsLabels(String... labels) {
		for (String label : labels) {
			if (!payloadMap.containsKey(label)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Contains exact labels checks if the payload only holds the specified labels.
	 *
	 * @param labels the labels
	 * @return true, if the payload only holds the specified labels.
	 */
	public boolean containsExactLabels(String... labels) {
		int counter = 0;
		
		for (String label : labels) {
			if (!payloadMap.containsKey(label)) {
				return false;
			}
			counter++;
		}
		
		if (payloadMap.size() == counter) {
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if the value corresponding to the label is a boolean.
	 *
	 * @param label the label
	 * @return true, if the value corresponding to the label is a boolean.
	 */
	public boolean isBoolean(String label) {
		if (payloadMap.get(label).equals("true") || payloadMap.get(label).equals("false")){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Checks if the value corresponding to the label is an integer.
	 *
	 * @param label the label
	 * @return true, if the value corresponding to the label is an integer.
	 */
	public boolean isInteger(String label) {
		char[] num = payloadMap.get(label).toCharArray();
		for (int i=0; i<num.length; i++) {
			if (!Character.isDigit(num[i])) {
				return false;
			}
		}
		return true;
	}

}

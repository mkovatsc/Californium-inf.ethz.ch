package ch.ethz.inf.vs.californium.coap;


/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
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


/**
 * This class describes the CoAP Option Number Registry as defined in
 * draft-ietf-core-coap-12, sections 12.2 and 5.10
 * 
 * @author Dominique Im Obersteg, Daniel Pauli and Francesco Corazza
 * @version 0.1
 * 
 */
public final class OptionNumberRegistry {
	
	public static final int DEFAULT_MAX_AGE = 60;
	
	// draft-ietf-core-coap-13
	public static final int RESERVED_0 = 0;
	public static final int IF_MATCH = 1;
	public static final int URI_HOST = 3;
	public static final int ETAG = 4;
	public static final int IF_NONE_MATCH = 5;
	public static final int URI_PORT = 7;
	public static final int LOCATION_PATH = 8;
	public static final int URI_PATH = 11;
	public static final int CONTENT_TYPE = 12;
	public static final int MAX_AGE = 14;
	public static final int URI_QUERY = 15;
	public static final int ACCEPT = 16;
	public static final int LOCATION_QUERY = 20;
	public static final int PROXY_URI = 35;
	public static final int PROXY_SCHEME = 39;

	// draft-ietf-core-observe-07
	public static final int OBSERVE = 6;

	// draft-ietf-core-block-08
	public static final int BLOCK2 = 23;
	public static final int BLOCK1 = 27;
	public static final int SIZE = 28;

	// derived constant
	public static final int TOKEN_LEN = 8;
	public static final int ETAG_LEN = 8;

	/**
	 * Returns the option format based on the option number.
	 * 
	 * @param optionNumber
	 *            The option number
	 * @return The option format corresponding to the option number
	 */
	public static optionFormats getFormatByNr(int optionNumber) {
		switch (optionNumber) {
		case CONTENT_TYPE:
		case MAX_AGE:
		case URI_PORT:
		case OBSERVE:
		case BLOCK2:
		case BLOCK1:
		case SIZE:
		case IF_NONE_MATCH:
		case ACCEPT:
			return optionFormats.INTEGER;
		case URI_HOST:
		case URI_PATH:
		case URI_QUERY:
		case LOCATION_PATH:
		case LOCATION_QUERY:
		case PROXY_URI:
		case PROXY_SCHEME:
			return optionFormats.STRING;
		case ETAG:
		case IF_MATCH:
			return optionFormats.OPAQUE;
		default:
			return optionFormats.UNKNOWN;
		}
	}

	/**
	 * Checks whether an option is critical.
	 * 
	 * @param optionNumber
	 *            The option number to check
	 * @return True iff the option is critical
	 */
	public static boolean isCritical(int optionNumber) {
		return (optionNumber & 1) > 1;
	}

	/**
	 * Checks whether an option is elective.
	 * 
	 * @param optionNumber
	 *            The option number to check
	 * @return True iff the option is elective
	 */
	public static boolean isElective(int optionNumber) {
		return (optionNumber & 1) == 0;
	}

	/**
	 * Checks whether an option is unsafe.
	 * 
	 * @param optionNumber
	 *            The option number to check
	 * @return <code>true</code> iff the option is unsafe
	 */
	public static boolean isUnsafe(int optionNumber) {
		// When bit 6 is 1, an option is Unsafe
		return (optionNumber & 2) > 0;
	}
	
	/**
	 * Checks whether an option is safe.
	 * 
	 * @param optionNumber
	 *            The option number to check
	 * @return <code>true</code> iff the option is safe
	 */
	public static boolean isSafe(int optionNumber) {
		return !isUnsafe(optionNumber);
	}

	/**
	 * Checks whether an option is not a cache-key.
	 * 
	 * @param optionNumber
	 *            The option number to check
	 * @return <code>true</code> iff the option is not a cache-key
	 */
	public static boolean isNoCacheKey(int optionNumber) {
		/*
		 * When an option is not Unsafe, it is not a Cache-Key (NoCacheKey) if
		 * and only if bits 3-5 are all set to 1; all other bit combinations
		 * mean that it indeed is a Cache-Key
		 */
		return (optionNumber & 0x1E) == 0x1C;
	}
	
	/**
	 * Checks whether an option is a cache-key.
	 * 
	 * @param optionNumber
	 *            The option number to check
	 * @return <code>true</code> iff the option is a cache-key
	 */
	public static boolean isCacheKey(int optionNumber) {
		return !isNoCacheKey(optionNumber);
	}

	/**
	 * Checks if is single value.
	 * 
	 * @param optionNumber
	 *            the option number
	 * @return true, if is single value
	 */
	public static boolean isSingleValue(int optionNumber) {
		switch (optionNumber) {
		case CONTENT_TYPE:
		case MAX_AGE:
		case PROXY_URI:
		case PROXY_SCHEME:
		case URI_HOST:
		case URI_PORT:
		case IF_NONE_MATCH:
			return true;
		case ETAG:
		case ACCEPT:
		case IF_MATCH:
		case URI_PATH:
		case URI_QUERY:
		case LOCATION_PATH:
		case LOCATION_QUERY:
		default:
			return false;
		}
	}

	/**
	 * Checks if is uri option.
	 * 
	 * @param optionNumber
	 *            the option number
	 * @return true, if is uri option
	 */
	public static boolean isUriOption(int optionNumber) {
		boolean result = optionNumber == URI_HOST || optionNumber == URI_PATH || optionNumber == URI_PORT || optionNumber == URI_QUERY;
		return result;
	}

	/**
	 * Returns a string representation of the option number.
	 * 
	 * @param code
	 *            The option number to describe
	 * @return A string describing the option number
	 */
	public static String toString(int optionNumber) {
		switch (optionNumber) {
		case RESERVED_0:
			return "Reserved (0)";
		case CONTENT_TYPE:
			return "Content-Type";
		case MAX_AGE:
			return "Max-Age";
		case PROXY_URI:
			return "Proxy-Uri";
		case ETAG:
			return "ETag";
		case URI_HOST:
			return "Uri-Host";
		case LOCATION_PATH:
			return "Location-Path";
		case URI_PORT:
			return "Uri-Port";
		case LOCATION_QUERY:
			return "Location-Query";
		case URI_PATH:
			return "Uri-Path";
		case OBSERVE:
			return "Observe";
		case ACCEPT:
			return "Accept";
		case IF_MATCH:
			return "If-Match";
		case URI_QUERY:
			return "Uri-Query";
		case BLOCK2:
			return "Block2";
		case BLOCK1:
			return "Block1";
		case SIZE:
			return "Size";
		case IF_NONE_MATCH:
			return "If-None-Match";
		case PROXY_SCHEME:
			return "Proxy-Scheme";
		default:
			return String.format("Unknown option [%d]", optionNumber);
		}
	}

	private OptionNumberRegistry() {
	}

	/**
	 * The Enum optionFormats.
	 */
	public static enum optionFormats {
		INTEGER, STRING, OPAQUE, UNKNOWN
	}

}

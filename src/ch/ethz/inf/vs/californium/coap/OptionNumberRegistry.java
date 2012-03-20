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
package ch.ethz.inf.vs.californium.coap;

/*
 * This class describes the CoAP Option Number Registry 
 * as defined in draft-ietf-core-coap-07, 
 * sections 11.2 and 5.4.5
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class OptionNumberRegistry {

	// Constants ///////////////////////////////////////////////////////////////

	public static final int RESERVED_0     = 0;

	public static final int CONTENT_TYPE   = 1;
	public static final int MAX_AGE        = 2;
	public static final int PROXY_URI      = 3;
	public static final int ETAG           = 4;
	public static final int URI_HOST       = 5;
	public static final int LOCATION_PATH  = 6;
	public static final int URI_PORT       = 7;
	public static final int LOCATION_QUERY = 8;
	public static final int URI_PATH       = 9;
	public static final int OBSERVE        = 10; // draft-ietf-core-observe
	public static final int TOKEN          = 11;
	public static final int ACCEPT         = 12;
	//public static final int BLOCK          = 13; // deprecated
	public static final int IF_MATCH       = 13;
	public static final int URI_QUERY      = 15;
	public static final int BLOCK2         = 17; // draft-ietf-core-block
	public static final int BLOCK1         = 19; // draft-ietf-core-block
	public static final int IF_NONE_MATCH  = 21;

	public static final int FENCEPOST_DIVISOR = 14;
	
	public static final int TOKEN_LEN = 8;

	// Formats
	// ///////////////////////////////////////////////////////////////////

	public static enum optionFormats {
		INTEGER,
		STRING,
		OPAQUE,
		UNKNOWN,
		ERROR
	}

	// Static Functions ////////////////////////////////////////////////////////

	/*
	 * Checks whether an option is elective
	 * 
	 * @param optionNumber The option number to check
	 * 
	 * @return True iff the option is elective
	 */
	public static boolean isElective(int optionNumber) {
		return (optionNumber & 1) == 0;
	}

	/*
	 * Checks whether an option is critical
	 * 
	 * @param optionNumber The option number to check
	 * 
	 * @return True iff the option is critical
	 */
	public static boolean isCritical(int optionNumber) {
		return (optionNumber & 1) == 1;
	}

	/*
	 * Checks whether an option is a fencepost option
	 * 
	 * @param optionNumber The option number to check
	 * 
	 * @return True iff the option is a fencepost option
	 */
	public static boolean isFencepost(int optionNumber) {
		return optionNumber % FENCEPOST_DIVISOR == 0;
	}

	/*
	 * Returns the next fencepost option number following a given option number
	 * 
	 * @param optionNumber The option number
	 * 
	 * @return The smallest fencepost option number larger than the given option
	 * number
	 */
	public static int nextFencepost(int optionNumber) {
		return (optionNumber / FENCEPOST_DIVISOR + 1) * FENCEPOST_DIVISOR;
	}

	/*
	 * Returns a string representation of the option number
	 * 
	 * @param code The option number to describe
	 * 
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
		case TOKEN:
			return "Token";
		//case BLOCK:
		//	return "Block";
		case ACCEPT:
			return"Accept";
		case IF_MATCH:
			return "If-Match";
		case URI_QUERY:
			return "Uri-Query";
		case BLOCK2:
			return "Block2";
		case BLOCK1:
			return "Block1";
		case IF_NONE_MATCH:
			return "If-None-Match";
		}
		return String.format("Unknown option [number %d]", optionNumber);
	}

	/*
	 * Returns the option format based on the option number
	 * 
	 * @param optionNumber The option number
	 * 
	 * @return The option format corresponding to the option number
	 */
	public static optionFormats getFormatByNr(int optionNumber) {
		switch (optionNumber) {
		case RESERVED_0:
			return optionFormats.UNKNOWN;
		case CONTENT_TYPE:
			return optionFormats.INTEGER;
		case PROXY_URI:
			return optionFormats.STRING;
		case ETAG:
			return optionFormats.OPAQUE;
		case URI_HOST:
			return optionFormats.STRING;
		case LOCATION_PATH:
			return optionFormats.STRING;
		case URI_PORT:
			return optionFormats.INTEGER;
		case LOCATION_QUERY:
			return optionFormats.STRING;
		case URI_PATH:
			return optionFormats.STRING;
		case TOKEN:
			return optionFormats.OPAQUE;
		case URI_QUERY:
			return optionFormats.STRING;
		default:
			return optionFormats.ERROR;
		}
	}

}

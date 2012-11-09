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

/**
 * This class describes the CoAP Code Registry as defined in 
 * draft-ietf-core-coap-08, section 11.1
 * 
 * @author Dominique Im Obersteg, Daniel Pauli, and Matthias Kovatsch
 */
public class CodeRegistry {

// Constants ///////////////////////////////////////////////////////////////////

	public static final int EMPTY_MESSAGE = 0;

// CoAP method codes ///////////////////////////////////////////////////////////
	
	public static final int METHOD_GET = 1;
	public static final int METHOD_POST = 2;
	public static final int METHOD_PUT = 3;
	public static final int METHOD_DELETE = 4;

// CoAP response codes /////////////////////////////////////////////////////////
	
	public static final int CLASS_SUCCESS = 2;
	public static final int CLASS_CLIENT_ERROR = 4;
	public static final int CLASS_SERVER_ERROR = 5;

	// class 2.xx
	public static final int RESP_CREATED = 65;
	public static final int RESP_DELETED = 66;
	public static final int RESP_VALID = 67;
	public static final int RESP_CHANGED = 68;
	public static final int RESP_CONTENT = 69;

	// class 4.xx
	public static final int RESP_BAD_REQUEST = 128;
	public static final int RESP_UNAUTHORIZED = 129;
	public static final int RESP_BAD_OPTION = 130;
	public static final int RESP_FORBIDDEN = 131;
	public static final int RESP_NOT_FOUND = 132;
	public static final int RESP_METHOD_NOT_ALLOWED = 133;
	public static final int RESP_NOT_ACCEPTABLE = 134;
	public static final int RESP_PRECONDITION_FAILED = 140;
	public static final int RESP_REQUEST_ENTITY_TOO_LARGE = 141;
	public static final int RESP_UNSUPPORTED_MEDIA_TYPE = 143;

	// class 5.xx
	public static final int RESP_INTERNAL_SERVER_ERROR = 160;
	public static final int RESP_NOT_IMPLEMENTED = 161;
	public static final int RESP_BAD_GATEWAY = 162;
	public static final int RESP_SERVICE_UNAVAILABLE = 163;
	public static final int RESP_GATEWAY_TIMEOUT = 164;
	public static final int RESP_PROXYING_NOT_SUPPORTED = 165;

	// from draft-ietf-core-block
	public static final int RESP_REQUEST_ENTITY_INCOMPLETE = 136;

// Static methods //////////////////////////////////////////////////////////////

	/**
	 * Checks whether a code indicates a request.
	 * 
	 * @param code the code to check
	 * @return True iff the code indicates a request
	 */
	public static boolean isRequest(int code) {
		return (code >= 1) && (code <= 31);
	}

	/**
	 * Checks whether a code indicates a response
	 * 
	 * @param code the code to check
	 * @return True iff the code indicates a response
	 */
	public static boolean isResponse(int code) {

		return (code >= 64) && (code <= 191);
	}

	/**
	 * Checks whether a code is valid
	 * 
	 * @param code the code to check
	 * @return True iff the code is valid
	 */
	public static boolean isValid(int code) {
		//return ((code >= 0) && (code <= 31)) || ((code >= 64) && (code <= 191));
		return (code >= 0) && (code <= 255); // allow unknown custom codes
	}

	/**
	 * Returns the response class of a code
	 * 
	 * @param code the code to check
	 * @return The response class of the code
	 */
	public static int responseClass(int code) {
		return (code >> 5) & 0x7;
	}

	public static Message getMessageSubClass(int code) {
		if (isRequest(code)) {
			switch (code) {
			case METHOD_GET:
				return new GETRequest();
			case METHOD_POST:
				return new POSTRequest();
			case METHOD_PUT:
				return new PUTRequest();
			case METHOD_DELETE:
				return new DELETERequest();
			default:
				return new UnsupportedRequest(code);
			}
		} else if (isResponse(code) || code == EMPTY_MESSAGE) {
			return new Response(code);
		} else {
			return new Message(null, code);
		}
	}

	/**
	 * Returns a string representation of the code
	 * 
	 * @param code the code to describe
	 * @return A string describing the code
	 */
	public static String toString(int code) {

		switch (code) {
		case EMPTY_MESSAGE:
			return "Empty Message";

		case METHOD_GET:
			return "GET";
		case METHOD_POST:
			return "POST";
		case METHOD_PUT:
			return "PUT";
		case METHOD_DELETE:
			return "DELETE";

		case RESP_CREATED:
			return "2.01 Created";
		case RESP_DELETED:
			return "2.02 Deleted";
		case RESP_VALID:
			return "2.03 Valid";
		case RESP_CHANGED:
			return "2.04 Changed";
		case RESP_CONTENT:
			return "2.05 Content";
		case RESP_BAD_REQUEST:
			return "4.00 Bad Request";
		case RESP_UNAUTHORIZED:
			return "4.01 Unauthorized";
		case RESP_BAD_OPTION:
			return "4.02 Bad Option";
		case RESP_FORBIDDEN:
			return "4.03 Forbidden";
		case RESP_NOT_FOUND:
			return "4.04 Not Found";
		case RESP_METHOD_NOT_ALLOWED:
			return "4.05 Method Not Allowed";
		case RESP_NOT_ACCEPTABLE:
			return "4.06 Not Acceptable";
		case RESP_REQUEST_ENTITY_INCOMPLETE:
			return "4.08 Request Entity Incomplete";
		case RESP_PRECONDITION_FAILED:
			return "4.12 Precondition Failed";
		case RESP_REQUEST_ENTITY_TOO_LARGE:
			return "4.13 Request Entity Too Large";
		case RESP_UNSUPPORTED_MEDIA_TYPE:
			return "4.15 Unsupported Media Type";
		case RESP_INTERNAL_SERVER_ERROR:
			return "5.00 Internal Server Error";
		case RESP_NOT_IMPLEMENTED:
			return "5.01 Not Implemented";
		case RESP_BAD_GATEWAY:
			return "5.02 Bad Gateway";
		case RESP_SERVICE_UNAVAILABLE:
			return "5.03 Service Unavailable";
		case RESP_GATEWAY_TIMEOUT:
			return "5.04 Gateway Timeout";
		case RESP_PROXYING_NOT_SUPPORTED:
			return "5.05 Proxying Not Supported";
		}

		if (isValid(code)) {

			if (isRequest(code)) {
				return String.format("Unknown Request [code %d]", code);
			} else if (isResponse(code)) {
				return String.format("Unknown Response [code %d]", code);
			} else {
				return String.format("Reserved [code %d]", code);
			}

		} else {
			return String.format("Invalid Message [code %d]", code);
		}
	}
}

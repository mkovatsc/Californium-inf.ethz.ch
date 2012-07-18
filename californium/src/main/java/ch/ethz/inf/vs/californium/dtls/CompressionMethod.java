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
package ch.ethz.inf.vs.californium.dtls;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * The algorithm used to compress data prior to encryption.
 * 
 * @author Stefan Jucker
 * 
 */
public enum CompressionMethod {
	NULL(0x00);
	
	// DTLS-specific constants ////////////////////////////////////////

	private static final int COMPRESSION_METHOD_BITS = 8;
	
	// Members ////////////////////////////////////////////////////////

	private int code;
	
	// Constructor ////////////////////////////////////////////////////

	private CompressionMethod(int code) {
		this.code = code;
	}
	
	// Methods ////////////////////////////////////////////////////////

	public int getCode() {
		return code;
	}

	public static CompressionMethod getMethodByCode(int code) {
		switch (code) {
		case 0x00:
			return CompressionMethod.NULL;

		default:
			return null;
		}
	}
	
	// Serialization //////////////////////////////////////////////////

	/**
	 * Takes a list of compression methods and creates the representing byte
	 * stream.
	 * 
	 * @param compressionMethods
	 *            the list of the compression methods
	 * @return the corresponding byte array
	 */
	public static byte[] listToByteArray(List<CompressionMethod> compressionMethods) {

		DatagramWriter writer = new DatagramWriter();
		for (CompressionMethod compressionMethod : compressionMethods) {
			writer.write(compressionMethod.getCode(), COMPRESSION_METHOD_BITS);
		}

		return writer.toByteArray();
	}

	/**
	 * Takes a byte array and creates the representing list of compression
	 * methods.
	 * 
	 * @param byteArray
	 *            the encoded compression methods as byte array
	 * @param numElements
	 *            the number of compression methods represented in the byte
	 *            array
	 * @return corresponding list of compression methods
	 */
	public static List<CompressionMethod> listFromByteArray(byte[] byteArray, int numElements) {
		List<CompressionMethod> compressionMethods = new ArrayList<CompressionMethod>();
		DatagramReader reader = new DatagramReader(byteArray);

		for (int i = 0; i < numElements; i++) {
			int code = reader.read(COMPRESSION_METHOD_BITS);
			compressionMethods.add(CompressionMethod.getMethodByCode(code));
		}
		return compressionMethods;
	}

}

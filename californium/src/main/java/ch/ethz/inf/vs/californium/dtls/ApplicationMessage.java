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

import ch.ethz.inf.vs.californium.util.ByteArrayUtils;

/**
 * Application data messages are carried by the record layer and are fragmented,
 * compressed, and encrypted based on the current connection state. The messages
 * are treated as transparent data to the record layer.
 * 
 * @author Stefan Jucker
 * 
 */
public class ApplicationMessage implements DTLSMessage {

	// Members ////////////////////////////////////////////////////////

	/** The (to the record layer) transparent data. */
	private byte[] data;

	// Constructor ////////////////////////////////////////////////////

	/**
	 * 
	 * @param data
	 *            the application data.
	 */
	public ApplicationMessage(byte[] data) {
		this.data = data;
	}
	
	// Methods ////////////////////////////////////////////////////////

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("\tApplication Data: " + ByteArrayUtils.toHexString(data) + "\n");

		return sb.toString();
	}
	
	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		return data;
	}

	public static DTLSMessage fromByteArray(byte[] byteArray) {
		return new ApplicationMessage(byteArray);
	}

	// Getters and Setters ////////////////////////////////////////////

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}

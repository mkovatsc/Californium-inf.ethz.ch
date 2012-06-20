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

import java.util.Arrays;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;

/**
 * A Finished message is always sent immediately after a
 * {@link ChangeCipherSpecMessage} to verify that the key exchange and
 * authentication processes were successful. It is essential that a
 * {@link ChangeCipherSpecMessage} be received between the other handshake
 * messages and the Finished message. The Finished message is the first one
 * protected with the just negotiated algorithms, keys, and secrets. The value
 * handshake_messages includes all handshake messages starting at
 * {@link ClientHello} up to, but not including, this {@link Finished} message.
 * See <a href="http://tools.ietf.org/html/rfc5246#section-7.4.9">RFC 5246</a>.
 * 
 * @author Stefan Jucker
 * 
 */
public class Finished extends HandshakeMessage {

	// Members ////////////////////////////////////////////////////////

	private byte[] verifyData;

	// Constructors ///////////////////////////////////////////////////

	/**
	 * Generates the verify data according to <a
	 * href="http://tools.ietf.org/html/rfc5246#section-7.4.9">RFC 5246</a>:<br />
	 * <code>PRF(master_secret,
	 * finished_label, Hash(handshake_messages))</code>.
	 * 
	 * @param masterSecret
	 *            the master_secret
	 * @param isClient
	 *            to determine the finished_label
	 * @param handshakeHash
	 *            the hash
	 */
	public Finished(byte[] masterSecret, boolean isClient, byte[] handshakeHash) {
		verifyData = getVerifyData(masterSecret, isClient, handshakeHash);
	}

	/**
	 * Called when reconstructing byteArray.
	 * 
	 * @param verifyData
	 */
	public Finished(byte[] verifyData) {
		this.verifyData = verifyData;
	}

	// Methods ////////////////////////////////////////////////////////

	public boolean verifyData(byte[] masterSecret, boolean isClient, byte[] handshakeHash) {
		byte[] myVerifyData = getVerifyData(masterSecret, isClient, handshakeHash);

		return Arrays.equals(myVerifyData, verifyData);
	}

	private byte[] getVerifyData(byte[] masterSecret, boolean isClient, byte[] handshakeHash) {
		byte[] data = null;

		String label = (isClient) ? Handshaker.CLIENT_FINISHED_LABEL : Handshaker.SERVER_FINISHED_LABEL;

		// TODO check this
		data = Handshaker.doPRF(masterSecret, label, handshakeHash);

		return data;
	}

	@Override
	public HandshakeType getMessageType() {
		return HandshakeType.FINISHED;
	}

	@Override
	public int getMessageLength() {
		return verifyData.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append("\t\t" + Arrays.toString(verifyData) + "\n");

		return sb.toString();
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] toByteArray() {
		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(super.toByteArray());
		
		// TODO is no length field needed for this verify data?
		writer.writeBytes(verifyData);

		return writer.toByteArray();
	}

	public static HandshakeMessage fromByteArray(byte[] byteArray) {
		DatagramReader reader = new DatagramReader(byteArray);

		byte[] verifyData = reader.readBytesLeft();

		return new Finished(verifyData);
	}

}

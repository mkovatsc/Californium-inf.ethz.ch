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
 * This class represents a fragmented handshake message. It treats the
 * underlying handshake body as transparent data and just helps keeping track of
 * the fragment_offset and fragment_length.
 * 
 * @author Stefan Jucker
 * 
 */
public class FragmentedHandshakeMessage extends HandshakeMessage {

	// Members ////////////////////////////////////////////////////////

	/** The fragmented handshake body. */
	private byte[] fragmentedBytes;

	/** The handshake message's type. */
	private HandshakeType type;

	/** The handshake message's unfragmented length. */
	private int messageLength;

	// Constructors ///////////////////////////////////////////////////

	/**
	 * Called when reassembling a handshake message or received a fragment
	 * during the handshake.
	 * 
	 * @param type
	 *            the message's type.
	 * @param messageLength
	 *            the message's total length.
	 * @param messageSeq
	 *            the message's message_seq.
	 * @param fragmentOffset
	 *            the message's fragment_offset.
	 * @param fragmentedBytes
	 *            the fragment's byte representation.
	 */
	public FragmentedHandshakeMessage(HandshakeType type, int messageLength, int messageSeq, int fragmentOffset, byte[] fragmentedBytes) {
		this.type = type;
		this.messageLength = messageLength;
		this.fragmentedBytes = fragmentedBytes;
		setMessageSeq(messageSeq);
		setFragmentOffset(fragmentOffset);
		setFragmentLength(fragmentedBytes.length);
	}

	/**
	 * Called when fragmenting a handshake message.
	 * 
	 * @param fragmentedBytes
	 *            the fragment's byte representation.
	 * @param type
	 *            the message's type.
	 * @param fragmentOffset
	 *            the fragment's fragment_offset.
	 * @param messageLength
	 *            the message's total (unfragmented) length.
	 */
	public FragmentedHandshakeMessage(byte[] fragmentedBytes, HandshakeType type, int fragmentOffset, int messageLength) {
		this.fragmentedBytes = fragmentedBytes;
		this.type = type;
		setFragmentOffset(fragmentOffset);
		setFragmentLength(fragmentedBytes.length);
		this.messageLength = messageLength;
	}

	// Methods ////////////////////////////////////////////////////////

	@Override
	public HandshakeType getMessageType() {
		return type;
	}

	@Override
	public int getMessageLength() {
		return messageLength;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\t\t\tFragmented Handshake Message: " + fragmentedBytes.length + " bytes\n");
		sb.append("\t\t\t\t" + ByteArrayUtils.toHexString(fragmentedBytes) + "\n");

		return sb.toString();
	}

	// Serialization //////////////////////////////////////////////////

	@Override
	public byte[] fragmentToByteArray() {
		return fragmentedBytes;
	}

}

package ch.ethz.inf.vs.californium.network.serializer;

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

import java.io.ByteArrayInputStream;

/**
 * This class describes the functionality to read raw network-ordered datagrams
 * on bit-level.
 * 
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class DatagramReader {

	// Attributes //////////////////////////////////////////////////////////////

	private ByteArrayInputStream byteStream;

	private byte currentByte;
	private int currentBitIndex;
	
	// Constructors ////////////////////////////////////////////////////////////

	/**
	 * Initializes a new BitReader object
	 * 
	 * @param byteArray
	 *            The byte array to read from
	 */
	public DatagramReader(byte[] byteArray) {

		// initialize underlying byte stream
		byteStream = new ByteArrayInputStream(byteArray);

		// initialize bit buffer
		currentByte = 0;
		currentBitIndex = -1; // indicates that no byte read yet
	}

	// Methods /////////////////////////////////////////////////////////////////

	/**
	 * 
	 * Reads a sequence of bits from the stream
	 * 
	 * @param numBits
	 *            The number of bits to read
	 * 
	 * @return A Long containing the bits read
	 */
	public long readLong(int numBits) {

		long bits = 0; // initialize all bits to zero

		for (int i = numBits - 1; i >= 0; i--) {

			// check whether new byte needs to be read
			if (currentBitIndex < 0) {
				readCurrentByte();
			}

			// test current bit
			boolean bit = (currentByte >> currentBitIndex & 1) != 0;
			if (bit) {
				// set bit at i-th position
				bits |= (1L << i);
			}

			// decrease current bit index
			--currentBitIndex;

		}

		return bits;
	}

	/**
	 * Reads a sequence of bits from the stream
	 * 
	 * @param numBits
	 *            The number of bits to read
	 * 
	 * @return An integer containing the bits read
	 */
	public int read(int numBits) {

		int bits = 0; // initialize all bits to zero

		for (int i = numBits - 1; i >= 0; i--) {

			// check whether new byte needs to be read
			if (currentBitIndex < 0) {
				readCurrentByte();
			}

			// test current bit
			boolean bit = (currentByte >> currentBitIndex & 1) != 0;
			if (bit) {
				// set bit at i-th position
				bits |= (1 << i);
			}

			// decrease current bit index
			--currentBitIndex;

		}

		return bits;
	}

	/**
	 * Reads a sequence of bytes from the stream
	 * 
	 * @param count
	 *            The number of bytes to read
	 * 
	 * @return The sequence of bytes read from the stream
	 */
	public byte[] readBytes(int count) {

		// for negative count values, read all bytes left
		if (count < 0)
			count = byteStream.available();

		// allocate byte array
		byte[] bytes = new byte[count];

		// are there bits left to read in buffer?
		if (currentBitIndex >= 0) {

			for (int i = 0; i < count; i++) {
				bytes[i] = (byte) read(Byte.SIZE);
			}

		} else {

			// if bit buffer is empty, call can be delegated
			// to byte stream to increase performance
			byteStream.read(bytes, 0, bytes.length);
		}

		return bytes;
	}

	/**
	 * Reads the next byte from the stream.
	 * 
	 * @return The next byte.
	 */
	public byte readNextByte() {
		byte[] bytes = readBytes(1);

		return bytes[0];
	}

	/**
	 * Reads the complete sequence of bytes left in the stream
	 * 
	 * @return The sequence of bytes left in the stream
	 */
	public byte[] readBytesLeft() {
		return readBytes(-1);
	}

	/**
	 * 
	 * @return <code>true</code> if there are bytes left to read,
	 *         <code>false</code> otherwise.
	 */
	public boolean bytesAvailable() {
		return byteStream.available() > 0;
	}

	// Utilities ///////////////////////////////////////////////////////////////

	/**
	 * Reads new bits from the stream
	 */
	private void readCurrentByte() {

		// try to read from byte stream
		int val = byteStream.read();

		if (val >= 0) {
			// byte successfully read
			currentByte = (byte) val;
		} else {
			// end of stream reached;
			// return implicit zero bytes
			currentByte = 0;
		}

		// reset current bit index
		currentBitIndex = Byte.SIZE - 1;
	}
}

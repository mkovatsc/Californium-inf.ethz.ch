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

public class BlockOption extends Option {

	private static int encode(int num, int szx, boolean m) {
		int value = 0;
		
		value |= (szx & 0x7)     ;
		value |= (m ? 1 : 0) << 3;
		value |= num         << 4;
		
		return value;
	}	

	public BlockOption(int nr) {
		super(0, nr);
	}
	
	public BlockOption(int nr, int num, int szx, boolean m) {
		super(encode(num, szx, m), nr);
	}

	public void setValue(int num, int szx, boolean m) {
		setIntValue(encode(num, szx, m));
	}
	
	public int getNUM() {
		return getIntValue() >> 4;
	}
	public void setNUM(int num) {
		setValue(num, getSZX(), getM());
	}

	public int getSZX() {
		return getIntValue() & 0x7;
	}
	public void setSZX(int szx) {
		setValue(getNUM(), szx, getM());
	}
	public int getSize() {
		return decodeSZX(getIntValue() & 0x7);
	}
	public void setSize(int size) {
		setValue(getNUM(), encodeSZX(size), getM());
	}
	
	public boolean getM() {
		return (getIntValue() >> 3 & 0x1) != 0;
	}
	public void setM(boolean m) {
		setValue(getNUM(), getSZX(), m);
	}

	/*
	 * Decodes a 3-bit SZX value into a block size as specified by
	 * draft-ietf-core-block-03, section-2.1:
	 * 
	 * 0 --> 2^4 = 16 bytes
	 * ... 
	 * 6 --> 2^10 = 1024 bytes
	 * 
	 */
	public static int decodeSZX(int szx) {
		return 1 << (szx + 4);
	}

	/*
	 * Encodes a block size into a 3-bit SZX value as specified by
	 * draft-ietf-core-block-03, section-2.1:
	 * 
	 * 16 bytes = 2^4 --> 0
	 * ... 
	 * 1024 bytes = 2^10 -> 6
	 * 
	 */
	public static int encodeSZX(int blockSize) {
		return (int)(Math.log(blockSize)/Math.log(2)) - 4;
	}
	
	public static boolean validSZX(int szx) {
		return (szx >= 0 && szx <= 6);
	}
	
	@Override
	public String toString() {
		return String.format("NUM: %d, SZX: %d (%d bytes), M: %b", 
			getNUM(), getSZX(), getSize(), getM());		
	}
	
}

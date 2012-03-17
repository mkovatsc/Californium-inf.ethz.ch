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
package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.*;
import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.Option;

public class OptionTest {

	@Test
	public void testRawOption() {
		byte[] dataRef = "test".getBytes();
		int nrRef = 1;

		Option opt = new Option(dataRef, nrRef);

		assertArrayEquals(dataRef, opt.getRawValue());
		assertEquals(dataRef.length, opt.getLength());
	}

	@Test
	public void testIntOption() {

		int oneByteValue = 255; // fits in 1 Byte
		int twoBytesValue = 256; // needs 2 Bytes

		int nrRef = 1;

		Option optOneByte = new Option(oneByteValue, nrRef);
		Option optTwoBytes = new Option(twoBytesValue, nrRef);

		assertEquals(1, optOneByte.getLength());
		assertEquals(2, optTwoBytes.getLength());
		assertEquals(255, optOneByte.getIntValue());
		assertEquals(256, optTwoBytes.getIntValue());
	}

	@Test
	public void testStringOption() {
		String strRef = "test";
		int nrRef = 1;

		Option opt = new Option(strRef, nrRef);

		assertEquals(strRef, opt.getStringValue());
		assertEquals(strRef.getBytes().length, opt.getLength());
	}

	@Test
	public void testOptionNr() {
		byte[] dataRef = "test".getBytes();
		int nrRef = 1;

		Option opt = new Option(dataRef, nrRef);

		assertEquals(nrRef, opt.getOptionNumber());
	}

	@Test
	public void equalityTest() {
		int oneByteValue = 255; // fits in 1 Byte
		int twoBytesValue = 256; // needs 2 Bytes

		int nrRef = 1;

		Option optOneByte = new Option(oneByteValue, nrRef);
		Option optTwoBytes = new Option(twoBytesValue, nrRef);
		Option optTwoBytesRef = new Option(twoBytesValue, nrRef);

		assertTrue(optTwoBytes.equals(optTwoBytesRef));
		assertFalse(optTwoBytes.equals(optOneByte));
	}

	public static String getHexString(byte[] b) throws Exception {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
}

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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;

import ch.ethz.inf.vs.californium.util.DatagramReader;
import ch.ethz.inf.vs.californium.util.DatagramWriter;



/*
 * This unit test examines the DatagramReader and DatagramWriter 
 * classes for consistency and correct data format.
 *  
 * @author Dominique Im Obersteg & Daniel Pauli
 * @version 0.1
 * 
 */
public class DatagramReadWriteTest {

	@Test
	public void test32BitInt() {

		final int intIn = 0x87654321;

		DatagramWriter writer = new DatagramWriter();
		writer.write(intIn, 32);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int intOut = reader.read(32);

		assertEquals(intIn, intOut);
	}

	@Test
	public void test32BitIntZero() {

		final int intIn = 0x00000000;

		DatagramWriter writer = new DatagramWriter();
		writer.write(intIn, 32);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int intOut = reader.read(32);

		assertEquals(intIn, intOut);
	}

	@Test
	public void test32BitIntOne() {

		final int intIn = 0xFFFFFFFF;

		DatagramWriter writer = new DatagramWriter();
		writer.write(intIn, 32);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int intOut = reader.read(32);

		assertEquals(intIn, intOut);
	}

	@Test
	public void test16BitInt() {

		final int intIn = 0x00004321;

		DatagramWriter writer = new DatagramWriter();
		writer.write(intIn, 16);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int intOut = reader.read(16);

		assertEquals(intIn, intOut);
	}

	@Test
	public void test8BitInt() {

		final int intIn = 0x00000021;

		DatagramWriter writer = new DatagramWriter();
		writer.write(intIn, 8);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int intOut = reader.read(8);

		assertEquals(intIn, intOut);
	}

	@Test
	public void test4BitInt() {

		final int intIn = 0x0000005;

		DatagramWriter writer = new DatagramWriter();
		writer.write(intIn, 4);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int intOut = reader.read(4);

		assertEquals(intIn, intOut);
	}

	@Test
	public void test2BitInt() {

		final int intIn = 0x00000002;

		DatagramWriter writer = new DatagramWriter();
		writer.write(intIn, 2);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int intOut = reader.read(2);

		assertEquals(intIn, intOut);
	}

	@Test
	public void test1BitInt() {

		final int intIn = 0x00000001;

		DatagramWriter writer = new DatagramWriter();
		writer.write(intIn, 1);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int intOut = reader.read(1);

		assertEquals(intIn, intOut);
	}

	@Test
	public void testByteOrder() {

		final int intIn = 1234567890;

		DatagramWriter writer = new DatagramWriter();
		writer.write(intIn, 32);

		final byte[] data = writer.toByteArray();
		ByteBuffer buf = ByteBuffer.wrap(data);
		buf.order(ByteOrder.BIG_ENDIAN);
		int intTrans = buf.getInt();

		assertEquals(intIn, intTrans);

		DatagramReader reader = new DatagramReader(data);
		int intOut = reader.read(32);

		assertEquals(intIn, intOut);
	}

	@Test
	public void testAlignedBytes() {

		final byte[] bytesIn = "Some aligned Bytes".getBytes();

		DatagramWriter writer = new DatagramWriter();
		writer.writeBytes(bytesIn);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		byte[] bytesOut = reader.readBytes(bytesIn.length);

		assertArrayEquals(bytesIn, bytesOut);

	}

	@Test
	public void testUnalignedBytes1() {

		final int bitCount = 1;
		final int bitsIn = 0x1;
		final byte[] bytesIn = "Some unaligned Bytes".getBytes();

		DatagramWriter writer = new DatagramWriter();
		writer.write(bitsIn, bitCount);
		writer.writeBytes(bytesIn);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int bitsOut = reader.read(bitCount);
		byte[] bytesOut = reader.readBytes(bytesIn.length);

		assertEquals(bitsIn, bitsOut);
		assertArrayEquals(bytesIn, bytesOut);

	}

	@Test
	public void testUnalignedBytes3() {

		final int bitCount = 3;
		final int bitsIn = 0x5;
		final byte[] bytesIn = "Some unaligned Bytes".getBytes();

		DatagramWriter writer = new DatagramWriter();
		writer.write(bitsIn, bitCount);
		writer.writeBytes(bytesIn);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int bitsOut = reader.read(bitCount);
		byte[] bytesOut = reader.readBytes(bytesIn.length);

		assertEquals(bitsIn, bitsOut);
		assertArrayEquals(bytesIn, bytesOut);

	}

	@Test
	public void testUnalignedBytes7() {

		final int bitCount = 7;
		final int bitsIn = 0x69;
		final byte[] bytesIn = "Some unaligned Bytes".getBytes();

		DatagramWriter writer = new DatagramWriter();
		writer.write(bitsIn, bitCount);
		writer.writeBytes(bytesIn);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int bitsOut = reader.read(bitCount);
		byte[] bytesOut = reader.readBytes(bytesIn.length);

		assertEquals(bitsIn, bitsOut);
		assertArrayEquals(bytesIn, bytesOut);

	}

	@Test
	public void testBytesLeft() {

		final int bitCount = 8;
		final int bitsIn = 0xAA;
		final byte[] bytesIn = "Some payload".getBytes();

		DatagramWriter writer = new DatagramWriter();
		writer.write(bitsIn, bitCount);
		writer.writeBytes(bytesIn);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int bitsOut = reader.read(bitCount);
		byte[] bytesOut = reader.readBytesLeft();

		assertEquals(bitsIn, bitsOut);
		assertArrayEquals(bytesIn, bytesOut);

	}

	@Test
	public void testBytesLeftUnaligned() {

		final int bitCount = 7;
		final int bitsIn = 0x55;
		final byte[] bytesIn = "Some payload".getBytes();

		DatagramWriter writer = new DatagramWriter();
		writer.write(bitsIn, bitCount);
		writer.writeBytes(bytesIn);

		DatagramReader reader = new DatagramReader(writer.toByteArray());
		int bitsOut = reader.read(bitCount);
		byte[] bytesOut = reader.readBytesLeft();

		assertEquals(bitsIn, bitsOut);
		assertArrayEquals(bytesIn, bytesOut);

	}

	@Test
	public void testGETRequestHeader() {

		final int versionIn = 1;
		final int versionSz = 2;
		final int typeIn = 0; // Confirmable
		final int typeSz = 2;
		final int optionCntIn = 1;
		final int optionCntSz = 4;
		final int codeIn = 1; // GET Request
		final int codeSz = 8;
		final int msgIdIn = 0x1234;
		final int msgIdSz = 16;

		DatagramWriter writer = new DatagramWriter();
		writer.write(versionIn, versionSz);
		writer.write(typeIn, typeSz);
		writer.write(optionCntIn, optionCntSz);
		writer.write(codeIn, codeSz);
		writer.write(msgIdIn, msgIdSz);

		final byte[] data = writer.toByteArray();
		final byte[] dataRef = { 0x41, 0x01, 0x12, 0x34 };

		assertArrayEquals(dataRef, data);

		DatagramReader reader = new DatagramReader(data);
		int versionOut = reader.read(versionSz);
		int typeOut = reader.read(typeSz);
		int optionCntOut = reader.read(optionCntSz);
		int codeOut = reader.read(codeSz);
		int msgIdOut = reader.read(msgIdSz);

		assertEquals(versionIn, versionOut);
		assertEquals(typeIn, typeOut);
		assertEquals(optionCntIn, optionCntOut);
		assertEquals(codeIn, codeOut);
		assertEquals(msgIdIn, msgIdOut);

	}

}

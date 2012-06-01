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

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Message.messageType;

public class MessageTest {

	@Test
	public void testMessage() {

		Message msg = new Message();

		msg.setCode(CodeRegistry.METHOD_GET);
		msg.setType(messageType.CON);
		msg.setMID(12345);
		msg.setPayload("some payload".getBytes());

		System.out.println(msg.toString());

		byte[] data = msg.toByteArray();
		Message convMsg = Message.fromByteArray(data);

		assertEquals(msg.getCode(), convMsg.getCode());
		assertEquals(msg.getType(), convMsg.getType());
		assertEquals(msg.getMID(), convMsg.getMID());
		assertEquals(msg.getOptionCount(), convMsg.getOptionCount());
		assertArrayEquals(msg.getPayload(), convMsg.getPayload());
	}

	@Test
	public void testOptionMessage() {
		Message msg = new Message();

		msg.setCode(CodeRegistry.METHOD_GET);
		msg.setType(messageType.CON);
		msg.setMID(12345);
		msg.setPayload("hallo".getBytes());
		msg.addOption(new Option("a".getBytes(), 1));
		msg.addOption(new Option("b".getBytes(), 2));

		byte[] data = msg.toByteArray();
		Message convMsg = Message.fromByteArray(data);

		assertEquals(msg.getCode(), convMsg.getCode());
		assertEquals(msg.getType(), convMsg.getType());
		assertEquals(msg.getMID(), convMsg.getMID());
		assertEquals(msg.getOptionCount(), convMsg.getOptionCount());
		assertArrayEquals(msg.getPayload(), convMsg.getPayload());
	}

	@Test
	public void testExtendedOptionMessage() {
		Message msg = new Message();

		msg.setCode(CodeRegistry.METHOD_GET);
		msg.setType(messageType.CON);
		msg.setMID(12345);

		msg.addOption(new Option("a".getBytes(), 1));
		msg.addOption(new Option("ab".getBytes(), 197));

		// will fail as limit of max 15 options would be exceeded
		// msg.addOption(new Option ("c".getBytes(), 212));

		byte[] data = msg.toByteArray();
		try {
			System.out.printf("Testing getHexString(): 0x%s (%d)\n", getHexString(data), data.length);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Message convMsg = Message.fromByteArray(data);

		assertEquals(msg.getCode(), convMsg.getCode());
		assertEquals(msg.getType(), convMsg.getType());
		assertEquals(msg.getMID(), convMsg.getMID());

		assertEquals(msg.getOptionCount(), convMsg.getOptionCount());
	}

	public static String getHexString(byte[] b) throws Exception {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

}

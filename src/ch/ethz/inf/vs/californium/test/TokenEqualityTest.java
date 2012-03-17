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
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;


public class TokenEqualityTest {

	@Test
	public void testEmptyToken() {
		Option t1 = new Option(new byte[0], OptionNumberRegistry.TOKEN);
		Option t2 = new Option(new byte[0], OptionNumberRegistry.TOKEN);
		assertEquals(t1, t2);
		
		assertEquals(0, t1.getLength());
		
		Option t3 = new Option("Not empty", OptionNumberRegistry.TOKEN);
		assertFalse(t1.equals(t3)); // Why no assertNotEquals in JUnit?!
	}
	
	@Test
	public void testOneByteToken() {
		Option t1 = new Option(0xAB, OptionNumberRegistry.TOKEN);
		Option t2 = new Option(0xAB, OptionNumberRegistry.TOKEN);
		assertEquals(t1, t2);
		
		assertEquals(1, t1.getLength());
		
		Option t3 = new Option(0xAC, OptionNumberRegistry.TOKEN);
		assertFalse(t1.equals(t3)); // Why no assertNotEquals in JUnit?!
	}
	
	@Test
	public void testTwoByteToken() {
		Option t1 = new Option(0xABCD, OptionNumberRegistry.TOKEN);
		Option t2 = new Option(0xABCD, OptionNumberRegistry.TOKEN);
		assertEquals(t1, t2);
		
		assertEquals(2, t1.getLength());
		
		Option t3 = new Option(0xABCE, OptionNumberRegistry.TOKEN);
		assertFalse(t1.equals(t3)); // Why no assertNotEquals in JUnit?!
	}

	@Test
	public void testFourByteToken() {
		Option t1 = new Option(0xABCDEF01, OptionNumberRegistry.TOKEN);
		Option t2 = new Option(0xABCDEF01, OptionNumberRegistry.TOKEN);
		assertEquals(t1, t2);
		
		assertEquals(4, t1.getLength());
		
		Option t3 = new Option(0xABCDEF02, OptionNumberRegistry.TOKEN);
		assertFalse(t1.equals(t3)); // Why no assertNotEquals in JUnit?!
	}

	
}

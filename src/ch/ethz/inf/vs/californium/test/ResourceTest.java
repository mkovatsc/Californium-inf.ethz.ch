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
 * This file is part of the Californium CoAP framework.
 ******************************************************************************/
package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.*;

import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.endpoint.RemoteResource;
import ch.ethz.inf.vs.californium.endpoint.Resource;


public class ResourceTest {

	@Test
	public void simpleTest() {
		String input = "</sensors/temp>;ct=41;rt=\"TemperatureC\"";
		Resource root = RemoteResource.newRoot(input);

		Resource res = root.getResource("/sensors/temp");
		assertNotNull(res);

		assertEquals("temp", res.getName());
		assertEquals(41, res.getContentTypeCode());
		assertEquals("TemperatureC", res.getResourceType());
	}

	@Test
	public void extendedTest() {
		String input = "</myPäth>;rt=\"MyName\";if=\"/someRef/path\";ct=42;obs;sz=10";
		Resource root = RemoteResource.newRoot(input);
		root.prettyPrint();

		Resource res = root.getResource("/myPäth");
		
		res.prettyPrint();
		
		assertNotNull(res);


		assertEquals("myPäth", res.getName());
		assertEquals("/myPäth", res.getPath());
		assertEquals("MyName", res.getResourceType().get(0));
		assertEquals("/someRef/path", res.getInterfaceDescription().get(0));
		assertEquals(42, res.getContentTypeCode().get(0).intValue());
		assertEquals(10, res.getMaximumSizeEstimate());
		assertTrue(res.isObservable());
	
	}

	@Test
	public void conversionTest() {
		String link1 = "</myUri/something>;ct=42;if=\"/someRef/path\";obs;rt=\"MyName\";sz=10";
		String link2 = "</myUri>";
		String link3 = "</a>";
		String format = link1 + "," + link2 + "," + link3;
		Resource res = RemoteResource.newRoot(format);
		res.prettyPrint();
		String result = LinkFormat.serialize(res, null, true);
		assertEquals(link3 + "," + link2 + "," + link1, result);
	}
}

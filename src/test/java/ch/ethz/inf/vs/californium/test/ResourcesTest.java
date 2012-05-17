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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.endpoint.RemoteResource;
import ch.ethz.inf.vs.californium.endpoint.Resource;


//import ch.ethz.inf.vs.californium.coap.Resources;

public class ResourcesTest {
	@Test
	public void TwoResourceTest() {

		// note: order of attributes may change

		String resourceInput1 = "</myUri>,</myUri/something>;ct=42;if=\"/someRef/path\";obs;rt=\"MyName\";sz=10";
		String resourceInput2 = "</sensors>,</sensors/temp>;ct=41;obs;rt=\"TemperatureC\"";

		// Build link format string
		String resourceInput = resourceInput1 + "," + resourceInput2;

		// Construct two resources from link format substrings
		// Resource res1 = Resource.fromLinkFormat(resourceInput1);
		// Resource res2 = Resource.fromLinkFormat(resourceInput2);

		// Build resources from assembled link format string
		Resource resource = RemoteResource.newRoot(resourceInput);

		// Check if resources are in hash map
		// assertTrue(resources.hasResource(res1.getResourceName()));
		// assertTrue(resources.hasResource(res2.getResourceName()));

		// Check if link format string equals input
		// String expectedLinkFormat = res1.toLinkFormat() + "," +
		// res2.toLinkFormat();
		// assertEquals(expectedLinkFormat, resources.toLinkFormat());
		assertEquals(resourceInput, LinkFormat.serialize(resource, null, true));
	}

}

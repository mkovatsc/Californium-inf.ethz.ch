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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.LinkFormat;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.RemoteResource;
import ch.ethz.inf.vs.californium.endpoint.Resource;


public class ResourceTest {

	@Test
	public void simpleTest() {
		System.out.println("=[ simpleTest ]==============================");
		
		String input = "</sensors/temp>;ct=41;rt=\"TemperatureC\"";
		Resource root = RemoteResource.newRoot(input);
		root.prettyPrint();

		Resource res = root.getResource("/sensors/temp");
		assertNotNull(res);

		System.out.println(res.getName());

		assertEquals("temp", res.getName());
		assertEquals(Integer.valueOf(41), res.getContentTypeCode().get(0));
		assertEquals("TemperatureC", res.getResourceType().get(0));
	}

	@Test
	public void extendedTest() {
		System.out.println("=[ extendedTest ]==============================");
		
		String input = "</my/Päth>;rt=\"MyName\";if=\"/someRef/path\";ct=42;obs;sz=10";
		Resource root = RemoteResource.newRoot(input);
		
		RemoteResource my = new RemoteResource("my");
		my.setResourceType("replacement");
		root.add(my);

		root.prettyPrint();

		Resource res = root.getResource("/my/Päth");
		assertNotNull(res);
		res = root.getResource("my/Päth");
		assertNotNull(res);
		res = root.getResource("my");
		res = res.getResource("Päth");
		assertNotNull(res);
		res = res.getResource("/my/Päth");
		assertNotNull(res);

		assertEquals("Päth", res.getName());
		assertEquals("/my/Päth", res.getPath());
		assertEquals("MyName", res.getResourceType().get(0));
		assertEquals("/someRef/path", res.getInterfaceDescription().get(0));
		assertEquals(42, res.getContentTypeCode().get(0).intValue());
		assertEquals(10, res.getMaximumSizeEstimate());
		assertTrue(res.isObservable());
		
		res = root.getResource("my");
		assertNotNull(res);
		assertEquals("replacement", res.getResourceType().get(0));
	}

	@Test
	public void conversionTest() {
		System.out.println("=[ conversionTest ]==============================");
		
		String link1 = "</myUri/something>;ct=42;if=\"/someRef/path\";obs;rt=\"MyName\";sz=10";
		String link2 = "</myUri>;rt=\"NonDefault\"";
		String link3 = "</a>";
		String format = link1 + "," + link2 + "," + link3;
		Resource res = RemoteResource.newRoot(format);
		res.prettyPrint();
		String result = LinkFormat.serialize(res, null, true);
		System.out.println(link3 + "," + link2 + "," + link1);
		System.out.println(result);
		assertEquals(link3 + "," + link2 + "," + link1, result);
	}
	
	@Test
	public void concreteTest() {
		System.out.println("=[ concreteTest ]==============================");
		
		String link = "</careless>;rt=\"SepararateResponseTester\";title=\"This resource will ACK anything, but never send a separate response\",</feedback>;rt=\"FeedbackMailSender\";title=\"POST feedback using mail\",</helloWorld>;rt=\"HelloWorldDisplayer\";title=\"GET a friendly greeting!\",</image>;ct=21;ct=22;ct=23;ct=24;rt=\"Image\";sz=18029;title=\"GET an image with different content-types\",</large>;rt=\"block\";title=\"Large resource\",</large_update>;rt=\"block\";rt=\"observe\";title=\"Large resource that can be updated using PUT method\",</mirror>;rt=\"RequestMirroring\";title=\"POST request to receive it back as echo\",</obs>;obs;rt=\"observe\";title=\"Observable resource which changes every 5 seconds\",</query>;title=\"Resource accepting query parameters\",</seg1/seg2/seg3>;title=\"Long path resource\",</separate>;title=\"Resource which cannot be served immediately and which cannot be acknowledged in a piggy-backed way\",</storage>;obs;rt=\"Storage\";title=\"PUT your data here or POST new resources!\",</test>;title=\"Default test resource\",</timeResource>;rt=\"CurrentTime\";title=\"GET the current time\",</toUpper>;rt=\"UppercaseConverter\";title=\"POST text here to convert it to uppercase\",</weatherResource>;rt=\"ZurichWeather\";title=\"GET the current weather in zurich\"";
		Resource res = RemoteResource.newRoot(link);
		String result = LinkFormat.serialize(res, null, true);
		System.out.println(link);
		System.out.println(result);
		assertEquals(link, result);
	}

	@Test
	public void matchTest() {
		System.out.println("=[ matchTest ]==============================");
		
		String link1 = "</myUri/something>;ct=42;if=\"/someRef/path\";obs;rt=\"MyName\";sz=10";
		String link2 = "</myUri>;ct=50;rt=\"MyName\"";
		String link3 = "</a>;sz=10;rt=\"MyNope\"";
		String format = link1 + "," + link2 + "," + link3;
		Resource res = RemoteResource.newRoot(format);
		res.prettyPrint();
		
		List<Option> query = new ArrayList<Option>();
		query.add(new Option("rt=MyName", OptionNumberRegistry.URI_QUERY));
		
		System.out.println(LinkFormat.matches(res.getResource("/myUri/something"), query));
		
		String queried = LinkFormat.serialize(res, query, true);

		assertEquals(link2+","+link1, queried);
	}
}

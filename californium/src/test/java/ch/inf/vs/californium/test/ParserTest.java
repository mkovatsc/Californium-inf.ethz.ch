package ch.inf.vs.californium.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.inf.vs.californium.coap.CoAP.Code;
import ch.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.inf.vs.californium.coap.Option;
import ch.inf.vs.californium.coap.Request;
import ch.inf.vs.californium.coap.Response;
import ch.inf.vs.californium.network.parser.DataParser;
import ch.inf.vs.californium.network.parser.DataUnparser;

public class ParserTest {

	@Test
	public void testRequestParsing() {
		Request request = new Request(Code.POST);
		request.setMid(7);
		request.setToken(new byte[] {11, 82, -91, 77, 3});
		request.getOptions().addIfMatch(new byte[] {34, -17})
							.addIfMatch(new byte[] {88, 12, -2, -99, 5})
							.setContentFormat(40)
							.setAccept(40);
		
		DataParser parser = new DataParser();
		byte[] bytes = parser.parseRequest(request);
		
		DataUnparser unparser = new DataUnparser(bytes);
		assertTrue(unparser.isRequest());

		Request result = unparser.unparseRequest();
		assertEquals(request.getMid(), result.getMid());
		assertArrayEquals(request.getToken(), result.getToken());
		assertEquals(request.getOptions().asSortedList(), result.getOptions().asSortedList());
	}
	
	@Test
	public void testResponseParsing() {
		Response response = new Response(ResponseCode.CONTENT);
		response.setMid(9);
		response.setToken(new byte[] {22, -1, 0, 78, 100, 22});
		response.getOptions().addETag(new byte[] {1, 0, 0, 0, 0, 1})
							.addLocationPath("/one/two/three/four/five/six/seven/eight/nine/ten")
							.addOption(new Option(57453, "Arbitrary".hashCode()))
							.addOption(new Option(19205, "Arbitrary1"))
							.addOption(new Option(19205, "Arbitrary2"))
							.addOption(new Option(19205, "Arbitrary3"));
		
		DataParser parser = new DataParser();
		byte[] bytes = parser.parseResponse(response);
		
		DataUnparser unparser = new DataUnparser(bytes);
		assertTrue(unparser.isResponse());
		
		Response result = unparser.unparseResponse();
		assertEquals(response.getMid(), result.getMid());
		assertArrayEquals(response.getToken(), result.getToken());
		assertEquals(response.getOptions().asSortedList(), result.getOptions().asSortedList());
	}
}

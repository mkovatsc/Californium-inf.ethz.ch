package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.network.serialization.DataParser;
import ch.ethz.inf.vs.californium.network.serialization.DataSerializer;

/**
 * This test tests the serialization of messages to byte arrays and the parsing
 * back to messages.
 */
public class ParserTest {

	@Test
	public void testRequestParsing() {
		Request request = new Request(Code.POST);
		request.setType(Type.NON);
		request.setMID(7);
		request.setToken(new byte[] {11, 82, -91, 77, 3});
		request.getOptions().addIfMatch(new byte[] {34, -17})
							.addIfMatch(new byte[] {88, 12, -2, -99, 5})
							.setContentFormat(40)
							.setAccept(40);
		
		DataSerializer serializer = new DataSerializer();
		byte[] bytes = serializer.serializeRequest(request);
		
		DataParser parser = new DataParser(bytes);
		assertTrue(parser.isRequest());

		Request result = parser.parseRequest();
		assertEquals(request.getMID(), result.getMID());
		assertArrayEquals(request.getToken(), result.getToken());
		assertEquals(request.getOptions().asSortedList(), result.getOptions().asSortedList());
	}
	
	@Test
	public void testResponseParsing() {
		Response response = new Response(ResponseCode.CONTENT);
		response.setType(Type.NON);
		response.setMID(9);
		response.setToken(new byte[] {22, -1, 0, 78, 100, 22});
		response.getOptions().addETag(new byte[] {1, 0, 0, 0, 0, 1})
							.addLocationPath("/one/two/three/four/five/six/seven/eight/nine/ten")
							.addOption(new Option(57453, "Arbitrary".hashCode()))
							.addOption(new Option(19205, "Arbitrary1"))
							.addOption(new Option(19205, "Arbitrary2"))
							.addOption(new Option(19205, "Arbitrary3"));
		
		DataSerializer serializer = new DataSerializer();
		byte[] bytes = serializer.serializeResponse(response);
		
		DataParser parser = new DataParser(bytes);
		assertTrue(parser.isResponse());
		
		Response result = parser.parseResponse();
		assertEquals(response.getMID(), result.getMID());
		assertArrayEquals(response.getToken(), result.getToken());
		assertEquals(response.getOptions().asSortedList(), result.getOptions().asSortedList());
	}
}

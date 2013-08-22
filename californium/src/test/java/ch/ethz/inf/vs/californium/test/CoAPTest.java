package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;

/**
 * Tests that the mapping from a value to an enum is correct.
 */
public class CoAPTest {
	
	@Test
	public void testType() {
		for (Type type:Type.values()) {
			assertEquals(type, Type.valueOf(type.value));
		}
	}
	
	@Test
	public void testCode() {
		for (Code code:Code.values()) {
			assertEquals(code, Code.valueOf(code.value));
		}
	}
	
	@Test
	public void testResponseCode() {
		for (ResponseCode code:ResponseCode.values()) {
			assertEquals(code, ResponseCode.valueOf(code.value));
		}
	}
}

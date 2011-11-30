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

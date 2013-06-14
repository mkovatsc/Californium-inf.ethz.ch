package ch.inf.vs.californium.test;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import ch.inf.vs.californium.coap.Option;

/**
 * This test tests the class Option. We test that the conversion of String,
 * integer and long values to byte arrays work properly.
 */
public class OptionTest {

	@Test
	public void testSetValue() {
		Option option = new Option();

		option.setValue(new byte[4]);
		assertArrayEquals(option.getValue(), new byte[4]);
		
		option.setValue(new byte[] {69, -104, 35, 55, -104, 116, 35, -104});
		assertArrayEquals(option.getValue(), new byte[] {69, -104, 35, 55, -104, 116, 35, -104});
	}
	
	@Test
	public void testSetStringValue() {
		Option option = new Option();
		
		option.setStringValue("");
		assertArrayEquals(option.getValue(), new byte[0]);

		option.setStringValue("Californium");
		assertArrayEquals(option.getValue(), "Californium".getBytes());
	}
	
	@Test
	public void testSetIntegerValue() {
		Option option = new Option();

		option.setIntegerValue(0);
		assertArrayEquals(option.getValue(), new byte[0]);
		
		option.setIntegerValue(11);
		assertArrayEquals(option.getValue(), new byte[] {11});
		
		option.setIntegerValue(255);
		assertArrayEquals(option.getValue(), new byte[] { (byte) 255 });
		
		option.setIntegerValue(256);
		assertArrayEquals(option.getValue(), new byte[] {0, 1});
		
		option.setIntegerValue(18273);
		assertArrayEquals(option.getValue(), new byte[] {97, 71});
		
		option.setIntegerValue(1<<16);
		assertArrayEquals(option.getValue(), new byte[] {0, 0, 1});
		
		option.setIntegerValue(23984773);
		assertArrayEquals(option.getValue(), new byte[] {(byte) 133, (byte) 250, 109, 1});
		
		option.setIntegerValue(0xFFFFFFFF);
		assertArrayEquals(option.getValue(), new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
	}
	
	@Test
	public void testSetLongValue() {
		Option option = new Option();

		option.setLongValue(0);
		assertArrayEquals(option.getValue(), new byte[0]);
		
		option.setLongValue(11);
		assertArrayEquals(option.getValue(), new byte[] {11});
		
		option.setLongValue(255);
		assertArrayEquals(option.getValue(), new byte[] { (byte) 255 });
		
		option.setLongValue(256);
		assertArrayEquals(option.getValue(), new byte[] {0, 1});
		
		option.setLongValue(18273);
		assertArrayEquals(option.getValue(), new byte[] {97, 71});
		
		option.setLongValue(1<<16);
		assertArrayEquals(option.getValue(), new byte[] {0, 0, 1});
		
		option.setLongValue(23984773);
		assertArrayEquals(option.getValue(), new byte[] {(byte) 133, (byte) 250, 109, 1});

		option.setLongValue(0xFFFFFFFFL);
		assertArrayEquals(option.getValue(), new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
		
		option.setLongValue(0x9823749837239845L);
		assertArrayEquals(option.getValue(), new byte[] {69, -104, 35, 55, -104, 116, 35, -104});
		
		option.setLongValue(0xFFFFFFFFFFFFFFFFL);
		assertArrayEquals(option.getValue(), new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
	}
}

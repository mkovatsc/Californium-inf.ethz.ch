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

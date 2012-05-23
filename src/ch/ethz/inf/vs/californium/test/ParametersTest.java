package ch.ethz.inf.vs.californium.test;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Parameters;
import ch.ethz.inf.vs.californium.coap.Request;

public class ParametersTest extends TestCase {

	Request request;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		request = new GETRequest();
	}
	
	@Test
	public void testEmpty() {
		Parameters parameters = new Parameters(request);
		
		assertEquals(0, parameters.size());
		assertEquals(null, parameters.getValue(null));
		assertEquals(null, parameters.getValue(""));
		assertEquals(null, parameters.getValue("foo"));
	}
	
	public void testParameterWithoutValue1() {
		addQueryParameter("foo");
		Parameters parameters = new Parameters(request);
		
		assertEquals(1, parameters.size());
		assertEquals(null, parameters.getValue("foo"));
		
		assertEquals(1, parameters.getValues("foo").size());
		assertEquals(0, parameters.getValues("").size());
		assertEquals(0, parameters.getValues("bar").size());
	}
	
	public void testParameterWithoutValue2() {
		addQueryParameter("foo=");
		Parameters parameters = new Parameters(request);
		
		assertEquals(1, parameters.size());
		assertEquals(1, parameters.getValues("foo").size());
		assertEquals("", parameters.getValue("foo"));
		
	}
	
	public void testParameterWithValue() {
		addQueryParameter("foo=bar");
		Parameters parameters = new Parameters(request);
		
		assertEquals(1, parameters.size());
		assertEquals("bar", parameters.getValue("foo"));
		assertEquals(null, parameters.getValue("fooo"));
		assertEquals(null, parameters.getValue(null));
		
		List<String> values = parameters.getValues("foo");
		assertEquals(1, values.size());
		assertEquals("bar", values.get(0));
	}
	
	public void testParameterWithMultipleValues() {
		addQueryParameter("foo=bar");
		addQueryParameter("foo=123");
		addQueryParameter("other_stuff");
		Parameters parameters = new Parameters(request);
		
		assertEquals(3, parameters.size());
		assertEquals("bar", parameters.getValue("foo"));
		
		List<String> values = parameters.getValues("foo");
		assertEquals(2, values.size());
		assertEquals("bar", values.get(0));
		assertEquals("123", values.get(1));
	}
	
	
	private void addQueryParameter(String string){
		request.addOption(new Option(string, Parameters.OPTION_NUMBER));
	}

}

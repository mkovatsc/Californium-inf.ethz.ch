/**
 * 
 */

package ch.ethz.inf.vs.californium.test;

import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import ch.ethz.inf.vs.californium.coap.Message.messageType;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.CoapTranslator;

/**
 * @author Francesco Corazza
 * 
 */
public class CoapTranslatorTest {

	private static final int[] METHODS = { CodeRegistry.METHOD_GET, CodeRegistry.METHOD_DELETE, CodeRegistry.METHOD_POST, CodeRegistry.METHOD_PUT };

	@Test
	public void testIPv4GetRequest() {
		testMultipleRequests(null, "coap://192.168.1.1:5684/resource", null);
	}

	@Test
	public void testIPv6GetRequest() {
		testMultipleRequests(null, "coap://[2001:620:8:101f:250:c2ff:ff18:8d32]:5684/resource", null);
	}

	@Test
	public void testNoPortGetRequest() {
		testMultipleRequests(null, "coap://localhost/resource", null);
	}

	/**
	 * Test method for
	 * {@link ch.ethz.inf.vs.californium.util.CoapTranslator#getRequest(ch.ethz.inf.vs.californium.coap.Request, ch.ethz.inf.vs.californium.coap.Request)}
	 * .
	 */
	@Test
	public void testNormalGetRequest() {
		testMultipleRequests(null, "coap://localhost:5684/resource", null);
	}

	@Test
	public void testOptionGetRequest() {
		List<Option> options = new LinkedList<Option>();
		options.add(new Option("text/plain", OptionNumberRegistry.ACCEPT));
		// it should not be considered
		options.add(new Option("res", OptionNumberRegistry.URI_PATH));
		options.add(new Option("true", OptionNumberRegistry.OBSERVE));
		options.add(new Option("AERR", OptionNumberRegistry.ETAG));

		testMultipleRequests(null, "coap://localhost:5684/resource", options);
	}

	@Test
	public void testPayloadGetRequest() throws UnsupportedEncodingException {
		testMultipleRequests("AAA".getBytes("UTF-8"), "coap://localhost:5684/resource", null);
	}

	// public void testFilliUriRequest() {
	// testMultipleRequests(null, "coap://localhost:5684/resource", null);
	// }

	@Test
	public void testQueryGetRequest() {
		testMultipleRequests(null, "coap://localhost:5684/resource?a=1&b=2&c=3", null);
	}

	@Test
	public void testSubResourceGetRequest() {
		testMultipleRequests(null, "coap://localhost:5684/resource/sub1/sub2", null);
	}

	// /**
	// * Test method for
	// * {@link
	// ch.ethz.inf.vs.californium.util.CoapTranslator#fillResponse(ch.ethz.inf.vs.californium.coap.Response,
	// ch.ethz.inf.vs.californium.coap.Response)}
	// * .
	// */
	// @Test
	// public void testFillResponse() {
	// fail("Not yet implemented");
	// }

	private void testMultipleRequests(byte[] payload, String proxyUri, List<Option> options) {
		for (int method : METHODS) {
			testRequest(method, messageType.CON, payload, proxyUri, options);
			testRequest(method, messageType.NON, payload, proxyUri, options);
		}
	}

	private void testRequest(int code, messageType type, byte[] payload, String proxyUri, List<Option> options) {
		Request incomingRequest = new Request(code, type == messageType.CON);
		incomingRequest.setPayload(payload);
		incomingRequest.setOption(new Option(proxyUri, OptionNumberRegistry.PROXY_URI));
		incomingRequest.setOptions(options);

		Request testedRequest = null;
		try {
			testedRequest = CoapTranslator.getRequest(incomingRequest);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertTrue(code == testedRequest.getCode());
		assertTrue(type == testedRequest.getType());

		if (payload != null) {
			assertTrue(payload.equals(testedRequest.getPayload()));
		}

		try {
			URI incomingRequestproxyUri = incomingRequest.getProxyUri();
			URI testedRequestCompleteUri = testedRequest.getCompleteUri();
			if (incomingRequestproxyUri.getPort() == -1) {
				assertTrue(testedRequestCompleteUri.getPort() == 5683);
			} else {
				assertTrue(incomingRequestproxyUri.equals(testedRequestCompleteUri));

				String testedRequestCompleteUriString = testedRequest.getCompleteUri().toString();
				assertTrue(proxyUri.equals(testedRequestCompleteUriString));
			}

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (options != null) {
			for (Option option : options) {
				if (option.getOptionNumber() != OptionNumberRegistry.PROXY_URI && !OptionNumberRegistry.isUriOption(option.getOptionNumber())) {
					Option testedOption = testedRequest.getFirstOption(option.getOptionNumber());
					assertTrue(option.getRawValue() == testedOption.getRawValue());
				}
			}
		}
	}
}

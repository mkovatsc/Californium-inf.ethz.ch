package ch.ethz.inf.vs.californium.plugtests.tests;

import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.Utils;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_OBS_02: Handle resource observation with NON messages
 * 
 * @author Matthias Kovatsch
 */
public class CO02_05 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/obs-non";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	public CO02_05(String serverURI) {
		super(CO02_05.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.NON);
		// set Observe option
		request.setObserve();
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	@Override
	protected synchronized void executeRequest(Request request, String serverURI, String resourceUri) {

		// defensive check for slash
		if (!serverURI.endsWith("/") && !resourceUri.startsWith("/")) {
			resourceUri = "/" + resourceUri;
		}

		URI uri = null;
		try {
			uri = new URI(serverURI + resourceUri);
		} catch (URISyntaxException use) {
			throw new IllegalArgumentException("Invalid URI: "
					+ use.getMessage());
		}

		request.setURI(uri);

		// for observing
		int observeLoop = 6;
		long time = 5000;

		// print request info
		if (verbose) {
			System.out.println("Request for test " + this.testName
					+ " sent");
			Utils.prettyPrint(request);
		}

		// execute the request
		try {
			Response response = null;
			boolean success = true;

			request.send();

			System.out.println();
			System.out.println("**** TEST: " + testName + " ****");
			System.out.println("**** BEGIN CHECK ****");

			response = request.waitForResponse(time);
			if (response != null) {
				success &= checkType(Type.NON, response.getType());
				success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
				success &= hasContentType(response);
				success &= hasToken(response);
				success &= hasObserve(response);
			
				time = response.getOptions().getMaxAge() * 1000;
				System.out.println("+++++ Max-Age: "+time+" +++++");
				if (time==0) time = 5000;
	
				// receive multiple responses
				for (int l = 0; success && l < observeLoop; ++l) {
					response = request.waitForResponse(time + 1000);
	
					// checking the response
					if (response != null) {
						System.out.println("Received notification " + l);
	
						// print response info
						if (verbose) {
							System.out.println("Response received");
							System.out.println("Time elapsed (ms): "
									+ response.getRTT());
							Utils.prettyPrint(response);
						}
						
						success &= checkResponse(request, response);
					}
				}
			}
			
            System.out.println("+++++++++++++++++++++++");
            System.out.println("++++ SEE WIRESHARK ++++");
            System.out.println("++++  FOR SERVER   ++++");
            System.out.println("++++ CANCELLATION  ++++");
            System.out.println("+++++++++++++++++++++++");

			if (success) {
				System.out.println("**** TEST PASSED ****");
				addSummaryEntry(testName + ": PASSED (conditionally)");
			} else {
				System.out.println("**** TEST FAILED ****");
				addSummaryEntry(testName + ": --FAILED--");
			}

			tickOffTest();
			
		} catch (InterruptedException e) {
			System.err.println("Interupted during receive: "
					+ e.getMessage());
			System.exit(-1);
		}
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;
		
		success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
		success &= checkToken(request.getToken(), response.getToken());
		success &= hasContentType(response);
		success &= hasNonEmptyPalyoad(response);
		success &= hasObserve(response);

		return success;
	}
}
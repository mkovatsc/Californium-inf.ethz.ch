package ch.ethz.inf.vs.californium.examples.plugtest2;

import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.examples.PlugtestClient;
import ch.ethz.inf.vs.californium.examples.PlugtestClient.TestClientAbstract;

/**
 * TD_COAP_OBS_05: Server detection of deregistration (client OFF).
 * 
 * @author Matthias Kovatsch
 */
public class CO05 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/obs";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	public CO05(String serverURI) {
		super(CO05.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.CON);
		// set Observe option
		request.setObserve();
		// request.setToken(TokenManager.getInstance().acquireToken());

		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkInt(EXPECTED_RESPONSE_CODE.value,
				response.getCode().value, "code");
		// success &= checkType(Type.CON, response.getType());
		success &= hasContentType(response);
		success &= hasToken(response);

		return success;
	}

	@Override
	protected synchronized void executeRequest(Request request,
			String serverURI, String resourceUri) {
		if (serverURI == null || serverURI.isEmpty()) {
			throw new IllegalArgumentException(
					"serverURI == null || serverURI.isEmpty()");
		}

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
		int observeLoop = 2;

		// print request info
		if (verbose) {
			System.out.println("Request for test " + this.testName
					+ " sent");
			PlugtestClient.prettyPrint(request);
		}

		// execute the request
		try {
			Response response = null;
			boolean success = true;

			request.send();

			System.out.println();
			System.out.println("**** TEST: " + testName + " ****");
			System.out.println("**** BEGIN CHECK ****");

			response = request.waitForResponse(5000);

			if (response != null) {
				success &= checkInt(EXPECTED_RESPONSE_CODE.value,
						response.getCode().value, "code");
				success &= checkType(Type.ACK, response.getType());
				success &= hasContentType(response);
				success &= hasToken(response);
				success &= hasObserve(response);
			}

			// receive multiple responses
			for (int l = 0; success && l < observeLoop; ++l) {
				response = request.waitForResponse(5000);

				// checking the response
				if (response != null) {

					// print response info
					if (verbose) {
						System.out.println("Response received");
						System.out.println("Time elapsed (ms): "
								+ response.getRTT());
						PlugtestClient.prettyPrint(response);
					}

					// success &= checkResponse(response.getRequest(),
					// response);
					success &= checkResponse(request, response);

					if (!hasObserve(response)) {
						break;
					}
				}
			}

			// TODO client is switched off, server's confirmable not
			// acknowledged

			if (success) {
				System.out.println("**** TEST PASSED ****");
				addSummaryEntry(testName + ": PASSED");
			} else {
				System.out.println("**** TEST FAILED ****");
				addSummaryEntry(testName + ": FAILED");
			}

			tickOffTest();
			
		} catch (InterruptedException e) {
			System.err.println("Interupted during receive: "
					+ e.getMessage());
			System.exit(-1);
		}
	}
}
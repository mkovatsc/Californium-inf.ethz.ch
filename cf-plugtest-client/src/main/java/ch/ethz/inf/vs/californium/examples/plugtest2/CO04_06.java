package ch.ethz.inf.vs.californium.examples.plugtest2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.EmptyMessage;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.examples.PlugtestClient;
import ch.ethz.inf.vs.californium.examples.PlugtestClient.TestClientAbstract;
import ch.ethz.inf.vs.californium.network.EndpointManager;

/**
 * TD_COAP_OBS_04: Client detection of deregistration (Max-Age).
 * TD_COAP_OBS_06: Server detection of deregistration (explicit RST).
 * 
 * @author Matthias Kovatsch
 */
public class CO04_06 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/obs";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	private Timer timer = new Timer(true);

	public CO04_06(String serverURI) {
		super(CO04_06.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.CON);
		// set Observe option
		request.setObserve();
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		// success &= checkType(Type.CON, response.getType());
		success &= checkInt(EXPECTED_RESPONSE_CODE.value,
				response.getCode().value, "code");
		success &= hasContentType(response);

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
		// if (request.requiresToken()) {
		// request.setToken(TokenManager.getInstance().acquireToken());
		// }

		// enable response queue for synchronous I/O
		// if (sync) {
		// request.enableResponseQueue(true);
		// }

		// for observing
		int observeLoop = 5;

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
			boolean timedOut = false;

			MaxAgeTask timeout = null;

			request.send();

			System.out.println();
			System.out.println("**** TEST: " + testName + " ****");
			System.out.println("**** BEGIN CHECK ****");

			response = request.waitForResponse(5000);
			if (response != null) {
				success &= checkType(Type.ACK, response.getType());
				success &= checkInt(EXPECTED_RESPONSE_CODE.value,
						response.getCode().value, "code");
				success &= hasContentType(response);
				success &= hasToken(response);
				success &= hasObserve(response);
			}

			for (int l = 0; success && l < observeLoop; ++l) {

				response = request.waitForResponse(5000);

				// checking the response
				if (response != null) {

					if (l >= 2 && !timedOut) {
						System.out.println("+++++++++++++++++++++++");
						System.out.println("++++ REBOOT SERVER ++++");
						System.out.println("+++++++++++++++++++++++");
					}

					if (timeout != null) {
						timeout.cancel();
						timer.purge();
					}

					long time = response.getOptions().getMaxAge() * 1000;

					timeout = new MaxAgeTask(request);
					timer.schedule(timeout, time + 1000);

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

				} else {
					timedOut = true;
					System.out.println("PASS: Max-Age timed out");
					request.setMID(-1);
					request.send();

					++observeLoop;
				}
			}

			success &= timedOut;

			// RST to cancel
			System.out.println("+++++++ Cancelling +++++++");
			EmptyMessage rst = EmptyMessage.newRST(response);
			EndpointManager.getEndpointManager().getDefaultEndpoint().sendEmptyMessage(null, rst);

			response = request.waitForResponse(5000);

			success &= response == null;

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
package ch.ethz.inf.vs.californium.examples.plugtest2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MessageObserverAdapter;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.examples.PlugtestClient;
import ch.ethz.inf.vs.californium.examples.PlugtestClient.MaxAgeTask;
import ch.ethz.inf.vs.californium.examples.PlugtestClient.TestClientAbstract;

/**
 * TD_COAP_OBS_08: Server cleans the observers list when observed resource
 * content-format changes
 * 
 * @author Matthias Kovatsch
 */
public class CO08 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/obs";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;
	public final ResponseCode EXPECTED_RESPONSE_CODE_1 = ResponseCode.CHANGED;
	public final ResponseCode EXPECTED_RESPONSE_CODE_2 = ResponseCode.INTERNAL_SERVER_ERROR;

	private Timer timer = new Timer(true);

	public CO08(String serverURI) {
		super(CO08.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.CON);
		// request.setToken(TokenManager.getInstance().acquireToken(false));
		request.setObserve();
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);

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

		// enable response queue for synchronous I/O
		// request.enableResponseQueue(true);

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

			response = request.waitForResponse(10000);

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
				response = request.waitForResponse(10000);

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

			// Delete the /obs resource of the server (either locally or by
			// having another CoAP client perform a DELETE request)
			Request asyncRequest = new Request(Code.PUT, Type.CON);

			asyncRequest.setURI(uri);
			// if (asyncRequest.requiresToken()) {
			// asyncRequest.setToken(TokenManager.getInstance().acquireToken());
			// }

			// asyncRequest.setContentType((int)Math.random()*0xFFFF+1);
			asyncRequest.getOptions().setContentFormat(
					(int) Math.random() * 0xFFFF + 1);

			// asyncRequest.registerResponseHandler(new ResponseHandler() {
			// public void handleResponse(Response response) {
			// if (response != null) {
			// checkInt(EXPECTED_RESPONSE_CODE_1, response.getCode(),
			// "code");
			// }
			// }
			// });
			asyncRequest.addMessageObserver(new MessageObserverAdapter() {
				public void responded(Response response) {
					if (response != null) {
						checkInt(EXPECTED_RESPONSE_CODE_1.value,
								response.getCode().value, "code");
					}
				}
			});

			// enable response queue for synchronous I/O
			asyncRequest.send();

			long time = response.getOptions().getMaxAge() * 1000;

			MaxAgeTask timeout = new MaxAgeTask(request);
			timer.schedule(timeout, time + 1000);

			response = request.waitForResponse(10000);
			System.out.println("received " + response);

			if (response != null) {
				success &= checkInt(EXPECTED_RESPONSE_CODE_2.value,
						response.getCode().value, "code");
				success &= hasToken(response);
				if (hasObserve(response)) {
					System.out.println("INFO: Has observe");
				} else {
					System.out.println("INFO: No observe");
				}

				System.out.println("PASS: " + EXPECTED_RESPONSE_CODE_2
						+ " received");
			} else {
				success = false;
				System.out.println("FAIL: No " + EXPECTED_RESPONSE_CODE_2
						+ " received");
			}

			if (success) {
				System.out.println("**** TEST PASSED ****");
				addSummaryEntry(testName + ": PASSED");
			} else {
				System.out.println("**** TEST FAILED ****");
				addSummaryEntry(testName + ": FAILED");
			}

			tickOffTest();

			// } catch (IOException e) {
			// System.err.println("Failed to execute request: " +
			// e.getMessage());
			// System.exit(-1);
		} catch (InterruptedException e) {
			System.err.println("Interupted during receive: "
					+ e.getMessage());
			System.exit(-1);
		}
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkInt(EXPECTED_RESPONSE_CODE.value,
				response.getCode().value, "code");
		// success &= checkType(Type.CON, response.getType());
		success &= hasContentType(response);

		return success;
	}
}
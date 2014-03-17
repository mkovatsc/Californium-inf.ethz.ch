package ch.ethz.inf.vs.californium.plugtests.tests;

import java.net.URI;
import java.net.URISyntaxException;

import ch.ethz.inf.vs.californium.Utils;
import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_LINK_09: Arrange link descriptions hierarchically
 * 
 * @author Matthias Kovatsch
 */
public class CL09 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/.well-known/core";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;
	public static final String RESOURCE_URI_2 = "/path";
	public static final String RESOURCE_URI_3 = "/path/sub1";
	public static final String URI_QUERY = "ct=40";

	public CL09(String serverURI) {
		super(CL09.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.CON);
		request.getOptions().addURIQuery(URI_QUERY);
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
			throw new IllegalArgumentException("Invalid URI: " + use.getMessage());
		}

		request.setURI(uri);

		// print request info
		if (verbose) {
			System.out.println("Request for test " + this.testName + " sent");
			Utils.prettyPrint(request);
		}

		// execute the request
		try {
			Response response = null;
			boolean success = true;

			System.out.println();
			System.out.println("**** TEST: " + testName + " ****");
			System.out.println("**** BEGIN CHECK ****");

			request.send();
			response = request.waitForResponse(6000);

			// checking the response
			if (response != null) {

				// print response info
				if (verbose) {
					System.out.println("Response received");
					System.out.println("Time elapsed (ms): "
							+ response.getRTT());
					Utils.prettyPrint(response);
				}

				success &= checkType(Type.ACK, response.getType());
				success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
				success &= checkOption(MediaTypeRegistry.APPLICATION_LINK_FORMAT, response.getOptions().getContentFormat(), "Content-Format");
				success &= checkDiscovery(RESOURCE_URI_2, response.getPayloadString());
				
				// Client sends a GET request for /path to Server
				request = new Request(Code.GET, Type.CON);
				try {
					uri = new URI(serverURI + RESOURCE_URI_2);
				} catch (URISyntaxException use) {
					throw new IllegalArgumentException("Invalid URI: "
							+ use.getMessage());
				}

				request.setURI(uri);

				request.send();
				response = request.waitForResponse(6000);

				// checking the response
				if (response != null) {

					// print response info
					if (verbose) {
						System.out.println("Response received");
						System.out.println("Time elapsed (ms): "
								+ response.getRTT());
						Utils.prettyPrint(response);
					}

					success &= checkType(Type.ACK, response.getType());
					success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
					success &= checkOption(MediaTypeRegistry.APPLICATION_LINK_FORMAT, response.getOptions().getContentFormat(), "Content-Format");
					success &= checkDiscovery(RESOURCE_URI_3, response.getPayloadString());
					
					// Client sends a GET request for /path/sub1
					request = new Request(Code.GET, Type.CON);
					try {
						uri = new URI(serverURI + RESOURCE_URI_3);
					} catch (URISyntaxException use) {
						throw new IllegalArgumentException("Invalid URI: "
								+ use.getMessage());
					}

					request.setURI(uri);
					
					request.send();
					response = request.waitForResponse(6000);

					// checking the response
					if (response != null) {

						// print response info
						if (verbose) {
							System.out.println("Response received");
							System.out.println("Time elapsed (ms): "
									+ response.getRTT());
							Utils.prettyPrint(response);
						}

						success &= checkType(Type.ACK, response.getType());
						success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
					}
				}
			}

			if (success) {
				System.out.println("**** TEST PASSED ****");
				addSummaryEntry(testName + ": PASSED");
			} else {
				System.out.println("**** TEST FAILED ****");
				addSummaryEntry(testName + ": --FAILED--");
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
		return false;
	}
}
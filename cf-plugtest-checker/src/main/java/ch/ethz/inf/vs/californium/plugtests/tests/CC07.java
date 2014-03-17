package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_CORE_07: Perform PUT transaction (NON mode).
 * 
 * @author Francesco Corazza and Matthias Kovatsch
 */
public class CC07 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/test";
	private final int[] expectedResponseCodes = new int[] {
			ResponseCode.CREATED.value, ResponseCode.CHANGED.value };

	public CC07(String serverURI) {
		super(CC07.class.getSimpleName());

		// create the request
		Request request = new Request(Code.PUT);
		request.setConfirmable(false);
		// add payload
		request.setPayload("TD_COAP_CORE_07", MediaTypeRegistry.TEXT_PLAIN);
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkType(Type.NON, response.getType());
		// Code = 68 (2.04 Changed) or 65 (2.01 Created)
		success &= checkInts(expectedResponseCodes,
				response.getCode().value, "code");

		return success;
	}
}
package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_CORE_04: Perform POST transaction (CON mode).
 * 
 * @author Francesco Corazza and Matthias Kovatsch
 */
public class CC04 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/test";
	private final int[] expectedResponseCodes = new int[] {
			ResponseCode.CREATED.value, ResponseCode.CHANGED.value };

	public CC04(String serverURI) {
		super(CC04.class.getSimpleName());

		// create the request
		Request request = Request.newPost();
		// add payload
		request.setPayload("TD_COAP_CORE_04", MediaTypeRegistry.TEXT_PLAIN);
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkType(Type.ACK, response.getType());
		// Code = 65(2.01 Created) or 68 (2.04 changed)
		success &= checkInts(expectedResponseCodes,
				response.getCode().value, "code");
		success &= checkInt(request.getMID(), response.getMID(), "MID");

		return success;
	}
}
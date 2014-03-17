package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_CORE_01: Perform GET transaction (CON mode).
 * 
 * @author Francesco Corazza and Matthias Kovatsch
 */
public class CC01 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/test";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	public CC01(String serverURI) {
		super(CC01.class.getSimpleName());

		// create the request
		Request request = Request.newGet();

		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkType(Type.ACK, response.getType());
		success &= checkInt(EXPECTED_RESPONSE_CODE.value,
				response.getCode().value, "code");
		success &= checkInt(request.getMID(), response.getMID(), "MID");
		success &= hasContentType(response);
		success &= hasNonEmptyPalyoad(response);

		return success;
	}
}
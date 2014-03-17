package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_CORE_13 Handle request containing several Uri-Path options.
 * 
 * @author Matthias Kovatsch
 */
public class CC13 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/seg1/seg2/seg3";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	public CC13(String serverURI) {
		super(CC13.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.CON);
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkType(Type.ACK, response.getType());
		success &= checkInt(EXPECTED_RESPONSE_CODE.value,
				response.getCode().value, "code");
		success &= hasContentType(response);
		success &= hasNonEmptyPalyoad(response);

		return success;
	}
}
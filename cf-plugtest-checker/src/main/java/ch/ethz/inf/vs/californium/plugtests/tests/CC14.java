package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_CORE_14: Handle request containing several Uri-Query options.
 * 
 * @author Matthias Kovatsch
 */
public class CC14 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/query";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	public CC14(String serverURI) {
		super(CC14.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.CON);
		// add query
		request.getOptions().addURIQuery("first=1");
		request.getOptions().addURIQuery("second=2");
		request.getOptions().addURIQuery("third=3");

		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkTypes(new Type[] { Type.ACK, Type.CON },
				response.getType());
		success &= checkInt(EXPECTED_RESPONSE_CODE.value,
				response.getCode().value, "code");
		success &= hasContentType(response);
		success &= hasNonEmptyPalyoad(response);

		return success;
	}
}
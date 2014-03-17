package ch.ethz.inf.vs.californium.plugtests.tests;

import java.util.Arrays;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_CORE_18: Perform POST transaction with responses containing
 * several Location-Path options (CON mode)
 * 
 * @author Matthias Kovatsch
 */
public class CC18 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/test";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CREATED;

	public CC18(String serverURI) {
		super(CC18.class.getSimpleName());

		// create the request
		Request request = new Request(Code.POST, Type.CON);
		// add payload
		request.setPayload("TD_COAP_CORE_18", MediaTypeRegistry.TEXT_PLAIN);
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkType(Type.ACK, response.getType());
		success &= checkInt(EXPECTED_RESPONSE_CODE.value,
				response.getCode().value, "code");
		success &= hasLocation(response);

		if (success) {

			List<String> path = response.getOptions().getLocationPaths();
			List<String> expc = Arrays.asList("location1", "location2",
					"location3");
			success &= checkOption(expc, path, "Location path");
		}
		return success;
	}
}
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
 * several Location-Query options (CON mode)
 * 
 * @author Matthias Kovatsch
 */
public class CC19 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/location-query";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CREATED;

	public CC19(String serverURI) {
		super(CC19.class.getSimpleName());

		// create the request
		Request request = new Request(Code.POST, Type.CON);
		// add payload
		request.setPayload("TD_COAP_CORE_19", MediaTypeRegistry.TEXT_PLAIN);
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkType(Type.ACK, response.getType());
		success &= checkInt(EXPECTED_RESPONSE_CODE.value,
				response.getCode().value, "code");
		success &= hasLocationQuery(response);

		// List<Option> options =
		// response.getOptions(OptionNumberRegistry.LOCATION_QUERY);
		// success &= checkOption(new Option("first=1",
		// OptionNumberRegistry.LOCATION_QUERY), options.get(0));
		// success &= checkOption(new Option("second=2",
		// OptionNumberRegistry.LOCATION_QUERY), options.get(1));

		List<String> query = response.getOptions().getLocationQueries();
		List<String> expec = Arrays.asList("first=1", "second=2");
		success &= checkOption(expec, query, "Location Query");

		return success;
	}
}
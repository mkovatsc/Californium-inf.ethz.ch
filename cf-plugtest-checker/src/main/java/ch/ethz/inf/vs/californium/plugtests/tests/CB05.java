package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

public class CB05 extends TestClientAbstract {

	// Handle POST with two-way blockwise transfer
	String data = PlugtestChecker.getLargeRequestPayload();
	private ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CHANGED;

	public CB05(String serverURI) {
		super(CB05.class.getSimpleName());

		Request request = Request.newPost();
		request.setPayload(data);
		request.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);

		// set the parameters and execute the request
		executeRequest(request, serverURI, "/large-post");
	}

	@Override
	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;
		
		success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
		success &= hasContentType(response);
		success &= hasNonEmptyPalyoad(response);
		
		return success;
	}
}
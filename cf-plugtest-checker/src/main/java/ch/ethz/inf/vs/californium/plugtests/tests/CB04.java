package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

public class CB04 extends TestClientAbstract {

	// Handle POST blockwise transfer for creating large resource
	String data = PlugtestChecker.getLargeRequestPayload();
	private ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CREATED;

	public CB04(String serverURI) {
		super(CB04.class.getSimpleName());

		Request request = Request.newPost();
		request.setPayload(data);
		request.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);

		// set the parameters and execute the request
		executeRequest(request, serverURI, "/large-create");

		// TODO: verify resource creation (optional): send GET request to
		// location path of response
	}

	@Override
	protected boolean checkResponse(Request request, Response response) {
		boolean success = response.getOptions().hasBlock1();

		if (!success) {
			System.out.println("FAIL: no Block1 option");
		} else {
			int maxNUM = response.getOptions().getBlock1().getNum();
			success &= checkInt(EXPECTED_RESPONSE_CODE.value,
					response.getCode().value, "code");
			success &= checkOption(new BlockOption(PlugtestChecker.PLUGTEST_BLOCK_SZX,
					false, maxNUM), response.getOptions().getBlock1(),
					"Block1");
			success &= hasLocation(response);
		}

		return success;
	}
}
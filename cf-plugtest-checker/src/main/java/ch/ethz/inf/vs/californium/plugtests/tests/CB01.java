package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

public class CB01 extends TestClientAbstract {

	// Handle GET blockwise transfer for large resource (early negotiation)
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	public CB01(String serverURI) {
		super(CB01.class.getSimpleName());

		Request request = Request.newGet();
		request.getOptions().setBlock2(BlockOption.size2Szx(64), false, 0);

		// set the parameters and execute the request
		executeRequest(request, serverURI, "/large");
	}

	@Override
	protected boolean checkResponse(Request request, Response response) {
		boolean success = response.getOptions().hasBlock2();

		if (!success) {
			System.out.println("FAIL: no Block2 option");
		} else {
			int maxNUM = response.getOptions().getBlock2().getNum();
			success &= checkType(Type.ACK, response.getType());
			success &= checkInt(EXPECTED_RESPONSE_CODE.value,
					response.getCode().value, "code");
			success &= checkOption(new BlockOption(PlugtestChecker.PLUGTEST_BLOCK_SZX,
					false, maxNUM), response.getOptions().getBlock2(),
					"Block2");
			success &= hasNonEmptyPalyoad(response);
			success &= hasContentType(response);
		}
		return success;
	}
}
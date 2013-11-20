package ch.ethz.inf.vs.californium.examples.plugtest2;

import ch.ethz.inf.vs.californium.coap.BlockOption;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.examples.PlugtestClient;
import ch.ethz.inf.vs.californium.examples.PlugtestClient.TestClientAbstract;

public class CB03 extends TestClientAbstract {

	// Handle PUT blockwise transfer for large resource
	String data = PlugtestClient.getLargeRequestPayload();
	private ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CHANGED;

	public CB03(String serverURI) {
		super(CB03.class.getSimpleName());

		Request request = Request.newPut();
		request.setPayload(data);
		request.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);

		// set the parameters and execute the request
		executeRequest(request, serverURI, "/large-update");

	}

	@Override
	protected boolean checkResponse(Request request, Response response) {
		boolean success = response.getOptions().hasBlock1();

		if (!success) {
			System.out.println("FAIL: no Block1 option");
		} else {
			int maxNUM = response.getOptions().getBlock1().getNum();
			success &= checkType(Type.ACK, response.getType());
			success &= checkInt(EXPECTED_RESPONSE_CODE.value,
					response.getCode().value, "code");
			success &= checkOption(new BlockOption(PlugtestClient.PLUGTEST_BLOCK_SZX,
					false, maxNUM), response.getOptions().getBlock1(),
					"Block1");
		}

		return success;
	}
}
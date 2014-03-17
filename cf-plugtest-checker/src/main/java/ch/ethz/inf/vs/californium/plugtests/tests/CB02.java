package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

public class CB02 extends TestClientAbstract {

	// Handle GET blockwise transfer for large resource (late negotiation)
	
    public static final String RESOURCE_URI = "/large";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;

	public CB02(String serverURI) {
		super(CB02.class.getSimpleName());

        // create the request
		Request request = new Request(Code.GET, Type.CON);
        // set the parameters and execute the request
        executeRequest(request, serverURI, RESOURCE_URI);
	}

	@Override
	protected boolean checkResponse(Request request, Response response) {
		boolean success = response.getOptions().hasBlock2();
        
        if (!success) {
            System.out.println("FAIL: no Block2 option");
        } else {
            success &= hasNonEmptyPalyoad(response);
            success &= hasContentType(response);
        }
        return success;
	}
}
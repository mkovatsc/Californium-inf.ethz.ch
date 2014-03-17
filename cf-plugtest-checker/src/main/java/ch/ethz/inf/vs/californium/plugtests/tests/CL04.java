package ch.ethz.inf.vs.californium.plugtests.tests;

import ch.ethz.inf.vs.californium.coap.CoAP.Code;
import ch.ethz.inf.vs.californium.coap.CoAP.ResponseCode;
import ch.ethz.inf.vs.californium.coap.CoAP.Type;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.plugtests.PlugtestChecker.TestClientAbstract;

/**
 * TD_COAP_LINK_04: Handle empty prefix value strings
 * 
 * @author Matthias Kovatsch
 */
public class CL04 extends TestClientAbstract {

	public static final String RESOURCE_URI = "/.well-known/core";
	public final ResponseCode EXPECTED_RESPONSE_CODE = ResponseCode.CONTENT;
	public static final String EXPECTED_RT = "rt=Type2";

	public CL04(String serverURI) {
		super(CL04.class.getSimpleName());

		// create the request
		Request request = new Request(Code.GET, Type.CON);
		// set query
		request.getOptions().addURIQuery(EXPECTED_RT);
		// set the parameters and execute the request
		executeRequest(request, serverURI, RESOURCE_URI);
	}

	protected boolean checkResponse(Request request, Response response) {
		boolean success = true;

		success &= checkType(Type.ACK, response.getType());
		success &= checkInt(EXPECTED_RESPONSE_CODE.value, response.getCode().value, "code");
		success &= checkOption(MediaTypeRegistry.APPLICATION_LINK_FORMAT, response.getOptions().getContentFormat(), "Content-Format");
		success &= checkDiscoveryAttributes(EXPECTED_RT, response.getPayloadString());
		
		return success;
	}
}